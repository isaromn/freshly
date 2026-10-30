package com.ecoalacena.app.domain.model

/**
 * Guesses the [Category] of a product from its name, in Spanish or English. It is the fallback
 * for when the AI (or the user) gives no usable category: "Whole Wheat Bread" -> BAKERY,
 * "Blueberries" -> FRUITS_VEGETABLES, "Leche entera" -> DAIRY.
 */
internal object FoodClassifier {

    /** The category suggested by the words in [name], or `null` when none is recognised. */
    fun categoryOf(name: String): Category? {
        val tokens = name.foodTokens()
        if (tokens.isEmpty()) return null

        // Phrases are checked on the normalised text, e.g. " peanut butter". They match from the start of a
        // word, so plurals work too ("green beans").
        val text = tokens.joinToString(separator = " ", prefix = " ")

        // The order matters: a processed form or a drink wins over the ingredient it names
        // ("apple juice", "tomato sauce", "chicken soup"), and dairy wins over fruit ("strawberry yogurt").
        return when {
            FoodKeywords.PRODUCE_PHRASES.any { " $it" in text } -> Category.FRUITS_VEGETABLES
            FoodKeywords.PANTRY_PHRASES.any { " $it" in text } -> Category.PANTRY_STAPLE
            tokens.hasAny(FoodKeywords.PANTRY_FORMS) -> Category.PANTRY_STAPLE
            tokens.hasAny(FoodKeywords.BEVERAGES) -> Category.BEVERAGES
            tokens.hasAny(FoodKeywords.MEAT_FISH) -> Category.MEAT_FISH
            tokens.hasAny(FoodKeywords.DAIRY) -> Category.DAIRY
            tokens.hasAny(FoodKeywords.BAKERY) -> Category.BAKERY
            tokens.hasAny(FoodKeywords.FRUITS_VEGETABLES) -> Category.FRUITS_VEGETABLES
            tokens.hasAny(FoodKeywords.PANTRY_STAPLES) -> Category.PANTRY_STAPLE
            else -> null
        }
    }
}
