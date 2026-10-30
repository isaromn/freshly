package com.ecoalacena.app.presentation.recipes

import com.ecoalacena.app.domain.model.Recipe

enum class RecipeStatus {
    LOADING,
    CONTENT,

    /** There is nothing usable in the pantry to cook with. */
    EMPTY_PANTRY,
    ERROR,
}

data class RecipeState(
    val status: RecipeStatus = RecipeStatus.LOADING,
    /** The AI recipe first (when available), followed by local recipes. */
    val recipes: List<Recipe> = emptyList(),
    /** Names of the pantry items the recipes were built around. */
    val usedIngredients: List<String> = emptyList(),
    /** Explains why there is no AI recipe, when that is the case. */
    val aiNotice: String? = null,
    /** Recipe open in the detail view, or `null` while the list is shown. */
    val selectedRecipeId: String? = null,
    /** Indexes of the steps the user ticked off, per recipe id. */
    val completedSteps: Map<String, Set<Int>> = emptyMap(),
    val errorMessage: String? = null,
) {
    val selectedRecipe: Recipe?
        get() = recipes.firstOrNull { it.id == selectedRecipeId }

    fun completedStepsOf(recipeId: String): Set<Int> = completedSteps[recipeId].orEmpty()
}

sealed interface RecipeIntent {
    /** The recipes tab became visible; reloads only if the pantry ingredients changed. */
    data object ScreenShown : RecipeIntent

    /** Forces a new set of recipes. */
    data object Refresh : RecipeIntent
    data class OpenRecipe(val recipeId: String) : RecipeIntent

    /** Goes back from the detail view to the list. */
    data object CloseRecipe : RecipeIntent

    /** Marks a step as done, or as pending again if it was already done. */
    data class ToggleStep(val recipeId: String, val stepIndex: Int) : RecipeIntent
}
