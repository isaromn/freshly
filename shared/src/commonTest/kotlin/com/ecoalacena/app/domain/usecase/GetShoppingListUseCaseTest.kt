package com.ecoalacena.app.domain.usecase

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.ShoppingReason
import com.ecoalacena.app.testing.FakePantryRepository
import com.ecoalacena.app.testing.FakeShoppingRepository
import com.ecoalacena.app.testing.TEST_NOW
import com.ecoalacena.app.testing.testItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class GetShoppingListUseCaseTest {

    private val pantry = FakePantryRepository()
    private val shopping = FakeShoppingRepository()
    private var now = TEST_NOW
    private val useCase = GetShoppingListUseCase(pantry, shopping, nowMillis = { now })

    private val milk = testItem("milk", "Leche", expiresInDays = 5, quantity = 2.0, unit = "l", category = Category.DAIRY)

    @Test
    fun suggestsWhatWasEatenRecentlyWithItsAmount() = runTest {
        pantry.markConsumed(milk, TEST_NOW - MILLIS_PER_DAY)

        val item = useCase().first().single()

        assertEquals("Leche", item.name)
        assertEquals(2.0, item.quantity)
        assertEquals("l", item.unit)
        assertEquals(ShoppingReason.CONSUMED, item.reason)
    }

    @Test
    fun ignoresWhatWasUsedUpMoreThanAWeekAgo() = runTest {
        pantry.markConsumed(milk, TEST_NOW - 8 * MILLIS_PER_DAY)

        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun thrownAwayFoodIsSuggestedAsExpired() = runTest {
        pantry.discard(milk, TEST_NOW - MILLIS_PER_DAY)

        assertEquals(ShoppingReason.EXPIRED, useCase().first().single().reason)
    }

    @Test
    fun suggestsFoodThatExpiredInThePantryThisWeek() = runTest {
        pantry.upsert(testItem("bread", "Pan", expiresInDays = -2, unit = "unit"))
        pantry.upsert(testItem("old", "Arroz viejo", expiresInDays = -30))

        val item = useCase().first().single()

        assertEquals("Pan", item.name)
        assertEquals(ShoppingReason.EXPIRED, item.reason)
    }

    @Test
    fun doesNotSuggestWhatIsStillFreshInThePantry() = runTest {
        pantry.markConsumed(milk, TEST_NOW - MILLIS_PER_DAY)
        pantry.upsert(milk.copy(id = "milk-2", name = "  leche "))

        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun groupsTheSameProductAndAddsUpTheAmounts() = runTest {
        pantry.markConsumed(milk.copy(id = "a", quantity = 1.0), TEST_NOW - 2 * MILLIS_PER_DAY)
        pantry.markConsumed(milk.copy(id = "b", quantity = 1.5, name = "LECHE"), TEST_NOW - MILLIS_PER_DAY)

        val item = useCase().first().single()

        assertEquals(2.5, item.quantity)
        assertEquals("LECHE", item.name)
    }

    @Test
    fun addsProductsByHandOnlyOnce() = runTest {
        useCase.add("  Café   molido ")
        useCase.add("café molido")
        useCase.add("   ")

        val item = useCase().first().single()
        assertEquals("Café molido", item.name)
        assertEquals(ShoppingReason.MANUAL, item.reason)
        assertEquals(null, item.quantity)
    }

    @Test
    fun aProductAddedByHandIsNotRepeatedAsASuggestion() = runTest {
        pantry.markConsumed(milk, TEST_NOW - MILLIS_PER_DAY)
        useCase.add("leche")

        assertEquals(listOf(ShoppingReason.MANUAL), useCase().first().map { it.reason })
    }

    @Test
    fun removingAManualItemDeletesIt() = runTest {
        useCase.add("Café")

        useCase.remove(useCase().first().single())

        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun aRemovedSuggestionComesBackOnlyWhenTheProductRunsOutAgain() = runTest {
        pantry.markConsumed(milk, TEST_NOW - MILLIS_PER_DAY)
        useCase.remove(useCase().first().single())
        assertTrue(useCase().first().isEmpty())

        now = TEST_NOW + MILLIS_PER_DAY
        pantry.markConsumed(milk.copy(id = "milk-2"), now)

        assertEquals("Leche", useCase().first().single().name)
    }
}
