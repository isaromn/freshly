package com.ecoalacena.app.presentation.recipes

import com.ecoalacena.app.domain.model.ImpactMetrics
import com.ecoalacena.app.domain.model.PantryItem
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
    /** The "recipe finished" sheet, or `null` when it is closed. */
    val completion: RecipeCompletion? = null,
    val errorMessage: String? = null,
) {
    val selectedRecipe: Recipe?
        get() = recipes.firstOrNull { it.id == selectedRecipeId }

    fun completedStepsOf(recipeId: String): Set<Int> = completedSteps[recipeId].orEmpty()
}

/** A pantry item offered for discounting when a recipe is finished; all of it is selected at first. */
data class CompletionIngredient(
    val item: PantryItem,
    val selected: Boolean = true,
    val quantity: Double = item.quantity,
)

enum class CompletionPhase {
    /** The user reviews which ingredients were used and how much. */
    SELECTING,
    SAVING,

    /** The ingredients were discounted: the impact of the meal is shown. */
    CELEBRATING,
}

data class RecipeCompletion(
    val recipeId: String,
    /** The recipe's ingredients that are still in the pantry. */
    val ingredients: List<CompletionIngredient>,
    val phase: CompletionPhase = CompletionPhase.SELECTING,
    /** Impact of this meal alone, once it was discounted. */
    val impact: ImpactMetrics? = null,
    val errorMessage: String? = null,
) {
    val selectedCount: Int
        get() = ingredients.count { it.selected }
}

sealed interface RecipeIntent {
    /** The recipes tab became visible; reloads only if the pantry ingredients changed. */
    data object ScreenShown : RecipeIntent

    /** Forces a new set of recipes. */
    data object Refresh : RecipeIntent
    data class OpenRecipe(val recipeId: String) : RecipeIntent

    /** Goes back from the detail view to the list. */
    data object CloseRecipe : RecipeIntent

    /** Marks a step as done, or as pending again. Ticking off the last step finishes the recipe. */
    data class ToggleStep(val recipeId: String, val stepIndex: Int) : RecipeIntent

    /** Opens the "recipe finished" sheet with the pantry items the recipe used. */
    data class FinishRecipe(val recipeId: String) : RecipeIntent

    /** Selects or unselects an ingredient to discount. */
    data class ToggleCompletionIngredient(val itemId: String) : RecipeIntent

    /** Changes how much of an ingredient was used (between 0 and what is in the pantry). */
    data class SetCompletionQuantity(val itemId: String, val quantity: Double) : RecipeIntent

    /** Discounts the selected ingredients from the pantry. */
    data object ConfirmCompletion : RecipeIntent

    /** Closes the sheet; after the celebration it also goes back to the list. */
    data object DismissCompletion : RecipeIntent
}
