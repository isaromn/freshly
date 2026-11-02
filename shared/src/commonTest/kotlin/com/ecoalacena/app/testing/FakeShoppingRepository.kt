package com.ecoalacena.app.testing

import com.ecoalacena.app.domain.model.ManualShoppingItem
import com.ecoalacena.app.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [ShoppingRepository]. */
class FakeShoppingRepository : ShoppingRepository {

    val manualItems = MutableStateFlow<List<ManualShoppingItem>>(emptyList())
    val dismissals = MutableStateFlow<Map<String, Long>>(emptyMap())

    override fun observeManualItems(): Flow<List<ManualShoppingItem>> = manualItems

    override fun observeDismissals(): Flow<Map<String, Long>> = dismissals

    override suspend fun addManualItem(item: ManualShoppingItem) {
        manualItems.value = manualItems.value.filterNot { it.id == item.id } + item
    }

    override suspend fun removeManualItem(id: String) {
        manualItems.value = manualItems.value.filterNot { it.id == id }
    }

    override suspend fun dismissSuggestion(nameKey: String, dismissedAtMillis: Long) {
        dismissals.value = dismissals.value + (nameKey to dismissedAtMillis)
    }
}
