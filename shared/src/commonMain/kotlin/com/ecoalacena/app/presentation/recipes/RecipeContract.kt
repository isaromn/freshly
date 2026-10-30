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
    val expandedRecipeId: String? = null,
    val errorMessage: String? = null,
)

sealed interface RecipeIntent {
    /** The recipes tab became visible; reloads only if the pantry ingredients changed. */
    data object ScreenShown : RecipeIntent

    /** Forces a new set of recipes. */
    data object Refresh : RecipeIntent
    data class ToggleRecipe(val recipeId: String) : RecipeIntent
}
