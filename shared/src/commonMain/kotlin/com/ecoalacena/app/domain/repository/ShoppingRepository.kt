package com.ecoalacena.app.domain.repository

import com.ecoalacena.app.domain.model.ManualShoppingItem
import kotlinx.coroutines.flow.Flow

interface ShoppingRepository {

    /** Products the user added by hand, oldest first. */
    fun observeManualItems(): Flow<List<ManualShoppingItem>>

    /** When each removed suggestion was dismissed, by normalised name (see `shoppingKey`). */
    fun observeDismissals(): Flow<Map<String, Long>>

    suspend fun addManualItem(item: ManualShoppingItem)

    suspend fun removeManualItem(id: String)

    /** Hides the suggestion for [nameKey] until the product is used up or expires again. */
    suspend fun dismissSuggestion(nameKey: String, dismissedAtMillis: Long)
}
