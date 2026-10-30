package com.ecoalacena.app.presentation.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.core.i18n.currentLanguage
import com.ecoalacena.app.core.i18n.currentStrings
import com.ecoalacena.app.domain.model.localizedFoodName
import com.ecoalacena.app.domain.usecase.EmptyPantryException
import com.ecoalacena.app.domain.usecase.GetHybridRecipeUseCase
import com.ecoalacena.app.presentation.util.toGeminiUserMessage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipeViewModel(
    private val getRecipes: GetHybridRecipeUseCase,
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
            is RecipeIntent.ToggleRecipe -> toggleRecipe(intent.recipeId)
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
                        expandedRecipeId = result.recipes.firstOrNull()?.id,
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

    private fun toggleRecipe(recipeId: String) {
        _state.update { current ->
            current.copy(
                expandedRecipeId = if (current.expandedRecipeId == recipeId) null else recipeId,
            )
        }
    }
}
