package com.ecoalacena.app.domain.usecase

import com.ecoalacena.app.domain.model.ImpactMetrics

/**
 * Converts kilograms of food saved from waste into money, CO2e and everyday equivalences.
 *
 * All factors are estimates, shown to the user as such in the impact dashboard.
 */
class CalculateImpactUseCase {

    operator fun invoke(savedKg: Double): ImpactMetrics {
        val kg = savedKg.coerceAtLeast(0.0)
        val co2Kg = kg * CO2E_KG_PER_KG_FOOD
        val copSaved = kg * AVERAGE_PRICE_COP_PER_KG

        return ImpactMetrics(
            savedKg = kg,
            moneySavedCop = copSaved,
            moneySavedUsd = copSaved / COP_PER_USD,
            co2AvoidedKg = co2Kg,
            treesEquivalent = co2Kg / CO2_KG_ABSORBED_PER_TREE_YEAR,
            carKmEquivalent = co2Kg / CO2_KG_PER_CAR_KM,
        )
    }

    companion object {
        /** Greenhouse gases embedded in 1 kg of wasted food (commonly cited FAO/WRAP average). */
        const val CO2E_KG_PER_KG_FOOD = 2.5

        /** Rough average retail price of 1 kg of groceries in Colombia. Adjust to taste. */
        const val AVERAGE_PRICE_COP_PER_KG = 9_000.0

        /** Approximate official exchange rate (TRM) in October 2026. */
        const val COP_PER_USD = 3_175.0

        /** Commonly used average for an established tree (ranges from 10 to 40 kg). */
        const val CO2_KG_ABSORBED_PER_TREE_YEAR = 25.0

        /** EPA: a typical passenger vehicle emits about 404 g of CO2 per mile (~0.251 kg/km). */
        const val CO2_KG_PER_CAR_KM = 0.251
    }
}
