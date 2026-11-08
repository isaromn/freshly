package com.ecoalacena.app.testing

import com.ecoalacena.app.data.notification.ExpirationNotifier
import com.ecoalacena.app.domain.model.NotificationSettings
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Records every call instead of showing notifications. */
class FakeNotifier(var permissionGranted: Boolean = true) : ExpirationNotifier {

    data class Scheduled(val item: PantryItem, val daysBefore: Int, val alertHour: Int, val notifyIfLate: Boolean)

    val scheduled = mutableListOf<Scheduled>()
    val cancelled = mutableListOf<String>()
    val shown = mutableListOf<Pair<String, String>>()

    override suspend fun scheduleExpirationAlert(
        item: PantryItem,
        daysBefore: Int,
        alertHour: Int,
        notifyIfLate: Boolean,
    ) {
        scheduled += Scheduled(item, daysBefore, alertHour, notifyIfLate)
    }

    override fun cancelNotification(itemId: String) {
        cancelled += itemId
    }

    override fun showInstantNotification(title: String, message: String) {
        shown += title to message
    }

    override suspend fun requestPermission(): Boolean = permissionGranted
}

class FakeSettingsRepository(initial: NotificationSettings = NotificationSettings()) : SettingsRepository {

    val settings = MutableStateFlow(initial)

    override fun observeNotificationSettings(): Flow<NotificationSettings> = settings

    override suspend fun saveNotificationSettings(settings: NotificationSettings) {
        this.settings.value = settings
    }
}
