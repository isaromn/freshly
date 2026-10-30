package com.ecoalacena.app.presentation.scan

import com.ecoalacena.app.domain.model.PantryItem

/** A photo chosen or taken by the user. Compared by reference, not by content. */
class ScanImage(
    val bytes: ByteArray,
    val mimeType: String,
)

enum class ScanStatus {
    /** Waiting for the user to take or pick a photo. */
    IDLE,
    ANALYZING,

    /** Gemini answered; the user is reviewing the suggested products. */
    REVIEW,
    ERROR,
    SAVING,
}

data class ScanState(
    val status: ScanStatus = ScanStatus.IDLE,
    val image: ScanImage? = null,
    /** Products suggested by Gemini, not yet saved to the pantry. */
    val drafts: List<PantryItem> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface ScanIntent {
    data class ImageSelected(val image: ScanImage) : ScanIntent
    data class ImagePickFailed(val message: String) : ScanIntent
    data object Retry : ScanIntent

    /** Discards the current photo and analysis and returns to the initial state. */
    data object Reset : ScanIntent
    data class RemoveDraft(val id: String) : ScanIntent
    data class AdjustExpiration(val id: String, val deltaDays: Int) : ScanIntent
    data object Confirm : ScanIntent

    /** Demo mode: loads a preloaded receipt, with no network or API key needed. */
    data object LoadDemoReceipt : ScanIntent
}

sealed interface ScanEffect {
    data object NavigateBack : ScanEffect
    data class ShowMessage(val message: String) : ScanEffect
}
