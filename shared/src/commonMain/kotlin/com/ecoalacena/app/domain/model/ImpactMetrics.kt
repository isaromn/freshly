package com.ecoalacena.app.domain.model

/** Environmental and economic impact of the food that was eaten before it expired (SDG 12). */
data class ImpactMetrics(
    val savedKg: Double,
    val moneySavedCop: Double,
    val moneySavedUsd: Double,
    val co2AvoidedKg: Double,
    /** Trees needed to absorb the avoided CO2e during one year. */
    val treesEquivalent: Double,
    /** Kilometres a typical car would drive to emit the avoided CO2e. */
    val carKmEquivalent: Double,
) {
    companion object {
        val EMPTY = ImpactMetrics(
            savedKg = 0.0,
            moneySavedCop = 0.0,
            moneySavedUsd = 0.0,
            co2AvoidedKg = 0.0,
            treesEquivalent = 0.0,
            carKmEquivalent = 0.0,
        )
    }
}
