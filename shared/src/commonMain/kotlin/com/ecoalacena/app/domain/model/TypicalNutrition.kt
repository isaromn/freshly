package com.ecoalacena.app.domain.model

import kotlin.math.roundToInt

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

/**
 * Typical nutrition of one [PantryItem.nutritionBasis] of this item: 100 g or 100 ml, or one piece
 * or pack of its average weight. `null` when its unit has no known weight.
 */
fun PantryItem.typicalNutritionPerBasis(): NutritionPer100g? {
    val basisKg = when (nutritionBasis) {
        NutritionBasis.PER_100_G, NutritionBasis.PER_100_ML -> HUNDRED_GRAMS_IN_KG
        NutritionBasis.PER_PIECE, NutritionBasis.PER_PACK -> copy(quantity = 1.0).estimatedWeightKg()
    } ?: return null
    val typical = category.typicalNutritionPer100g()
    val hundredGrams = basisKg * HUNDRED_GRAMS_PER_KG
    return NutritionPer100g(
        calories = typical.calories * hundredGrams,
        protein = typical.protein * hundredGrams,
        carbs = typical.carbs * hundredGrams,
        fat = typical.fat * hundredGrams,
    )
}

/** Typical kcal of one [PantryItem.nutritionBasis] of this item, rounded; see [typicalNutritionPerBasis]. */
fun PantryItem.typicalCaloriesPerBasis(): Int? = typicalNutritionPerBasis()?.calories?.roundToInt()

/**
 * This item with [calories] per [PantryItem.nutritionBasis] (typed by the user or the typical
 * value), and protein, carbs and fat taken from its category in the same proportion.
 */
fun PantryItem.withCalories(calories: Int): PantryItem {
    val typical = typicalNutritionPerBasis()
    val scale = typical?.calories?.takeIf { it > 0.0 }?.let { calories / it }
    return copy(
        caloriesPerUnit = calories,
        proteinGrams = scale?.let { (typical.protein * it).toOneDecimal() },
        carbsGrams = scale?.let { (typical.carbs * it).toOneDecimal() },
        fatGrams = scale?.let { (typical.fat * it).toOneDecimal() },
    )
}

private fun Double.toOneDecimal(): Float = ((this * 10).roundToInt() / 10.0).toFloat()

private const val HUNDRED_GRAMS_IN_KG = 0.1
