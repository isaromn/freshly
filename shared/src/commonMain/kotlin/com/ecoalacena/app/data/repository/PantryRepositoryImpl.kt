package com.ecoalacena.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.ecoalacena.app.database.PantryDatabase
import com.ecoalacena.app.database.PantryDatabaseQueries
import com.ecoalacena.app.database.PantryItemEntity
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.PantryEvent
import com.ecoalacena.app.domain.model.PantryEventKind
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

    override fun observeSavedCalories(): Flow<Double> =
        queries.selectTotalSavedCalories()
            .asFlow()
            .mapToOne(dispatcher)

    override fun observeSavedProteinGrams(): Flow<Double> =
        queries.selectTotalSavedProtein()
            .asFlow()
            .mapToOne(dispatcher)

    override suspend fun upsert(item: PantryItem) {
        withContext(dispatcher) {
            queries.insertItem(item)
        }
    }

    override suspend fun deleteById(id: String) {
        withContext(dispatcher) {
            queries.deleteById(id)
        }
    }

    override suspend fun discard(item: PantryItem, discardedAtMillis: Long) {
        withContext(dispatcher) {
            database.transaction {
                queries.deleteById(item.id)
                queries.insertEvent(
                    item.name,
                    item.category.name,
                    item.quantity,
                    item.unit,
                    PantryEventKind.DISCARDED.name,
                    discardedAtMillis,
                )
            }
        }
    }

    override fun observeEventsSince(sinceMillis: Long): Flow<List<PantryEvent>> =
        queries.selectEventsSince(sinceMillis)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities ->
                entities.map { entity ->
                    PantryEvent(
                        name = entity.name,
                        category = Category.fromRaw(entity.category),
                        quantity = entity.quantity,
                        unit = entity.unit,
                        kind = PantryEventKind.entries.firstOrNull { it.name == entity.kind } ?: PantryEventKind.CONSUMED,
                        happenedAt = entity.happenedAt,
                    )
                }
            }

    override suspend fun markConsumed(item: PantryItem, consumedAtMillis: Long, quantity: Double): Double =
        withContext(dispatcher) {
            val eaten = quantity.coerceIn(0.0, item.quantity)
            val remaining = item.quantity - eaten

            database.transactionWithResult {
                if (remaining <= LEFTOVER_EPSILON) {
                    queries.deleteById(item.id)
                } else {
                    queries.insertItem(item.copy(quantity = remaining))
                }

                queries.insertEvent(
                    item.name,
                    item.category.name,
                    eaten,
                    item.unit,
                    PantryEventKind.CONSUMED.name,
                    consumedAtMillis,
                )

                val eatenPart = item.copy(quantity = eaten)
                val weightKg = eatenPart.estimatedWeightKg()
                val wasExpired = item.expirationDate?.let { it < consumedAtMillis } ?: false
                if (weightKg != null && weightKg > 0.0 && !wasExpired) {
                    queries.insertSavedFood(
                        weightKg,
                        consumedAtMillis,
                        eatenPart.totalCalories(),
                        eatenPart.totalProteinGrams(),
                    )
                    weightKg
                } else {
                    0.0
                }
            }
        }

    private companion object {
        /** Less than this is left over from rounding, not food: the item is removed. */
        const val LEFTOVER_EPSILON = 1e-6
    }
}

private fun PantryDatabaseQueries.insertItem(item: PantryItem) {
    insertItem(
        item.id,
        item.name,
        item.category.name,
        item.quantity,
        item.unit,
        item.expirationDate,
        if (item.isOrganic) 1L else 0L,
        item.caloriesPerUnit?.toLong(),
        item.proteinGrams?.toDouble(),
        item.carbsGrams?.toDouble(),
        item.fatGrams?.toDouble(),
    )
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
        caloriesPerUnit = caloriesPerUnit?.toInt(),
        proteinGrams = proteinGrams?.toFloat(),
        carbsGrams = carbsGrams?.toFloat(),
        fatGrams = fatGrams?.toFloat(),
    )
