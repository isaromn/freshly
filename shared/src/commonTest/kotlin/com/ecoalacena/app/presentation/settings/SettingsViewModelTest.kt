package com.ecoalacena.app.presentation.settings

import com.ecoalacena.app.core.i18n.EnglishStrings
import com.ecoalacena.app.domain.model.AlertLead
import com.ecoalacena.app.domain.model.NotificationSettings
import com.ecoalacena.app.testing.FakeNotifier
import com.ecoalacena.app.testing.FakeSettingsRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val settings = FakeSettingsRepository(NotificationSettings(enabled = false))
    private val notifier = FakeNotifier()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun turningNotificationsOnAsksForPermissionFirst() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.NotificationsToggled(true))

        assertEquals(SettingsEffect.RequestPermission, viewModel.effects.first())
        assertFalse(settings.settings.value.enabled)

        viewModel.onIntent(SettingsIntent.PermissionResult(granted = true))
        assertTrue(viewModel.state.value.settings.enabled)
    }

    @Test
    fun aDeniedPermissionKeepsNotificationsOffAndExplainsWhy() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.NotificationsToggled(true))
        viewModel.onIntent(SettingsIntent.PermissionResult(granted = false))

        assertFalse(settings.settings.value.enabled)
        val effects = listOf(viewModel.effects.first(), viewModel.effects.first())
        assertEquals(SettingsEffect.ShowMessage(EnglishStrings.notificationsDenied), effects.last())
    }

    @Test
    fun leadHourAndHouseholdChoicesAreSaved() = runTest {
        settings.settings.value = NotificationSettings()
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.LeadSelected(AlertLead.SAME_DAY))
        viewModel.onIntent(SettingsIntent.AlertHourSelected(18))
        viewModel.onIntent(SettingsIntent.HouseholdUpdatesToggled(false))

        assertEquals(
            NotificationSettings(lead = AlertLead.SAME_DAY, alertHour = 18, householdUpdates = false),
            settings.settings.value,
        )
    }

    @Test
    fun theTestAlertIsShownOnceAllowed() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SendTestAlert)
        viewModel.onIntent(SettingsIntent.PermissionResult(granted = true))

        assertEquals(listOf(EnglishStrings.testAlertTitle to EnglishStrings.testAlertBody), notifier.shown)
    }

    private fun viewModel() = SettingsViewModel(settings, notifier, strings = { EnglishStrings })
}
