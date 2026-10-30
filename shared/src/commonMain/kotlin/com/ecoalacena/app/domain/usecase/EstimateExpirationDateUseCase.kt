package com.ecoalacena.app.domain.usecase

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.currentEpochMillis
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.FoodClassifier
import com.ecoalacena.app.domain.model.FoodKeywords
import com.ecoalacena.app.domain.model.foodTokens
import com.ecoalacena.app.domain.model.hasAny

/**
 * Estimates when a product will expire when the user does not know (or did not type) the date.
 *
 * It uses average shelf lives by category, refined by the words in the name (Spanish or English):
 *
 * | Product                                   | Days |
 * |-------------------------------------------|------|
 * | Dairy and yogurt                          | 14   |
 * | Bakery and cookies                        | 7    |
 * | Fresh fruit and vegetables                | 7    |
 * | Berries, leafy greens, mushrooms, herbs   | 5    |
 * | Fresh meat and fish                       | 5    |
 * | Cold cuts and sausages                    | 10   |
 * | Eggs                                      | 30   |
 * | Canned goods, dry grains and pasta        | 365  |
 * | Other pantry goods and beverages          | 180  |
 * | Anything else                             | 30   |
 *
 * These are approximations for products stored the usual way, not a food-safety guarantee: the
 * user can always correct the date.
 */
class EstimateExpirationDateUseCase(
    private val nowMillis: () -> Long = ::currentEpochMillis,
) {

    /**
     * The estimated expiration date, in epoch milliseconds, counting from [purchasedAtMillis]
     * (today by default).
     */
    operator fun invoke(
        name: String,
        category: Category,
        purchasedAtMillis: Long = nowMillis(),
    ): Long = purchasedAtMillis + shelfLifeDays(name, category) * MILLIS_PER_DAY

    /** Average number of days the product stays good. */
    fun shelfLifeDays(name: String, category: Category): Int {
        val tokens = name.foodTokens()

        // When the category is the generic default, the name often says more ("Yogurt", "Eggs").
        val effectiveCategory = if (category == Category.OTHER) {
            FoodClassifier.categoryOf(name) ?: Category.OTHER
        } else {
            category
        }

        return daysFromName(tokens, effectiveCategory) ?: effectiveCategory.averageShelfLifeDays()
    }

    private fun daysFromName(tokens: List<String>, category: Category): Int? =
        when {
            category.isDairyOrGeneric() && tokens.hasAny(FoodKeywords.EGGS) -> EGGS_DAYS
            tokens.hasAny(FoodKeywords.CANNED_FORMS) -> CANNED_DAYS
            category.isPantryOrGeneric() && tokens.hasAny(FoodKeywords.DRY_STAPLES) -> DRY_STAPLE_DAYS
            category.isMeatOrGeneric() && tokens.hasAny(FoodKeywords.COLD_CUTS) -> COLD_CUT_DAYS
            category == Category.FRUITS_VEGETABLES && tokens.hasAny(FoodKeywords.DELICATE_PRODUCE) ->
                DELICATE_PRODUCE_DAYS
            else -> null
        }

    private fun Category.isDairyOrGeneric() = this == Category.DAIRY || this == Category.OTHER

    private fun Category.isPantryOrGeneric() = this == Category.PANTRY_STAPLE || this == Category.OTHER

    private fun Category.isMeatOrGeneric() = this == Category.MEAT_FISH || this == Category.OTHER

    private fun Category.averageShelfLifeDays(): Int =
        when (this) {
            Category.DAIRY -> DAIRY_DAYS
            Category.BAKERY -> BAKERY_DAYS
            Category.FRUITS_VEGETABLES -> PRODUCE_DAYS
            Category.MEAT_FISH -> MEAT_DAYS
            Category.PANTRY_STAPLE -> PANTRY_DAYS
            Category.BEVERAGES -> BEVERAGE_DAYS
            Category.OTHER -> DEFAULT_DAYS
        }

    private companion object {
        const val DAIRY_DAYS = 14
        const val BAKERY_DAYS = 7
        const val PRODUCE_DAYS = 7
        const val DELICATE_PRODUCE_DAYS = 5
        const val MEAT_DAYS = 5
        const val COLD_CUT_DAYS = 10
        const val EGGS_DAYS = 30
        const val CANNED_DAYS = 365
        const val DRY_STAPLE_DAYS = 365
        const val PANTRY_DAYS = 180
        const val BEVERAGE_DAYS = 180
        const val DEFAULT_DAYS = 30
    }
}
