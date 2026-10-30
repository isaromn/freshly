package com.ecoalacena.app.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.currentEpochMillis
import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.core.i18n.currentStrings
import com.ecoalacena.app.domain.repository.PantryRepository
import com.ecoalacena.app.domain.usecase.AnalyzeFoodUseCase
import com.ecoalacena.app.presentation.util.toGeminiUserMessage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScanViewModel(
    private val analyzeFood: AnalyzeFoodUseCase,
    private val repository: PantryRepository,
    private val nowMillis: () -> Long = ::currentEpochMillis,
    /** Texts for the user's language; read each time a message is built. */
    private val strings: () -> AppStrings = ::currentStrings,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val _effects = Channel<ScanEffect>(Channel.BUFFERED)
    val effects: Flow<ScanEffect> = _effects.receiveAsFlow()

    private var analysisJob: Job? = null

    fun onIntent(intent: ScanIntent) {
        when (intent) {
            is ScanIntent.ImageSelected -> analyze(intent.image)
            is ScanIntent.ImagePickFailed -> sendEffect(ScanEffect.ShowMessage(intent.message))
            ScanIntent.Retry -> _state.value.image?.let(::analyze)
            ScanIntent.Reset -> reset()
            is ScanIntent.RemoveDraft -> removeDraft(intent.id)
            is ScanIntent.AdjustExpiration -> adjustExpiration(intent.id, intent.deltaDays)
            ScanIntent.Confirm -> confirm()
            ScanIntent.LoadDemoReceipt -> loadDemoReceipt()
        }
    }

    private fun analyze(image: ScanImage) {
        analysisJob?.cancel()
        _state.value = ScanState(status = ScanStatus.ANALYZING, image = image)

        analysisJob = viewModelScope.launch {
            val result = analyzeFood(image.bytes, image.mimeType)
            val drafts = result.getOrNull()
            val texts = strings()

            _state.update { current ->
                when {
                    drafts == null -> current.copy(
                        status = ScanStatus.ERROR,
                        errorMessage = result.exceptionOrNull().toGeminiUserMessage(
                            strings = texts,
                            fallback = texts.analyzeError,
                        ),
                    )

                    drafts.isEmpty() -> current.copy(
                        status = ScanStatus.ERROR,
                        errorMessage = texts.noFoodFound,
                    )

                    else -> current.copy(
                        status = ScanStatus.REVIEW,
                        drafts = drafts,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    /** Shows the preloaded receipt for review instantly: no network, no API key, no image. */
    private fun loadDemoReceipt() {
        analysisJob?.cancel()

        _state.value = analyzeFood.demoReceipt().fold(
            onSuccess = { drafts -> ScanState(status = ScanStatus.REVIEW, drafts = drafts) },
            onFailure = {
                ScanState(
                    status = ScanStatus.ERROR,
                    errorMessage = strings().demoLoadError,
                )
            },
        )
    }

    private fun reset() {
        analysisJob?.cancel()
        _state.value = ScanState()
    }

    private fun removeDraft(id: String) {
        _state.update { current ->
            current.copy(drafts = current.drafts.filterNot { it.id == id })
        }
    }

    private fun adjustExpiration(id: String, deltaDays: Int) {
        val now = nowMillis()
        _state.update { current ->
            current.copy(
                drafts = current.drafts.map { draft ->
                    if (draft.id != id) {
                        draft
                    } else {
                        val base = draft.expirationDate ?: now
                        val adjusted = (base + deltaDays * MILLIS_PER_DAY).coerceAtLeast(now)
                        draft.copy(expirationDate = adjusted)
                    }
                },
            )
        }
    }

    private fun confirm() {
        val current = _state.value
        if (current.status != ScanStatus.REVIEW || current.drafts.isEmpty()) return

        _state.update { it.copy(status = ScanStatus.SAVING) }

        viewModelScope.launch {
            try {
                current.drafts.forEach { draft -> repository.upsert(draft) }
                _state.value = ScanState()
                sendEffect(ScanEffect.NavigateBack)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _state.update { it.copy(status = ScanStatus.REVIEW) }
                sendEffect(ScanEffect.ShowMessage(strings().saveItemsError))
            }
        }
    }

    private fun sendEffect(effect: ScanEffect) {
        _effects.trySend(effect)
    }
}
