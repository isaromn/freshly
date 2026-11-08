package com.ecoalacena.app.presentation.pantry

import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.PantryItem

/** Freshness "traffic light": fresh, expiring soon and expired, plus a neutral state for items without a date. */
enum class FreshnessStatus {
    FRESH,
    EXPIRING_SOON,
    EXPIRED,
    NO_DATE,
}

data class PantryItemUi(
    val item: PantryItem,
    val freshness: FreshnessStatus,
    /** Whole days until expiration (negative when already expired), or `null` without a date. */
    val daysLeft: Int?,
)

/** The modal that is currently open, if any. */
sealed interface PantryDialog {
    /** Form to add a new item by hand. */
    data object Add : PantryDialog

    /** Same form, pre-filled with [item], to change it. */
    data class Edit(val item: PantryItem) : PantryDialog

    /**
     * Asks the user to confirm that [item] was eaten. [impactKg] is the weight that will be added
     * to the impact figures, or `null` when it will not count (already expired, or weight unknown).
     */
    data class ConfirmConsumed(val item: PantryItem, val impactKg: Double?) : PantryDialog

    /** Asks the user to confirm that [item] was thrown away; it counts as wasted food. */
    data class ConfirmDiscarded(val item: PantryItem) : PantryDialog

}

/** What the user typed in the form. [id] is `null` when a new item is being added. */
data class PantryItemInput(
    val id: String?,
    val name: String,
    val category: Category,
    val quantity: Double,
    val unit: String,
    /** Epoch milliseconds, or `null` when the item has no expiration date. */
    val expirationDate: Long?,
    val isOrganic: Boolean,
)

data class PantryState(
    val isLoading: Boolean = true,
    /** Items after applying the category filter, most urgent first. */
    val items: List<PantryItemUi> = emptyList(),
    val selectedCategory: Category? = null,
    val totalItems: Int = 0,
    val expiringSoonCount: Int = 0,
    val expiredCount: Int = 0,
    /** Every expired item, whatever the category filter, most overdue first. */
    val expiredItems: List<PantryItemUi> = emptyList(),
    /** Kilograms of food consumed before it expired. */
    val savedKg: Double = 0.0,
    val dialog: PantryDialog? = null,
)

sealed interface PantryIntent {
    data class SelectCategory(val category: Category?) : PantryIntent
    data object ScanClicked : PantryIntent

    /** The "Consumed" button was pressed: opens the confirmation, nothing changes yet. */
    data class ConsumedClicked(val itemId: String) : PantryIntent

    /** The "Discarded" button was pressed: opens the confirmation, nothing changes yet. */
    data class DiscardClicked(val itemId: String) : PantryIntent

    /** Confirmed: the item was eaten. Removes it and, if it counts, adds its weight to the impact. */
    data class MarkConsumed(val itemId: String) : PantryIntent

    /** Confirmed: the item was thrown away. It leaves the pantry and is recorded as wasted food. */
    data class DiscardItem(val itemId: String) : PantryIntent

    /** Expired food must not be eaten: takes every expired item out of the pantry as wasted food. */
    data object WasteAllExpired : PantryIntent

    /** Opens the empty form to add an item manually. */
    data object AddItemClicked : PantryIntent

    /** Opens the form pre-filled with an existing item. */
    data class EditItem(val itemId: String) : PantryIntent
    data object DismissDialog : PantryIntent

    /** Inserts the item (new id) or updates it (same id) in the local database. */
    data class SaveItem(val input: PantryItemInput) : PantryIntent
}

sealed interface PantryEffect {
    data object NavigateToScan : PantryEffect
    data class ShowMessage(val message: String) : PantryEffect
}
