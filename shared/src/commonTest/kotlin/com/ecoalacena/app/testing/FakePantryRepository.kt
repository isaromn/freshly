package com.ecoalacena.app.testing

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.domain.model.Category
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

    override suspend fun markConsumed(item: PantryItem, consumedAtMillis: Long) {
        items.value = items.value.filterNot { it.id == item.id }

        val weightKg = item.estimatedWeightKg()
        val wasExpired = item.expirationDate?.let { it < consumedAtMillis } ?: false
        if (weightKg != null && !wasExpired) {
            savedKg.value += weightKg
        }
    }
}
