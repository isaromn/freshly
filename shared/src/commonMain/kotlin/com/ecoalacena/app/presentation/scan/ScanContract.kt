package com.ecoalacena.app.presentation.scan

import com.ecoalacena.app.core.MILLIS_PER_DAY
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
    /** Purchase date printed on the scanned receipt; `null` for photos or when it is unreadable. */
    val receiptDate: Long? = null,
    val errorMessage: String? = null,
) {
    /** Days since the receipt was printed, when it is old enough to warn about it. */
    fun oldReceiptAgeDays(now: Long): Int? {
        val printedAt = receiptDate ?: return null
        val days = ((now - printedAt) / MILLIS_PER_DAY).toInt()
        return days.takeIf { it >= OLD_RECEIPT_DAYS }
    }

    /** Products that can go to the pantry. */
    val freshDrafts: List<PantryItem>
        get() = drafts.filterNot { it.isSpoiled }

    /** Products the scanner saw rotten: they are composted or discarded, never stored as food. */
    val spoiledDrafts: List<PantryItem>
        get() = drafts.filter { it.isSpoiled }
}

/** Receipts at least this old get a warning: their food may have gone off already. */
const val OLD_RECEIPT_DAYS = 7

sealed interface ScanIntent {
    data class ImageSelected(val image: ScanImage) : ScanIntent
    data class ImagePickFailed(val message: String) : ScanIntent
    data object Retry : ScanIntent

    /** Discards the current photo and analysis and returns to the initial state. */
    data object Reset : ScanIntent
    data class RemoveDraft(val id: String) : ScanIntent
    data class AdjustExpiration(val id: String, val deltaDays: Int) : ScanIntent

    /**
     * Corrects the name of a product the AI got wrong; a blank name is ignored. When the new name
     * is a known food, the category follows it ("Manzana" moves the item to fruits & vegetables),
     * and the expiration date is estimated again for the new food.
     */
    data class RenameDraft(val id: String, val name: String) : ScanIntent

    /** Replaces the quantity of a product; values that are not positive are ignored. */
    data class UpdateQuantity(val id: String, val quantity: Double) : ScanIntent

    /** Sets the exact expiration date picked in the calendar (never earlier than now). */
    data class SetExpiration(val id: String, val expirationDate: Long) : ScanIntent
    /** Saves the fresh products to the pantry; spoiled ones are never saved. */
    data object Confirm : ScanIntent

    /** Records a spoiled product as wasted food (it goes to compost) and takes it off the list. */
    data class CompostSpoiled(val id: String) : ScanIntent

    /** Demo mode: loads a preloaded receipt, with no network or API key needed. */
    data object LoadDemoReceipt : ScanIntent
}

sealed interface ScanEffect {
    data object NavigateBack : ScanEffect
    data class ShowMessage(val message: String) : ScanEffect
}
