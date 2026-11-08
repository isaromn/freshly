package com.ecoalacena.app.data.notification

import com.ecoalacena.app.core.i18n.EnglishStrings
import com.ecoalacena.app.domain.model.AlertLead
import com.ecoalacena.app.domain.model.NotificationSettings
import com.ecoalacena.app.testing.FakeNotifier
import com.ecoalacena.app.testing.FakePantryRepository
import com.ecoalacena.app.testing.FakeSettingsRepository
import com.ecoalacena.app.testing.TEST_NOW
import com.ecoalacena.app.testing.testItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AlertingPantryRepositoryTest {

    private val notifier = FakeNotifier()
    private val settings = FakeSettingsRepository(NotificationSettings(lead = AlertLead.ONE_DAY, alertHour = 8))
    private val pantry = FakePantryRepository()
    private val alerts = PantryAlerts(notifier, settings, strings = { EnglishStrings })
    private val repository = AlertingPantryRepository(pantry, alerts)

    private val milk = testItem("milk", "Whole Milk", expiresInDays = 3, quantity = 2.0)

    @Test
    fun newFoodGetsAnAlertWithTheUsersSettings() = runTest {
        repository.upsert(milk)

        assertEquals(listOf(FakeNotifier.Scheduled(milk, daysBefore = 1, alertHour = 8, notifyIfLate = true)), notifier.scheduled)
    }

    @Test
    fun editingFoodReplacesTheAlertWithoutAlertingRightAway() = runTest {
        repository.upsert(milk)
        repository.upsert(milk.copy(quantity = 1.0))

        assertEquals(listOf(true, false), notifier.scheduled.map { it.notifyIfLate })
    }

    @Test
    fun withNotificationsOffSavingCancelsInsteadOfScheduling() = runTest {
        settings.settings.value = NotificationSettings(enabled = false)

        repository.upsert(milk)

        assertTrue(notifier.scheduled.isEmpty())
        assertEquals(listOf("milk"), notifier.cancelled)
    }

    @Test
    fun removingOrDiscardingFoodCancelsItsAlert() = runTest {
        repository.deleteById("milk")
        repository.discard(testItem("bread"), TEST_NOW)

        assertEquals(listOf("milk", "bread"), notifier.cancelled)
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun usingTheLastOfAnItemCancelsItsAlertAndTellsTheHousehold() = runTest {
        pantry.upsert(milk)

        repository.markConsumed(milk, TEST_NOW)

        assertEquals(listOf("milk"), notifier.cancelled)
        assertEquals(listOf(EnglishStrings.usedUpAlertTitle("Whole Milk")), notifier.shown.map { it.first })
    }

    @Test
    fun usingPartOfAnItemKeepsItsAlert() = runTest {
        pantry.upsert(milk)

        repository.markConsumed(milk, TEST_NOW, quantity = 0.5)

        assertTrue(notifier.cancelled.isEmpty())
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun householdUpdatesCanBeTurnedOff() = runTest {
        settings.settings.value = NotificationSettings(householdUpdates = false)
        pantry.upsert(milk)

        repository.markConsumed(milk, TEST_NOW)

        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun changingTheSettingsPlansEveryAlertAgain() = runTest {
        val bread = testItem("bread", expiresInDays = 2)
        pantry.upsert(milk)
        pantry.upsert(bread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            alerts.keepInSync { listOf(milk, bread) }
        }

        settings.settings.value = NotificationSettings(lead = AlertLead.SAME_DAY, alertHour = 20)
        settings.settings.value = NotificationSettings(enabled = false)

        assertEquals(listOf(1, 1, 0, 0), notifier.scheduled.map { it.daysBefore })
        assertEquals(listOf(8, 8, 20, 20), notifier.scheduled.map { it.alertHour })
        assertTrue(notifier.scheduled.none { it.notifyIfLate })
        assertEquals(listOf("milk", "bread"), notifier.cancelled)
    }
}
