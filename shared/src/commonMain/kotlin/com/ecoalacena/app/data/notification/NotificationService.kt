package com.ecoalacena.app.data.notification

import com.ecoalacena.app.domain.model.PantryItem

/**
 * Native local notifications:
 * - Android: a "Pantry Alerts" channel, alerts scheduled with WorkManager (they survive reboots).
 * - iOS: `UNUserNotificationCenter` with calendar triggers.
 * - Desktop: system tray messages, scheduled while the app is open.
 *
 * When and what to notify is decided in shared code by [ExpirationAlertPlanner].
 */
expect class NotificationService : ExpirationNotifier {
    override suspend fun scheduleExpirationAlert(
        item: PantryItem,
        daysBefore: Int,
        alertHour: Int,
        notifyIfLate: Boolean,
    )

    override fun cancelNotification(itemId: String)

    override fun showInstantNotification(title: String, message: String)

    override suspend fun requestPermission(): Boolean
}
