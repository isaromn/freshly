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
     * Removes [item] from the pantry because it was eaten. If it was still fresh and its
     * weight can be estimated, the weight is added to the "waste avoided" total.
     */
    suspend fun markConsumed(item: PantryItem, consumedAtMillis: Long)
}
