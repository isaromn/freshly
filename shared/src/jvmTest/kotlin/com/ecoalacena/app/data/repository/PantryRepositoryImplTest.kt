package com.ecoalacena.app.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.database.PantryDatabase
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.ManualShoppingItem
import com.ecoalacena.app.domain.model.PantryEventKind
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

    @Test
    fun eatingAndThrowingAwayAreKeptInTheHistory() = runTest {
        val milk = rice.copy(id = "milk", name = "Leche", unit = "l", category = Category.DAIRY)
        repository.upsert(rice)
        repository.upsert(milk)

        repository.markConsumed(rice, NOW, quantity = 0.5)
        repository.discard(milk, NOW + 1)

        val events = repository.observeEventsSince(NOW - MILLIS_PER_DAY).first()
        assertEquals(listOf(PantryEventKind.DISCARDED, PantryEventKind.CONSUMED), events.map { it.kind })
        assertEquals(0.5, events.last().quantity)
        assertNull(repository.observeById("milk").first())
    }

    @Test
    fun aDatabaseFromVersionOneGetsTheShoppingTablesAndNutrition() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // The tables of version 1, as they were before the shopping list existed.
        driver.execute(
            null,
            """
            CREATE TABLE pantryItemEntity (
                id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL DEFAULT 'OTHER',
                quantity REAL NOT NULL, unit TEXT NOT NULL, expirationDate INTEGER, isOrganic INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            0,
        )
        driver.execute(
            null,
            "CREATE TABLE savedFoodEntity (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, kg REAL NOT NULL, savedAt INTEGER NOT NULL)",
            0,
        )

        PantryDatabase.Schema.migrate(driver, 1, PantryDatabase.Schema.version)

        val database = PantryDatabase(driver)
        val shopping = ShoppingRepositoryImpl(database, Dispatchers.Unconfined)
        shopping.addManualItem(ManualShoppingItem("1", "Café", NOW))
        assertEquals(listOf("Café"), shopping.observeManualItems().first().map { it.name })

        val pantry = PantryRepositoryImpl(database, Dispatchers.Unconfined)
        pantry.upsert(rice.copy(caloriesPerUnit = 130))
        assertEquals(130, pantry.observeById("rice").first()?.caloriesPerUnit)
        assertEquals(4L, PantryDatabase.Schema.version)
    }

    @Test
    fun nutritionIsStoredAndEatingInTimeAddsItsCalories() = runTest {
        val apples = rice.copy(
            id = "apples",
            name = "Manzanas",
            quantity = 4.0,
            unit = "unit",
            caloriesPerUnit = 95,
            proteinGrams = 0.5f,
            carbsGrams = 25f,
            fatGrams = 0.3f,
        )
        repository.upsert(apples)
        assertEquals(apples, repository.observeById("apples").first())

        repository.markConsumed(apples, NOW, quantity = 2.0)

        // Two apples of 95 kcal; the two left keep their nutrition.
        assertEquals(190.0, repository.observeSavedCalories().first())
        assertEquals(1.0, repository.observeSavedProteinGrams().first())
        assertEquals(95, repository.observeById("apples").first()?.caloriesPerUnit)
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
