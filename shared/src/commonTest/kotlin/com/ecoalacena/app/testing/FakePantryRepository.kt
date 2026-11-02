package com.ecoalacena.app.testing

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.PantryEvent
import com.ecoalacena.app.domain.model.PantryEventKind
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.repository.PantryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** A fixed "now" shared by tests so that expirations are deterministic. */
const val TEST_NOW = 1_700_000_000_000L

/** Builds a [PantryItem] expiring [expiresInDays] days after [TEST_NOW] (`null` = no date). */
fun testItem(
    id: String,
    name: String = id,
    expiresInDays: Int? = null,
    quantity: Double = 1.0,
    unit: String = "kg",
    category: Category = Category.OTHER,
): PantryItem =
    PantryItem(
        id = id,
        name = name,
        quantity = quantity,
        unit = unit,
        expirationDate = expiresInDays?.let { TEST_NOW + it * MILLIS_PER_DAY },
        isOrganic = false,
        category = category,
    )

/** In-memory [PantryRepository] that behaves like the real one for the cases tests need. */
class FakePantryRepository(initialItems: List<PantryItem> = emptyList()) : PantryRepository {

    val items = MutableStateFlow(initialItems)
    val savedKg = MutableStateFlow(0.0)
    val savedCalories = MutableStateFlow(0.0)

    override fun observeSavedCalories(): Flow<Double> = savedCalories
    val events = MutableStateFlow<List<PantryEvent>>(emptyList())

    override suspend fun discard(item: PantryItem, discardedAtMillis: Long) {
        items.value = items.value.filterNot { it.id == item.id }
        events.value = listOf(item.toEvent(item.quantity, PantryEventKind.DISCARDED, discardedAtMillis)) + events.value
    }

    override fun observeEventsSince(sinceMillis: Long): Flow<List<PantryEvent>> =
        events.map { list -> list.filter { it.happenedAt >= sinceMillis } }

    private fun PantryItem.toEvent(quantity: Double, kind: PantryEventKind, at: Long) =
        PantryEvent(name, category, quantity, unit, kind, at)

    override fun observeAll(): Flow<List<PantryItem>> = items

    override fun observeById(id: String): Flow<PantryItem?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override fun observeExpiringSoon(thresholdEpochMillis: Long): Flow<List<PantryItem>> =
        items.map { list ->
            list.filter { item -> item.expirationDate?.let { it <= thresholdEpochMillis } ?: false }
        }

    override fun observeSavedKg(): Flow<Double> = savedKg

    override suspend fun upsert(item: PantryItem) {
        items.value = items.value.filterNot { it.id == item.id } + item
    }

    override suspend fun deleteById(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }

    override suspend fun markConsumed(item: PantryItem, consumedAtMillis: Long, quantity: Double): Double {
        val eaten = quantity.coerceIn(0.0, item.quantity)
        val remaining = item.quantity - eaten
        items.value = items.value.filterNot { it.id == item.id } +
            listOfNotNull(item.copy(quantity = remaining).takeIf { remaining > 1e-6 })
        events.value = listOf(item.toEvent(eaten, PantryEventKind.CONSUMED, consumedAtMillis)) + events.value

        val weightKg = item.copy(quantity = eaten).estimatedWeightKg()
        val wasExpired = item.expirationDate?.let { it < consumedAtMillis } ?: false
        if (weightKg == null || weightKg <= 0.0 || wasExpired) return 0.0
        savedKg.value += weightKg
        savedCalories.value += item.copy(quantity = eaten).totalCalories() ?: 0.0
        return weightKg
    }
}
