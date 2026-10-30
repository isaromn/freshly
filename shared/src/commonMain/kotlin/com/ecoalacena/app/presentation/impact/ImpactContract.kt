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
)

sealed interface ImpactIntent {
    data class SelectCurrency(val currency: ImpactCurrency) : ImpactIntent
}
