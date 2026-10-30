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
import com.ecoalacena.app.domain.model.RecipeDifficulty
import com.ecoalacena.app.domain.model.RecipeSource
import com.ecoalacena.app.domain.recipe.LocalRecipeEngine
import com.ecoalacena.app.domain.repository.PantryRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first

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
 * Hybrid recipe engine. It takes the items closest to expiring from the pantry, asks Gemini
 * for a recipe, and always adds recipes from the offline [LocalRecipeEngine]. If Gemini
 * is unavailable (no network, no key, quota) the user still gets local suggestions.
 *
 * Every recipe, from the AI or from the offline templates, is written in the language of the device
 * (`es` or `en`). The language is read once per request and passed down to [GeminiRecipeService] and
 * [LocalRecipeEngine], so all of them use the same one.
 */
class GetHybridRecipeUseCase(
    private val repository: PantryRepository,
    private val recipeService: GeminiRecipeService,
    private val localEngine: LocalRecipeEngine = LocalRecipeEngine(),
    private val nowMillis: () -> Long = ::currentEpochMillis,
    /** Language of the device, read on every request. */
    private val language: () -> AppLanguage = ::currentLanguage,
) {

    /**
     * Picks the ingredients to cook with: items that expire within a few days, or, if there
     * are none, any item that has not expired yet. Most urgent first.
     */
    suspend fun selectIngredients(): List<PantryItem> {
        val now = nowMillis()

        val expiringSoon = repository
            .observeExpiringSoon(now + EXPIRING_WINDOW_DAYS * MILLIS_PER_DAY)
            .first()
            .filter { it.isNotExpired(now) }

        val pool = expiringSoon.ifEmpty {
            repository.observeAll().first().filter { it.isNotExpired(now) }
        }

        return pool
            .sortedWith(compareBy<PantryItem, Long?>(nullsLast<Long>()) { it.expirationDate })
            .take(MAX_INGREDIENTS)
    }

    /**
     * @param outputLanguage language of the AI recipe; the device language by default.
     */
    suspend operator fun invoke(
        outputLanguage: AppLanguage = language(),
    ): Result<HybridRecipeResult> =
        try {
            Result.success(buildSuggestions(outputLanguage))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(error)
        }

    private suspend fun buildSuggestions(outputLanguage: AppLanguage): HybridRecipeResult {
        val ingredients = selectIngredients()
        if (ingredients.isEmpty()) throw EmptyPantryException()

        val now = nowMillis()
        val aiResult = recipeService.generateRecipe(
            ingredientLines = ingredients.map { it.toPromptLine(now) },
            outputLanguage = outputLanguage,
        )
        // The AI recipe is built around the most urgent item, so it takes that item's category.
        val aiRecipe = aiResult.getOrNull()?.toRecipeOrNull()?.copy(category = ingredients.first().category)

        val aiFailure = when {
            aiRecipe != null -> null
            else -> aiResult.exceptionOrNull() ?: GeminiException("Gemini returned an incomplete recipe.")
        }

        val localLimit = if (aiRecipe != null) LOCAL_RECIPES_WITH_AI else LOCAL_RECIPES_WITHOUT_AI
        val localRecipes = localEngine.suggest(ingredients, localLimit, outputLanguage)

        return HybridRecipeResult(
            recipes = listOfNotNull(aiRecipe) + localRecipes,
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
        )
    }

    private companion object {
        const val EXPIRING_WINDOW_DAYS = 3L
        const val MAX_INGREDIENTS = 8
        const val MAX_NAME_LENGTH = 60
        const val MAX_PREP_MINUTES = 600
        const val LOCAL_RECIPES_WITH_AI = 2
        const val LOCAL_RECIPES_WITHOUT_AI = 3
    }
}
