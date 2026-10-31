package com.ecoalacena.app.presentation.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.core.i18n.currentLanguage
import com.ecoalacena.app.core.i18n.currentStrings
import com.ecoalacena.app.domain.model.localizedFoodName
import com.ecoalacena.app.domain.repository.PantryRepository
import com.ecoalacena.app.domain.usecase.ConsumeRecipeIngredientsUseCase
import com.ecoalacena.app.domain.usecase.EmptyPantryException
import com.ecoalacena.app.domain.usecase.GetHybridRecipeUseCase
import com.ecoalacena.app.domain.usecase.IngredientUse
import com.ecoalacena.app.presentation.util.toGeminiUserMessage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipeViewModel(
    private val getRecipes: GetHybridRecipeUseCase,
    private val repository: PantryRepository,
    private val consumeIngredients: ConsumeRecipeIngredientsUseCase,
    /** Texts for the user's language; read each time a message is built. */
    private val strings: () -> AppStrings = ::currentStrings,
    /** Language of the device; the AI recipe is requested in it. */
    private val language: () -> AppLanguage = ::currentLanguage,
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeState())
    val state: StateFlow<RecipeState> = _state.asStateFlow()

    private var loadJob: Job? = null

    /** Ids of the pantry items used for the current recipes; `null` if nothing loaded successfully. */
    private var loadedIngredientIds: List<String>? = null

    /** Language the current recipes were requested in, to reload them if the device language changes. */
    private var loadedLanguage: AppLanguage? = null

    fun onIntent(intent: RecipeIntent) {
        when (intent) {
            RecipeIntent.ScreenShown -> reloadIfPantryChanged()
            RecipeIntent.Refresh -> load()
            is RecipeIntent.OpenRecipe -> openRecipe(intent.recipeId)
            RecipeIntent.CloseRecipe -> _state.update { it.copy(selectedRecipeId = null) }
            is RecipeIntent.ToggleStep -> toggleStep(intent.recipeId, intent.stepIndex)
            is RecipeIntent.FinishRecipe -> finishRecipe(intent.recipeId)
            is RecipeIntent.ToggleCompletionIngredient -> updateCompletionIngredient(intent.itemId) {
                it.copy(selected = !it.selected)
            }
            is RecipeIntent.SetCompletionQuantity -> updateCompletionIngredient(intent.itemId) {
                it.copy(quantity = intent.quantity.coerceIn(0.0, it.item.quantity))
            }
            RecipeIntent.ConfirmCompletion -> confirmCompletion()
            RecipeIntent.DismissCompletion -> dismissCompletion()
        }
    }

    private fun reloadIfPantryChanged() {
        viewModelScope.launch {
            val currentIds = try {
                getRecipes.selectIngredients().map { it.id }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                null
            }

            val isLoading = _state.value.status == RecipeStatus.LOADING && loadJob?.isActive == true
            val pantryChanged = currentIds != loadedIngredientIds
            val languageChanged = language() != loadedLanguage
            if ((pantryChanged || languageChanged) && !isLoading) {
                load()
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        _state.update { it.copy(status = RecipeStatus.LOADING, errorMessage = null) }

        val outputLanguage = language()
        loadedLanguage = outputLanguage

        loadJob = viewModelScope.launch {
            getRecipes(outputLanguage).fold(
                onSuccess = { result ->
                    val texts = strings()
                    loadedIngredientIds = result.ingredients.map { it.id }
                    _state.value = RecipeState(
                        status = RecipeStatus.CONTENT,
                        recipes = result.recipes,
                        usedIngredients = result.ingredients.map { it.name.localizedFoodName(outputLanguage) },
                        aiNotice = result.aiFailure?.let { failure ->
                            val reason = failure.toGeminiUserMessage(
                                strings = texts,
                                fallback = texts.recipeAiFailed,
                            )
                            "$reason ${texts.recipeLocalSuffix}"
                        },
                    )
                },
                onFailure = { error ->
                    if (error is EmptyPantryException) {
                        loadedIngredientIds = emptyList()
                        _state.value = RecipeState(status = RecipeStatus.EMPTY_PANTRY)
                    } else {
                        loadedIngredientIds = null
                        _state.value = RecipeState(
                            status = RecipeStatus.ERROR,
                            errorMessage = strings().recipesLoadError,
                        )
                    }
                },
            )
        }
    }

    private fun openRecipe(recipeId: String) {
        _state.update { current ->
            if (current.recipes.none { it.id == recipeId }) current else current.copy(selectedRecipeId = recipeId)
        }
    }

    private fun toggleStep(recipeId: String, stepIndex: Int) {
        val recipe = _state.value.recipes.firstOrNull { it.id == recipeId } ?: return
        if (stepIndex !in recipe.steps.indices) return

        val done = _state.value.completedStepsOf(recipeId)
        val updated = if (stepIndex in done) done - stepIndex else done + stepIndex
        _state.update { it.copy(completedSteps = it.completedSteps + (recipeId to updated)) }

        // Ticking off the last step means the meal is ready.
        if (updated.size == recipe.steps.size && stepIndex in updated) finishRecipe(recipeId)
    }

    /** Opens the sheet with the recipe's ingredients that are still in the pantry. */
    private fun finishRecipe(recipeId: String) {
        val recipe = _state.value.recipes.firstOrNull { it.id == recipeId } ?: return

        viewModelScope.launch {
            val pantry = try {
                repository.observeAll().first()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                emptyList()
            }
            val used = recipe.pantryItemIds.mapNotNull { id -> pantry.firstOrNull { it.id == id } }
            _state.update {
                it.copy(completion = RecipeCompletion(recipeId, used.map { item -> CompletionIngredient(item) }))
            }
        }
    }

    private fun updateCompletionIngredient(itemId: String, change: (CompletionIngredient) -> CompletionIngredient) {
        _state.update { current ->
            val completion = current.completion?.takeIf { it.phase == CompletionPhase.SELECTING }
                ?: return@update current
            current.copy(
                completion = completion.copy(
                    ingredients = completion.ingredients.map { if (it.item.id == itemId) change(it) else it },
                ),
            )
        }
    }

    private fun confirmCompletion() {
        val completion = _state.value.completion?.takeIf { it.phase == CompletionPhase.SELECTING } ?: return
        val uses = completion.ingredients
            .filter { it.selected && it.quantity > 0.0 }
            .map { IngredientUse(it.item, it.quantity) }
        if (uses.isEmpty()) return

        _state.update { it.copy(completion = completion.copy(phase = CompletionPhase.SAVING, errorMessage = null)) }

        viewModelScope.launch {
            val next = try {
                completion.copy(phase = CompletionPhase.CELEBRATING, impact = consumeIngredients(uses))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                completion.copy(phase = CompletionPhase.SELECTING, errorMessage = strings().consumeError)
            }
            _state.update { it.copy(completion = next) }
        }
    }

    /**
     * Closes the sheet. After a celebration the pantry changed, so the recipe is closed and the
     * suggestions are rebuilt with what is left.
     */
    private fun dismissCompletion() {
        val celebrated = _state.value.completion?.phase == CompletionPhase.CELEBRATING
        if (_state.value.completion?.phase == CompletionPhase.SAVING) return

        _state.update { it.copy(completion = null, selectedRecipeId = if (celebrated) null else it.selectedRecipeId) }
        if (celebrated) reloadIfPantryChanged()
    }
}
