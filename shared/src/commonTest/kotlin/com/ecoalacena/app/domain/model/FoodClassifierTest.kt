package com.ecoalacena.app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FoodClassifierTest {

    @Test
    fun recognisesEnglishGroceryTerms() {
        assertCategory(Category.DAIRY, "Milk")
        assertCategory(Category.BAKERY, "Whole Wheat Bread")
        assertCategory(Category.DAIRY, "Eggs")
        assertCategory(Category.FRUITS_VEGETABLES, "Blueberries")
        assertCategory(Category.MEAT_FISH, "Chicken Breast")
        assertCategory(Category.PANTRY_STAPLE, "Rice")
        assertCategory(Category.BEVERAGES, "Orange juice")
    }

    @Test
    fun recognisesSpanishGroceryTerms() {
        assertCategory(Category.DAIRY, "Leche entera")
        assertCategory(Category.BAKERY, "Pan integral")
        assertCategory(Category.DAIRY, "Huevos")
        assertCategory(Category.FRUITS_VEGETABLES, "Arándanos")
        assertCategory(Category.MEAT_FISH, "Pechuga de pollo")
        assertCategory(Category.MEAT_FISH, "Jamón")
        assertCategory(Category.BEVERAGES, "Cerveza")
    }

    @Test
    fun aProcessedFormWinsOverTheIngredientItNames() {
        assertCategory(Category.BEVERAGES, "Apple juice")
        assertCategory(Category.PANTRY_STAPLE, "Tomato sauce")
        assertCategory(Category.PANTRY_STAPLE, "Canned tuna")
        assertCategory(Category.PANTRY_STAPLE, "Chicken soup")
        assertCategory(Category.PANTRY_STAPLE, "Peanut butter")
        assertCategory(Category.PANTRY_STAPLE, "Leche de almendra")
    }

    @Test
    fun dairyWinsOverFruitInAFlavouredProduct() {
        assertCategory(Category.DAIRY, "Strawberry yogurt")
    }

    @Test
    fun matchesWholeWordsOnly() {
        assertCategory(Category.FRUITS_VEGETABLES, "Eggplant")
        assertCategory(Category.FRUITS_VEGETABLES, "Pineapple")
        assertCategory(Category.FRUITS_VEGETABLES, "Green beans")
    }

    @Test
    fun returnsNullWhenNothingIsRecognised() {
        assertNull(FoodClassifier.categoryOf("Xyzzy"))
        assertNull(FoodClassifier.categoryOf(""))
        assertNull(FoodClassifier.categoryOf("   "))
    }

    private fun assertCategory(expected: Category, name: String) {
        assertEquals(expected, FoodClassifier.categoryOf(name), name)
    }
}
