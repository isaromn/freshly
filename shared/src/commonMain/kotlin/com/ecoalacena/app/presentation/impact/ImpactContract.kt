package com.ecoalacena.app.presentation.impact

import com.ecoalacena.app.domain.model.ImpactMetrics

enum class ImpactCurrency {
    COP,
    USD,
}

data class ImpactState(
    val isLoading: Boolean = true,
    val metrics: ImpactMetrics = ImpactMetrics.EMPTY,
    /** Currency of the "money saved" card. */
    val currency: ImpactCurrency = ImpactCurrency.COP,
    /** Currency of the "money lost to waste" card, chosen on its own. */
    val wastedCurrency: ImpactCurrency = ImpactCurrency.COP,
)

sealed interface ImpactIntent {
    data class SelectCurrency(val currency: ImpactCurrency) : ImpactIntent
    data class SelectWastedCurrency(val currency: ImpactCurrency) : ImpactIntent
}
