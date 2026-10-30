package com.ecoalacena.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.ecoalacena.app.database.PantryDatabase
import com.ecoalacena.app.database.PantryItemEntity
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.repository.PantryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Offline-first repository: the local SQLDelight database is the single source of truth.
 * All reads are reactive [Flow]s that re-emit whenever the table changes.
 */
class PantryRepositoryImpl(
    private val database: PantryDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : PantryRepository {

    private val queries = database.pantryDatabaseQueries

    override fun observeAll(): Flow<List<PantryItem>> =
        queries.selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeById(id: String): Flow<PantryItem?> =
        queries.selectById(id)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { entity -> entity?.toDomain() }

    override fun observeExpiringSoon(thresholdEpochMillis: Long): Flow<List<PantryItem>> =
        queries.selectExpiringSoon(thresholdEpochMillis)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeSavedKg(): Flow<Double> =
        queries.selectTotalSavedKg()
            .asFlow()
            .mapToOne(dispatcher)

    override suspend fun upsert(item: PantryItem) {
        withContext(dispatcher) {
            queries.insertItem(
                item.id,
                item.name,
                item.category.name,
                item.quantity,
                item.unit,
                item.expirationDate,
                if (item.isOrganic) 1L else 0L,
            )
        }
    }

    override suspend fun deleteById(id: String) {
        withContext(dispatcher) {
            queries.deleteById(id)
        }
    }

    override suspend fun markConsumed(item: PantryItem, consumedAtMillis: Long) {
        withContext(dispatcher) {
            database.transaction {
                queries.deleteById(item.id)

                val weightKg = item.estimatedWeightKg()
                val wasExpired = item.expirationDate?.let { it < consumedAtMillis } ?: false
                if (weightKg != null && weightKg > 0.0 && !wasExpired) {
                    queries.insertSavedFood(weightKg, consumedAtMillis)
                }
            }
        }
    }
}

private fun PantryItemEntity.toDomain(): PantryItem =
    PantryItem(
        id = id,
        name = name,
        quantity = quantity,
        unit = unit,
        expirationDate = expirationDate,
        isOrganic = isOrganic != 0L,
        category = Category.fromRaw(category),
    )
