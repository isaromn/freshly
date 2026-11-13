package com.ecoalacena.app.domain.recipe

import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.model.Recipe
import com.ecoalacena.app.domain.model.typicalNutritionPer100g
import kotlin.math.roundToInt

/**
 * Adds a nutrition estimate to an offline recipe, built from the pantry [items] it cooks with.
 *
 * Items with nutrition (from the scanner) use their own values; the rest use a typical value for
 * their category. Cooking oil adds [oilTablespoons] × 120 kcal. The recipe serves about one plate
 * per 350 g of food, between 1 and 8. Items whose weight cannot be estimated are left out; without
 * any weight the recipe keeps no nutrition.
 */
internal fun Recipe.withEstimatedNutrition(items: List<PantryItem>, oilTablespoons: Int = 0): Recipe {
    var weightKg = 0.0
    var calories = 0.0
    var protein = 0.0
    var carbs = 0.0
    var fat = 0.0

    items.forEach { item ->
        val itemKg = item.estimatedWeightKg()?.takeIf { it > 0.0 } ?: return@forEach
        weightKg += itemKg
        val known = item.totalCalories()
        if (known != null) {
            calories += known
            protein += item.totalProteinGrams() ?: 0.0
            carbs += item.totalCarbsGrams() ?: 0.0
            fat += item.totalFatGrams() ?: 0.0
        } else {
            val typical = item.category.typicalNutritionPer100g()
            val hundredGrams = itemKg * 10
            calories += typical.calories * hundredGrams
            protein += typical.protein * hundredGrams
            carbs += typical.carbs * hundredGrams
            fat += typical.fat * hundredGrams
        }
    }
    if (weightKg <= 0.0) return this

    calories += oilTablespoons * OIL_KCAL_PER_TABLESPOON
    fat += oilTablespoons * OIL_FAT_GRAMS_PER_TABLESPOON

    val portions = (weightKg / PORTION_KG).roundToInt().coerceIn(1, MAX_SERVINGS)
    val perServing = (calories / portions).roundToInt().takeIf { it > 0 } ?: return this

    return copy(
        servings = portions,
        caloriesPerServing = perServing,
        totalCalories = perServing * portions,
        proteinGrams = (protein / portions).roundToOneDecimal(),
        carbsGrams = (carbs / portions).roundToOneDecimal(),
        fatGrams = (fat / portions).roundToOneDecimal(),
    )
}

private fun Double.roundToOneDecimal(): Float = ((this * 10).roundToInt() / 10.0).toFloat()

/** About one plate: 350 g of food. */
private const val PORTION_KG = 0.35
private const val MAX_SERVINGS = 8
private const val OIL_KCAL_PER_TABLESPOON = 120.0
private const val OIL_FAT_GRAMS_PER_TABLESPOON = 13.5
