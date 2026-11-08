package com.ecoalacena.app.domain.repository

import com.ecoalacena.app.domain.model.NotificationSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    /** Emits the notification settings every time they change (the defaults when never saved). */
    fun observeNotificationSettings(): Flow<NotificationSettings>

    suspend fun saveNotificationSettings(settings: NotificationSettings)
}
