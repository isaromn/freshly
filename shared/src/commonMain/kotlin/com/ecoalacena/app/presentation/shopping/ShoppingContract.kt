package com.ecoalacena.app.presentation.shopping

import com.ecoalacena.app.domain.model.ShoppingItem

data class ShoppingState(
    val isLoading: Boolean = true,
    val items: List<ShoppingItem> = emptyList(),
    /** What the user is typing in the "add a product" field. */
    val newItemName: String = "",
)

sealed interface ShoppingIntent {
    data class NewItemNameChanged(val name: String) : ShoppingIntent

    /** Adds what was typed to the list. */
    data object AddItem : ShoppingIntent
    data class RemoveItem(val item: ShoppingItem) : ShoppingIntent

    /** Shares the list (share sheet on phones, clipboard on desktop). */
    data object Share : ShoppingIntent
}

sealed interface ShoppingEffect {
    data class ShowMessage(val message: String) : ShoppingEffect
}
