package com.ecoalacena.app.presentation.impact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoalacena.app.domain.model.wastedKg
import com.ecoalacena.app.domain.repository.PantryRepository
import com.ecoalacena.app.domain.usecase.CalculateImpactUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ImpactViewModel(
    repository: PantryRepository,
    private val calculateImpact: CalculateImpactUseCase,
) : ViewModel() {

    private val selectedCurrency = MutableStateFlow(ImpactCurrency.COP)
    private val wastedCurrency = MutableStateFlow(ImpactCurrency.COP)

    val state: StateFlow<ImpactState> = combine(
        repository.observeSavedKg(),
        repository.observeSavedCalories(),
        repository.observeSavedProteinGrams(),
        // Every thrown-away item ever recorded: the money lost is a running total, like the money saved.
        repository.observeEventsSince(0L),
        // Each money card has its own COP / USD buttons.
        combine(selectedCurrency, wastedCurrency, ::Pair),
    ) { savedKg, savedCalories, savedProteinGrams, events, (currency, wasted) ->
        ImpactState(
            isLoading = false,
            metrics = calculateImpact(savedKg, savedCalories, savedProteinGrams, wastedKg = events.wastedKg()),
            currency = currency,
            wastedCurrency = wasted,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ImpactState(),
    )

    fun onIntent(intent: ImpactIntent) {
        when (intent) {
            is ImpactIntent.SelectCurrency -> selectedCurrency.value = intent.currency
            is ImpactIntent.SelectWastedCurrency -> wastedCurrency.value = intent.currency
        }
    }
}
