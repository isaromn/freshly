package com.ecoalacena.app.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals

class CalculateImpactUseCaseTest {

    private val calculateImpact = CalculateImpactUseCase()

    @Test
    fun zeroKilogramsProducesZeroImpact() {
        val metrics = calculateImpact(0.0)

        assertEquals(0.0, metrics.savedKg)
        assertEquals(0.0, metrics.moneySavedCop)
        assertEquals(0.0, metrics.moneySavedUsd)
        assertEquals(0.0, metrics.co2AvoidedKg)
        assertEquals(0.0, metrics.treesEquivalent)
        assertEquals(0.0, metrics.carKmEquivalent)
    }

    @Test
    fun tenKilogramsScalesEveryMetric() {
        val metrics = calculateImpact(10.0)

        val expectedCo2 = 10.0 * CalculateImpactUseCase.CO2E_KG_PER_KG_FOOD
        val expectedCop = 10.0 * CalculateImpactUseCase.AVERAGE_PRICE_COP_PER_KG

        assertEquals(10.0, metrics.savedKg)
        assertEquals(expectedCop, metrics.moneySavedCop)
        assertEquals(expectedCop / CalculateImpactUseCase.COP_PER_USD, metrics.moneySavedUsd)
        assertEquals(expectedCo2, metrics.co2AvoidedKg)
        assertEquals(expectedCo2 / CalculateImpactUseCase.CO2_KG_ABSORBED_PER_TREE_YEAR, metrics.treesEquivalent)
        assertEquals(expectedCo2 / CalculateImpactUseCase.CO2_KG_PER_CAR_KM, metrics.carKmEquivalent)
    }

    @Test
    fun impactGrowsLinearlyWithKilograms() {
        val one = calculateImpact(1.0)
        val four = calculateImpact(4.0)

        assertEquals(one.moneySavedCop * 4, four.moneySavedCop)
        assertEquals(one.co2AvoidedKg * 4, four.co2AvoidedKg)
    }

    @Test
    fun negativeKilogramsAreTreatedAsZero() {
        val metrics = calculateImpact(-5.0)

        assertEquals(0.0, metrics.savedKg)
        assertEquals(0.0, metrics.co2AvoidedKg)
    }
}
