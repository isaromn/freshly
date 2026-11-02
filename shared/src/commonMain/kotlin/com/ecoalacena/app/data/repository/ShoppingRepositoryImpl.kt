package com.ecoalacena.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ecoalacena.app.database.PantryDatabase
import com.ecoalacena.app.domain.model.ManualShoppingItem
import com.ecoalacena.app.domain.repository.ShoppingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Shopping list storage in the same local SQLDelight database as the pantry. */
class ShoppingRepositoryImpl(
    database: PantryDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ShoppingRepository {

    private val queries = database.pantryDatabaseQueries

    override fun observeManualItems(): Flow<List<ManualShoppingItem>> =
        queries.selectShoppingItems()
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map { ManualShoppingItem(it.id, it.name, it.addedAt) } }

    override fun observeDismissals(): Flow<Map<String, Long>> =
        queries.selectDismissals()
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.associate { it.nameKey to it.dismissedAt } }

    override suspend fun addManualItem(item: ManualShoppingItem) {
        withContext(dispatcher) { queries.insertShoppingItem(item.id, item.name, item.addedAt) }
    }

    override suspend fun removeManualItem(id: String) {
        withContext(dispatcher) { queries.deleteShoppingItem(id) }
    }

    override suspend fun dismissSuggestion(nameKey: String, dismissedAtMillis: Long) {
        withContext(dispatcher) { queries.insertDismissal(nameKey, dismissedAtMillis) }
    }
}
