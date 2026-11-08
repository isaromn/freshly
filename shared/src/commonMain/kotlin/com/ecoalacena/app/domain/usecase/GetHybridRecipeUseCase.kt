package com.ecoalacena.app.domain.usecase

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.currentEpochMillis
import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.core.i18n.currentLanguage
import com.ecoalacena.app.data.remote.ai.GeminiException
import com.ecoalacena.app.data.remote.ai.GeminiRecipeResult
import com.ecoalacena.app.data.remote.ai.GeminiRecipeService
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.model.Recipe
import com.ecoalacena.app.domain.model.RecipeCategory
import com.ecoalacena.app.domain.model.RecipeDifficulty
import com.ecoalacena.app.domain.model.RecipeMode
import com.ecoalacena.app.domain.model.RecipeSource
import com.ecoalacena.app.domain.recipe.LocalRecipeEngine
import com.ecoalacena.app.domain.recipe.RecipePhotoFinder
import com.ecoalacena.app.domain.repository.PantryRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Thrown when there is nothing usable in the pantry to cook with. */
class EmptyPantryException : Exception("There are no usable ingredients in the pantry.")

data class HybridRecipeResult(
    /** The AI recipe first (when available), followed by local recipes. */
    val recipes: List<Recipe>,
    /** The pantry items the recipes were built around, most urgent first. */
    val ingredients: List<PantryItem>,
    /** Why there is no AI recipe, or `null` when Gemini answered. */
    val aiFailure: Throwable?,
)

/**
 * Hybrid recipe engine. It picks ingredients from the pantry according to the [RecipeMode] (the few
 * items closest to expiring, or everything that is still good), asks Gemini for a recipe, and always
 * adds recipes from the offline [LocalRecipeEngine]. If Gemini is unavailable (no network, no key,
 * quota) the user still gets local suggestions.
 *
 * Every recipe, from the AI or from the offline templates, is written in the language of the device
 * (`es` or `en`). The language is read once per request and passed down to [GeminiRecipeService] and
 * [LocalRecipeEngine], so all of them use the same one.
 */
