package com.ecoalacena.app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PantryItemTest {

    private fun item(
        quantity: Double,
        unit: String,
        category: Category = Category.OTHER,
    ) = PantryItem(
        id = "id",
        name = "Producto",
        quantity = quantity,
        unit = unit,
        expirationDate = null,
        isOrganic = false,
        category = category,
    )

    @Test
    fun estimatedWeightConvertsMassAndVolumeUnits() {
        assertEquals(2.0, item(2.0, "kg").estimatedWeightKg())
        assertEquals(0.5, item(500.0, "g").estimatedWeightKg())
        assertEquals(1.5, item(1.5, "l").estimatedWeightKg())
        assertEquals(0.25, item(250.0, "ml").estimatedWeightKg())
    }

    @Test
    fun itemsCountedByUnitUseTheAverageWeightOfTheirCategory() {
        assertEquals(0.6, item(4.0, "unit", Category.FRUITS_VEGETABLES).estimatedWeightKg()!!, TOLERANCE)
        assertEquals(1.5, item(6.0, "unit", Category.DAIRY).estimatedWeightKg()!!, TOLERANCE)
        assertEquals(0.8, item(2.0, "unit", Category.MEAT_FISH).estimatedWeightKg()!!, TOLERANCE)
        assertEquals(0.9, item(3.0, "unit").estimatedWeightKg()!!, TOLERANCE)
    }

    @Test
    fun packsUseAnAverageWeight() {
        assertEquals(1.0, item(2.0, "pack").estimatedWeightKg()!!, TOLERANCE)
    }

    @Test
    fun anUnrecognisedUnitHasNoEstimatedWeight() {
        assertNull(item(1.0, "bunch").estimatedWeightKg())
    }

    @Test
    fun categoryDefaultsToOther() {
        assertEquals(Category.OTHER, item(1.0, "kg").category)
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
