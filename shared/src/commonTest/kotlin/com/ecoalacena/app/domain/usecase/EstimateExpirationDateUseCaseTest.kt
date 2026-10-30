package com.ecoalacena.app.domain.usecase

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.domain.model.Category
import kotlin.test.Test
import kotlin.test.assertEquals

class EstimateExpirationDateUseCaseTest {

    private val estimate = EstimateExpirationDateUseCase { NOW }

    @Test
    fun dairyAndYogurtLastTwoWeeks() {
        assertDays(14, "Leche entera", Category.DAIRY)
        assertDays(14, "Greek Yogurt", Category.DAIRY)
        assertDays(14, "Queso campesino", Category.DAIRY)
    }

    @Test
    fun bakeryAndCookiesLastAWeek() {
        assertDays(7, "Pan tajado", Category.BAKERY)
        assertDays(7, "Whole Wheat Bread", Category.BAKERY)
        assertDays(7, "Cookies", Category.BAKERY)
        assertDays(7, "Galletas de avena", Category.BAKERY)
    }

    @Test
    fun freshProduceLastsFiveToSevenDays() {
        assertDays(7, "Manzanas", Category.FRUITS_VEGETABLES)
        assertDays(7, "Apples", Category.FRUITS_VEGETABLES)
        assertDays(5, "Blueberries", Category.FRUITS_VEGETABLES)
        assertDays(5, "Fresas", Category.FRUITS_VEGETABLES)
        assertDays(5, "Spinach", Category.FRUITS_VEGETABLES)
    }

    @Test
    fun freshMeatLastsFiveDaysAndColdCutsTen() {
        assertDays(5, "Pechuga de pollo", Category.MEAT_FISH)
        assertDays(5, "Ground beef", Category.MEAT_FISH)
        assertDays(10, "Jamón", Category.MEAT_FISH)
        assertDays(10, "Bacon", Category.MEAT_FISH)
        assertDays(10, "Salchichas", Category.MEAT_FISH)
    }

    @Test
    fun eggsLastThirtyDays() {
        assertDays(30, "Huevos", Category.DAIRY)
        assertDays(30, "Eggs", Category.DAIRY)
        assertDays(30, "Eggs", Category.OTHER)
    }

    @Test
    fun cannedGoodsAndDryGrainsLastAYear() {
        assertDays(365, "Arroz blanco", Category.PANTRY_STAPLE)
        assertDays(365, "Lentils", Category.PANTRY_STAPLE)
        assertDays(365, "Canned tuna", Category.PANTRY_STAPLE)
        assertDays(365, "Atún enlatado", Category.PANTRY_STAPLE)
        assertDays(365, "Canned peaches", Category.FRUITS_VEGETABLES)
    }

    @Test
    fun otherPantryGoodsAndBeveragesLastSixMonths() {
        assertDays(180, "Peanut butter", Category.PANTRY_STAPLE)
        assertDays(180, "Gaseosa", Category.BEVERAGES)
    }

    @Test
    fun theNameFillsInWhenTheCategoryIsTheGenericOne() {
        assertDays(14, "Yogurt", Category.OTHER)
        assertDays(7, "Whole Wheat Bread", Category.OTHER)
        assertDays(5, "Lettuce", Category.OTHER)
        assertDays(365, "Lentejas", Category.OTHER)
        assertDays(10, "Bacon", Category.OTHER)
    }

    @Test
    fun anUnrecognisedProductGetsTheDefault() {
        assertDays(30, "Cosa rara", Category.OTHER)
    }

    @Test
    fun matchingIgnoresCaseAccentsAndPlurals() {
        assertDays(5, "ARÁNDANOS", Category.FRUITS_VEGETABLES)
        assertDays(5, "strawberries", Category.FRUITS_VEGETABLES)
        assertDays(10, "JAMÓN", Category.MEAT_FISH)
    }

    @Test
    fun aWordInsideAnotherWordDoesNotMatch() {
        // "eggplant" is a vegetable, not eggs; "egg noodles" are dry pasta.
        assertDays(7, "Eggplant", Category.OTHER)
        assertDays(365, "Egg noodles", Category.PANTRY_STAPLE)
    }

    @Test
    fun theExpirationDateCountsFromToday() {
        assertEquals(NOW + 14 * MILLIS_PER_DAY, estimate("Milk", Category.DAIRY))
    }

    @Test
    fun theExpirationDateCanCountFromAnotherPurchaseDate() {
        val purchasedTwoDaysLater = NOW + 2 * MILLIS_PER_DAY

        val expiration = estimate("Milk", Category.DAIRY, purchasedAtMillis = purchasedTwoDaysLater)

        assertEquals(NOW + 16 * MILLIS_PER_DAY, expiration)
    }

    private fun assertDays(expected: Int, name: String, category: Category) {
        assertEquals(expected, estimate.shelfLifeDays(name, category), "$name ($category)")
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
