package com.ecoalacena.app.data.notification

import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.core.i18n.currentStrings
import com.ecoalacena.app.domain.model.NotificationSettings
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Keeps the notifications in line with the pantry and the user's settings: schedules an alert
 * when food is saved, cancels it when the food leaves the pantry, and tells the household when
 * something runs out.
 */
class PantryAlerts(
    private val notifier: ExpirationNotifier,
    private val settings: SettingsRepository,
    private val strings: () -> AppStrings = ::currentStrings,
) {

    /** [isNew] is `true` the first time the item is saved: if it is already close to expiring, alert now. */
    suspend fun onItemSaved(item: PantryItem, isNew: Boolean) {
        schedule(item, settings.observeNotificationSettings().first(), notifyIfLate = isNew)
    }

    fun onItemRemoved(itemId: String) {
        notifier.cancelNotification(itemId)
    }

    /** The last of [item] was eaten: no more alerts for it, and a household update if enabled. */
    suspend fun onItemUsedUp(item: PantryItem) {
        notifier.cancelNotification(item.id)
        val current = settings.observeNotificationSettings().first()
        if (current.enabled && current.householdUpdates) {
            val texts = strings()
            notifier.showInstantNotification(texts.usedUpAlertTitle(item.name), texts.usedUpAlertBody)
        }
    }

    /**
     * Plans the alerts of every item in [pantryItems] once now and again whenever the settings
     * change (turned off, other lead or hour). Desktop timers do not outlive the app, so this is
     * also what brings them back on start. Runs until the calling coroutine is cancelled.
     */
    suspend fun keepInSync(pantryItems: suspend () -> List<PantryItem>) {
        settings.observeNotificationSettings().collect { current ->
            pantryItems().forEach { item -> schedule(item, current, notifyIfLate = false) }
        }
    }

    private suspend fun schedule(item: PantryItem, current: NotificationSettings, notifyIfLate: Boolean) {
        if (!current.enabled || item.expirationDate == null) {
            notifier.cancelNotification(item.id)
        } else {
            notifier.scheduleExpirationAlert(
                item = item,
                daysBefore = current.lead.daysBefore,
                alertHour = current.alertHour,
                notifyIfLate = notifyIfLate,
            )
        }
    }
}
