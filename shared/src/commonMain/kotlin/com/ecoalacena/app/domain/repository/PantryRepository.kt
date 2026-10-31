package com.ecoalacena.app.domain.repository

import com.ecoalacena.app.domain.model.PantryItem
import kotlinx.coroutines.flow.Flow

interface PantryRepository {

    /** Emits the full pantry every time the underlying data changes. */
    fun observeAll(): Flow<List<PantryItem>>

    /** Emits the item with [id], or `null` if it does not exist. */
    fun observeById(id: String): Flow<PantryItem?>

    /**
     * Emits items that expire on or before [thresholdEpochMillis],
     * including items that are already expired.
     */
    fun observeExpiringSoon(thresholdEpochMillis: Long): Flow<List<PantryItem>>

    /** Emits the total kilograms of food consumed before it expired. */
    fun observeSavedKg(): Flow<Double>

    /** Inserts the item, or replaces it if one with the same id already exists. */
    suspend fun upsert(item: PantryItem)

    suspend fun deleteById(id: String)

    /**
     * Records that [quantity] of [item] was eaten (all of it by default). The item is removed when
     * nothing is left, otherwise its quantity goes down. If it was still fresh and the weight of the
     * eaten part can be estimated, that weight is added to the "waste avoided" total.
     *
     * @return the kilograms added to the "waste avoided" total (0 when nothing was counted).
     */
    suspend fun markConsumed(item: PantryItem, consumedAtMillis: Long, quantity: Double = item.quantity): Double
}
