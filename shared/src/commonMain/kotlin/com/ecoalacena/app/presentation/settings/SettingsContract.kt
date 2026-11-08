package com.ecoalacena.app.presentation.settings

import com.ecoalacena.app.domain.model.AlertLead
import com.ecoalacena.app.domain.model.NotificationSettings

data class SettingsState(
    val isLoading: Boolean = true,
    val settings: NotificationSettings = NotificationSettings(),
)

sealed interface SettingsIntent {
    data class NotificationsToggled(val enabled: Boolean) : SettingsIntent
    data class LeadSelected(val lead: AlertLead) : SettingsIntent
    data class AlertHourSelected(val hour: Int) : SettingsIntent
    data class HouseholdUpdatesToggled(val enabled: Boolean) : SettingsIntent
    data object SendTestAlert : SettingsIntent

    /** The answer of the system permission dialog the screen opened after [SettingsEffect.RequestPermission]. */
    data class PermissionResult(val granted: Boolean) : SettingsIntent
}

sealed interface SettingsEffect {
    /** Ask the system for permission to notify, then send [SettingsIntent.PermissionResult]. */
    data object RequestPermission : SettingsEffect
    data class ShowMessage(val message: String) : SettingsEffect
}