class GetHybridRecipeUseCase(
    private val repository: PantryRepository,
    private val recipeService: GeminiRecipeService,
    private val localEngine: LocalRecipeEngine = LocalRecipeEngine(),
    /** Looks for a photo of every recipe; without one (no key, no internet) the illustration stays. */
    private val photoFinder: RecipePhotoFinder = RecipePhotoFinder.None,
    private val nowMillis: () -> Long = ::currentEpochMillis,
    /** Language of the device, read on every request. */
    private val language: () -> AppLanguage = ::currentLanguage,
) {

    /**
     * Picks the ingredients to cook with, most urgent first, never expired ones:
     * - [RecipeMode.EXPIRING_SOON]: up to 5 items that expire within 3 days (or, if none does, the
     *   closest to expiring);
     * - [RecipeMode.AVAILABLE_PANTRY]: every item still good, up to 20.
     */
    suspend fun selectIngredients(mode: RecipeMode = RecipeMode.EXPIRING_SOON): List<PantryItem> {
        val now = nowMillis()
        val available = repository.observeAll().first()
            .filter { it.isNotExpired(now) }
            .sortedWith(compareBy<PantryItem, Long?>(nullsLast<Long>()) { it.expirationDate })

        return when (mode) {
            // The most urgent few (red or yellow in the pantry). With nothing about to expire, the
            // closest dates are used, so there is always something to cook.
            RecipeMode.EXPIRING_SOON -> {
                val threshold = now + EXPIRING_WINDOW_DAYS * MILLIS_PER_DAY
                available
                    .filter { item -> item.expirationDate != null && item.expirationDate <= threshold }
                    .ifEmpty { available }
                    .take(MAX_EXPIRING_INGREDIENTS)
            }

            // Everything that is still good, most urgent first, capped to keep the prompt short.
            RecipeMode.AVAILABLE_PANTRY -> available.take(MAX_PANTRY_INGREDIENTS)
        }
    }

    /**
     * @param outputLanguage language of the AI recipe; the device language by default.
     */
    suspend operator fun invoke(
        mode: RecipeMode = RecipeMode.EXPIRING_SOON,
        outputLanguage: AppLanguage = language(),
    ): Result<HybridRecipeResult> =
        try {
            Result.success(buildSuggestions(mode, outputLanguage))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(error)
        }

    private suspend fun buildSuggestions(mode: RecipeMode, outputLanguage: AppLanguage): HybridRecipeResult {
        val ingredients = selectIngredients(mode)
        if (ingredients.isEmpty()) throw EmptyPantryException()

        val now = nowMillis()
        val aiResult = recipeService.generateRecipe(
            ingredientLines = ingredients.map { it.toPromptLine(now) },
            mode = mode,
            outputLanguage = outputLanguage,
        )
        // The AI recipe is built from every selected ingredient.
        val aiRecipe = aiResult.getOrNull()?.toRecipeOrNull()?.copy(pantryItemIds = ingredients.map { it.id })

        val aiFailure = when {
            aiRecipe != null -> null
            else -> aiResult.exceptionOrNull() ?: GeminiException.InvalidResponse("Gemini returned an incomplete recipe.")
        }

        val localLimit = if (aiRecipe != null) LOCAL_RECIPES_WITH_AI else LOCAL_RECIPES_WITHOUT_AI
        val localRecipes = localEngine.suggest(ingredients, localLimit, outputLanguage)

        return HybridRecipeResult(
            recipes = withPhotos(listOfNotNull(aiRecipe) + localRecipes),
            ingredients = ingredients,
            aiFailure = aiFailure,
        )
    }

    private fun PantryItem.isNotExpired(now: Long): Boolean =
        expirationDate == null || expirationDate >= now

    private fun PantryItem.toPromptLine(now: Long): String {
        // Names come from the AI or the user: keep them on one line and short.
        val cleanName = name.replace(Regex("\\s+"), " ").trim().take(MAX_NAME_LENGTH)
        val expiry = expirationDate
            ?.let { expiration ->
                val days = ((expiration - now) / MILLIS_PER_DAY).toInt()
                if (days <= 0) "expires today" else "expires in $days days"
            }
            ?: "no expiration date"

        return "- $cleanName: ${promptAmount()}, $expiry"
    }

    /** Amount with the raw unit code ('2 kg', '6 unit'), so the prompt does not lean towards one language. */
    private fun PantryItem.promptAmount(): String {
        val amount = if (quantity % 1.0 == 0.0) quantity.toLong().toString() else quantity.toString()
        return "$amount $unit"
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun GeminiRecipeResult.toRecipeOrNull(): Recipe? {
        val cleanTitle = title.trim()
        val cleanSteps = steps.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanTitle.isEmpty() || cleanSteps.isEmpty()) return null

        return Recipe(
            id = "ai-${Uuid.random()}",
            title = cleanTitle,
            ingredients = ingredients.map { it.trim() }.filter { it.isNotEmpty() },
            steps = cleanSteps,
            prepTimeMinutes = prepTimeMinutes.coerceIn(1, MAX_PREP_MINUTES),
            difficulty = RecipeDifficulty.fromRaw(difficulty),
            source = RecipeSource.IS_AI,
            category = RecipeCategory.fromRaw(category),
            imageQuery = imageQuery.trim(),
        ).withNutrition(this)
    }

    /** Adds the AI's nutrition estimate when it is believable; otherwise the recipe shows none. */
    private fun Recipe.withNutrition(result: GeminiRecipeResult): Recipe {
        val portions = result.servings?.coerceIn(1, MAX_SERVINGS) ?: 1
        val calories = result.caloriesPerServing?.takeIf { it in 1..MAX_CALORIES_PER_SERVING } ?: return this
        fun Double?.grams(): Float? = this?.takeIf { it.isFinite() && it in 0.0..MAX_GRAMS_PER_SERVING }?.toFloat()

        return copy(
            servings = portions,
            caloriesPerServing = calories,
            totalCalories = calories * portions,
            proteinGrams = result.proteinGrams.grams(),
            carbsGrams = result.carbsGrams.grams(),
            fatGrams = result.fatGrams.grams(),
        )
    }

    /**
     * Searches the photos of all [recipes] at the same time. A search that fails or takes too long
     * (for example with no internet) leaves that recipe with its local illustration.
     */
    private suspend fun withPhotos(recipes: List<Recipe>): List<Recipe> =
        coroutineScope {
            recipes
                .map { recipe ->
                    async {
                        val photo = withTimeoutOrNull(PHOTO_TIMEOUT_MILLIS) {
                            photoFinder.findPhoto(recipe.imageQuery)
                        }
                        if (photo == null) recipe else recipe.copy(
                            imageUrl = photo.url,
                            imageCredit = photo.credit,
                            imageCreditUrl = photo.creditUrl,
                        )
                    }
                }
                .awaitAll()
        }

    private companion object {
        const val EXPIRING_WINDOW_DAYS = 3L
        const val MAX_EXPIRING_INGREDIENTS = 5
        const val MAX_PANTRY_INGREDIENTS = 20
        const val MAX_NAME_LENGTH = 60
        const val MAX_PREP_MINUTES = 600
        const val LOCAL_RECIPES_WITH_AI = 4
        const val LOCAL_RECIPES_WITHOUT_AI = 5
        const val PHOTO_TIMEOUT_MILLIS = 5_000L
        const val MAX_SERVINGS = 20
        const val MAX_CALORIES_PER_SERVING = 5_000
        const val MAX_GRAMS_PER_SERVING = 500.0
    }
}
