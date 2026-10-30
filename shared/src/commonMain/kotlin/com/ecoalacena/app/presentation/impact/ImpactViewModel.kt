package com.ecoalacena.app.presentation.impact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    val state: StateFlow<ImpactState> = combine(
        repository.observeSavedKg(),
        selectedCurrency,
    ) { savedKg, currency ->
        ImpactState(
            isLoading = false,
            metrics = calculateImpact(savedKg),
            currency = currency,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ImpactState(),
    )

    fun onIntent(intent: ImpactIntent) {
        when (intent) {
            is ImpactIntent.SelectCurrency -> selectedCurrency.value = intent.currency
        }
    }
}
