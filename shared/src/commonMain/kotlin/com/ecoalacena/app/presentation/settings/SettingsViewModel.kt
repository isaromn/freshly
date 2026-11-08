package com.ecoalacena.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.core.i18n.currentStrings
import com.ecoalacena.app.data.notification.ExpirationNotifier
import com.ecoalacena.app.domain.model.NotificationSettings
import com.ecoalacena.app.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Notification preferences. Saving them is enough: `PantryAlerts.keepInSync` sees the change and
 * plans every alert again.
 */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val notifier: ExpirationNotifier,
    private val strings: () -> AppStrings = ::currentStrings,
) : ViewModel() {

    val state: StateFlow<SettingsState> = settings.observeNotificationSettings()
        .map { SettingsState(isLoading = false, settings = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    /** What to do once the system answers the permission request. */
    private var afterPermission: PendingAction? = null

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.NotificationsToggled ->
                if (intent.enabled) askPermission(PendingAction.ENABLE) else update { it.copy(enabled = false) }

            is SettingsIntent.LeadSelected -> update { it.copy(lead = intent.lead) }
            is SettingsIntent.AlertHourSelected -> update { it.copy(alertHour = intent.hour) }
            is SettingsIntent.HouseholdUpdatesToggled -> update { it.copy(householdUpdates = intent.enabled) }
            SettingsIntent.SendTestAlert -> askPermission(PendingAction.TEST_ALERT)
            is SettingsIntent.PermissionResult -> onPermissionResult(intent.granted)
        }
    }

    private fun askPermission(action: PendingAction) {
        afterPermission = action
        _effects.trySend(SettingsEffect.RequestPermission)
    }

    private fun onPermissionResult(granted: Boolean) {
        val action = afterPermission ?: return
        afterPermission = null
        val texts = strings()

        if (!granted) {
            _effects.trySend(SettingsEffect.ShowMessage(texts.notificationsDenied))
            return
        }
        when (action) {
            PendingAction.ENABLE -> update { it.copy(enabled = true) }
            PendingAction.TEST_ALERT -> {
                notifier.showInstantNotification(texts.testAlertTitle, texts.testAlertBody)
                _effects.trySend(SettingsEffect.ShowMessage(texts.testAlertSent))
            }
        }
    }

    private fun update(change: (NotificationSettings) -> NotificationSettings) {
        val current = state.value.settings
        viewModelScope.launch { settings.saveNotificationSettings(change(current)) }
    }

    private enum class PendingAction { ENABLE, TEST_ALERT }
}
