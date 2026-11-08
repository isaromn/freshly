package com.ecoalacena.app.data.notification

import com.ecoalacena.app.domain.model.NotificationSettings
import com.ecoalacena.app.domain.model.PantryItem

/**
 * Local notifications of the app. [NotificationService] implements it on every platform; the
 * shared code depends on this interface so tests can use a fake.
 */
interface ExpirationNotifier {

    /**
     * Schedules (or replaces) the alert of [item] for [daysBefore] days before it expires, at
     * [alertHour] local time. Items without an expiration date, already expired, or whose alert
     * time has passed get no alert, unless [notifyIfLate] asks to alert right away instead (used
     * for food that was just added and is already close to expiring).
     */
    suspend fun scheduleExpirationAlert(
        item: PantryItem,
        daysBefore: Int = 2,
        alertHour: Int = NotificationSettings.DEFAULT_ALERT_HOUR,
        notifyIfLate: Boolean = false,
    )

    /** Cancels the pending alert of the item with [itemId] and removes it if it is on screen. */
    fun cancelNotification(itemId: String)

    /** Shows a notification now. */
    fun showInstantNotification(title: String, message: String)

    /**
     * Asks the system for permission to notify when it can (iOS) and returns whether notifications
     * are allowed. On Android 13+ the dialog needs an activity, so the screen asks through
     * `rememberNotificationPermissionRequest` and this only reports the current state.
     */
    suspend fun requestPermission(): Boolean
}
