package com.ecoalacena.app.domain.model

/** Nutrition of 100 g of a food. */
class NutritionPer100g(val calories: Double, val protein: Double, val carbs: Double, val fat: Double)

/** Typical nutrition of 100 g of a food of each category, for items that have none of their own. */
fun Category.typicalNutritionPer100g(): NutritionPer100g =
    when (this) {
        Category.FRUITS_VEGETABLES -> NutritionPer100g(calories = 45.0, protein = 1.2, carbs = 9.0, fat = 0.3)
        Category.DAIRY -> NutritionPer100g(calories = 120.0, protein = 7.0, carbs = 6.0, fat = 7.0)
        Category.MEAT_FISH -> NutritionPer100g(calories = 170.0, protein = 22.0, carbs = 0.0, fat = 9.0)
        Category.BAKERY -> NutritionPer100g(calories = 270.0, protein = 9.0, carbs = 50.0, fat = 4.0)
        Category.PANTRY_STAPLE -> NutritionPer100g(calories = 340.0, protein = 10.0, carbs = 65.0, fat = 3.0)
        Category.BEVERAGES -> NutritionPer100g(calories = 40.0, protein = 0.5, carbs = 9.0, fat = 0.0)
        Category.OTHER -> NutritionPer100g(calories = 150.0, protein = 5.0, carbs = 20.0, fat = 5.0)
    }

/**
 * Estimated kcal of the whole item: its own value when the scanner gave one, otherwise the typical
 * value of its category for its estimated weight. `null` when the weight cannot be estimated either.
 */
fun PantryItem.estimatedTotalCalories(): Double? =
    totalCalories() ?: typicalTotal { it.calories }

/** Estimated grams of protein of the whole item, like [estimatedTotalCalories]. */
fun PantryItem.estimatedTotalProteinGrams(): Double? =
    totalProteinGrams() ?: typicalTotal { it.protein }

private inline fun PantryItem.typicalTotal(value: (NutritionPer100g) -> Double): Double? {
    val kg = estimatedWeightKg()?.takeIf { it > 0.0 } ?: return null
    return value(category.typicalNutritionPer100g()) * kg * HUNDRED_GRAMS_PER_KG
}

private const val HUNDRED_GRAMS_PER_KG = 10
