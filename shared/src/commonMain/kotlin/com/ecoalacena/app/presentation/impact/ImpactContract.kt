package com.ecoalacena.app.presentation.impact

import com.ecoalacena.app.domain.model.ImpactMetrics

enum class ImpactCurrency {
    COP,
    USD,
}

data class ImpactState(
    val isLoading: Boolean = true,
    val metrics: ImpactMetrics = ImpactMetrics.EMPTY,
    val currency: ImpactCurrency = ImpactCurrency.COP,
    /** Estimated kcal of the food eaten before it expired (only items whose calories are known). */
    val savedCalories: Double = 0.0,
)

sealed interface ImpactIntent {
    data class SelectCurrency(val currency: ImpactCurrency) : ImpactIntent
}
