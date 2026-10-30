package com.ecoalacena.app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryTest {

    @Test
    fun fromRawMatchesEnumNamesIgnoringCaseAndSeparators() {
        assertEquals(Category.DAIRY, Category.fromRaw("DAIRY"))
        assertEquals(Category.DAIRY, Category.fromRaw("dairy"))
        assertEquals(Category.PANTRY_STAPLE, Category.fromRaw("Pantry Staple"))
        assertEquals(Category.FRUITS_VEGETABLES, Category.fromRaw(" fruits-vegetables "))
    }

    @Test
    fun fromRawMapsLegacyAndLooseWords() {
        assertEquals(Category.FRUITS_VEGETABLES, Category.fromRaw("fruits"))
        assertEquals(Category.FRUITS_VEGETABLES, Category.fromRaw("vegetables"))
        assertEquals(Category.MEAT_FISH, Category.fromRaw("meat"))
        assertEquals(Category.MEAT_FISH, Category.fromRaw("fish"))
        assertEquals(Category.BAKERY, Category.fromRaw("bread"))
        assertEquals(Category.BEVERAGES, Category.fromRaw("drinks"))
        assertEquals(Category.PANTRY_STAPLE, Category.fromRaw("canned"))
        assertEquals(Category.PANTRY_STAPLE, Category.fromRaw("snacks"))
    }

    @Test
    fun fromRawFallsBackToOther() {
        assertEquals(Category.OTHER, Category.fromRaw(null))
        assertEquals(Category.OTHER, Category.fromRaw(""))
        assertEquals(Category.OTHER, Category.fromRaw("frozen"))
        assertEquals(Category.OTHER, Category.fromRaw("???"))
    }
}
