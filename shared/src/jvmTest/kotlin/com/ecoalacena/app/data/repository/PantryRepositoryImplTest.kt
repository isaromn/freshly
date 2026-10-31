package com.ecoalacena.app.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.database.PantryDatabase
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.PantryItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/** Runs the real SQLDelight queries against an in-memory SQLite database. */
class PantryRepositoryImplTest {

    private val repository = PantryRepositoryImpl(
        database = PantryDatabase(
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PantryDatabase.Schema.create(it) },
        ),
        dispatcher = Dispatchers.Unconfined,
    )

    private val rice = PantryItem(
        id = "rice",
        name = "Arroz",
        quantity = 2.0,
        unit = "kg",
        expirationDate = NOW + 300 * MILLIS_PER_DAY,
        isOrganic = false,
        category = Category.PANTRY_STAPLE,
    )

    @Test
    fun eatingPartOfAnItemLeavesTheRestAndCountsOnlyWhatWasEaten() = runTest {
        repository.upsert(rice)

        val savedKg = repository.markConsumed(rice, NOW, quantity = 0.5)

        assertEquals(0.5, savedKg)
        assertEquals(1.5, repository.observeById("rice").first()?.quantity)
        assertEquals(0.5, repository.observeSavedKg().first())
    }

    @Test
    fun eatingAllOfAnItemRemovesIt() = runTest {
        repository.upsert(rice)

        val savedKg = repository.markConsumed(rice, NOW)

        assertEquals(2.0, savedKg)
        assertNull(repository.observeById("rice").first())
    }

    @Test
    fun expiredFoodIsRemovedButDoesNotCount() = runTest {
        val expired = rice.copy(expirationDate = NOW - MILLIS_PER_DAY)
        repository.upsert(expired)

        val savedKg = repository.markConsumed(expired, NOW)

        assertEquals(0.0, savedKg)
        assertEquals(0.0, repository.observeSavedKg().first())
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
