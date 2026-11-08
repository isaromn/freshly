package com.ecoalacena.app.domain.model

enum class Category {
    FRUITS_VEGETABLES,
    DAIRY,
    MEAT_FISH,
    BAKERY,
    PANTRY_STAPLE,
    BEVERAGES,
    OTHER;

    companion object {

        /** The short codes the vision AI answers with. */
        private val AI_CODES = mapOf(
            "FRUIT_VEG" to FRUITS_VEGETABLES,
            "MEAT" to MEAT_FISH,
            "BEVERAGE" to BEVERAGES,
        )

        private val FRUITS_VEGETABLES_WORDS = listOf("FRUIT", "VEGETABLE", "PRODUCE", "FRUTA", "VERDURA")
        private val DAIRY_WORDS = listOf("DAIRY", "MILK", "CHEESE", "YOGURT", "EGG", "LACTEO", "HUEVO")
        private val MEAT_FISH_WORDS = listOf("MEAT", "FISH", "SEAFOOD", "POULTRY", "CARNE", "PESCADO", "POLLO")
        private val BAKERY_WORDS = listOf("BAKERY", "BREAD", "PASTRY", "COOKIE", "PANADERIA", "GALLETA")
        private val BEVERAGES_WORDS = listOf("BEVERAGE", "DRINK", "JUICE", "SODA", "BEBIDA", "JUGO")
        private val PANTRY_STAPLE_WORDS = listOf(
            "PANTRY", "STAPLE", "GRAIN", "CANNED", "SNACK", "CONDIMENT", "GROCERY", "ENLATADO", "DESPENSA",
        )

        /**
         * Maps the text returned by the AI (or stored in the database) to a [Category].
         * Unknown or missing values fall back to [OTHER].
         */
        fun fromRaw(raw: String?): Category {
            val key = raw
                ?.trim()
                ?.uppercase()
                ?.replace(Regex("[^A-Z]+"), "_")
                ?.trim('_')
                .orEmpty()

            Category.entries.firstOrNull { it.name == key }?.let { return it }
            AI_CODES[key]?.let { return it }

            return when {
                FRUITS_VEGETABLES_WORDS.any { it in key } -> FRUITS_VEGETABLES
                DAIRY_WORDS.any { it in key } -> DAIRY
                MEAT_FISH_WORDS.any { it in key } -> MEAT_FISH
                BAKERY_WORDS.any { it in key } -> BAKERY
                BEVERAGES_WORDS.any { it in key } -> BEVERAGES
                PANTRY_STAPLE_WORDS.any { it in key } -> PANTRY_STAPLE
                else -> OTHER
            }
        }
    }
}

/** The amount of food the nutrition fields of a [PantryItem] describe. */
enum class NutritionBasis {
    PER_100_G,
    PER_100_ML,
    PER_PIECE,
    PER_PACK,
}

data class PantryItem(
    val id: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val expirationDate: Long?,
    val isOrganic: Boolean,
    val category: Category = Category.OTHER,
    /**
     * Estimated energy, in kcal, of one [nutritionBasis]: per 100 g (or 100 ml) for items measured
     * by weight or volume, per piece or pack for items counted by `unit` or `pack`.
     * `null` when it is not known (for example items typed by hand).
     */
    val caloriesPerUnit: Int? = null,
    /** Grams of protein per [nutritionBasis]. */
    val proteinGrams: Float? = null,
    /** Grams of carbohydrates per [nutritionBasis]. */
    val carbsGrams: Float? = null,
    /** Grams of fat per [nutritionBasis]. */
    val fatGrams: Float? = null,
) {

    /** What the nutrition fields refer to: 100 g / 100 ml, or one piece or pack. */
    val nutritionBasis: NutritionBasis
        get() = when (unit.lowercase()) {
            "kg", "g" -> NutritionBasis.PER_100_G
            "l", "ml" -> NutritionBasis.PER_100_ML
            "pack" -> NutritionBasis.PER_PACK
            else -> NutritionBasis.PER_PIECE
        }

    /** Estimated kcal of the whole item ([quantity] of it), or `null` when the calories are unknown. */
    fun totalCalories(): Double? = caloriesPerUnit?.let { it * nutritionBasisCount() }

    /** Estimated grams of protein of the whole item, or `null` when the protein is unknown. */
    fun totalProteinGrams(): Double? = proteinGrams?.let { it * nutritionBasisCount() }

    /** Estimated grams of carbohydrates of the whole item, or `null` when they are unknown. */
    fun totalCarbsGrams(): Double? = carbsGrams?.let { it * nutritionBasisCount() }

    /** Estimated grams of fat of the whole item, or `null` when it is unknown. */
    fun totalFatGrams(): Double? = fatGrams?.let { it * nutritionBasisCount() }

    /** How many times [nutritionBasis] fits in [quantity]: 1 kg holds ten portions of 100 g. */
    private fun nutritionBasisCount(): Double =
        when (unit.lowercase()) {
            "kg", "l" -> quantity * 10
            "g", "ml" -> quantity / 100
            else -> quantity
        }

    /**
     * Rough weight in kilograms, or `null` when the unit is not recognised.
     *
     * Mass and volume units are converted (1 litre is approximated as 1 kg). Items counted by
     * `unit` or `pack` use an average weight, so "6 eggs" or "3 apples" also count towards the
     * impact figures. These are estimates, as the impact dashboard says.
     */
    fun estimatedWeightKg(): Double? =
        when (unit.lowercase()) {
            "kg", "l" -> quantity
            "g", "ml" -> quantity / 1000.0
            "unit" -> quantity * category.averageUnitWeightKg()
            "pack" -> quantity * AVERAGE_PACK_WEIGHT_KG
            else -> null
        }

    private companion object {
        /** Average weight of one pack (a tray of yogurts, a bag of pasta...). */
        const val AVERAGE_PACK_WEIGHT_KG = 0.5
    }
}

/** Average weight of one item of this category, used when the quantity is counted in units. */
private fun Category.averageUnitWeightKg(): Double =
    when (this) {
        Category.FRUITS_VEGETABLES -> 0.15
        Category.DAIRY -> 0.25
        Category.MEAT_FISH -> 0.4
        Category.BAKERY -> 0.15
        Category.PANTRY_STAPLE -> 0.5
        Category.BEVERAGES -> 0.5
        Category.OTHER -> 0.3
    }
