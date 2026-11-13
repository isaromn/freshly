package com.ecoalacena.app.data.notification

import com.ecoalacena.app.core.currentEpochMillis
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.platform.DesktopTray
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.schedule

/**
 * Desktop notifications are messages of Freshly's tray icon (Notification Center on macOS, toasts
 * on Windows). A desktop app cannot wake itself up, so alerts are timers of the running app: closing
 * the window keeps Freshly in the menu bar or tray (see [DesktopTray]) and the alerts keep coming
 * until the user quits. `PantryAlerts.keepInSync` plans them again every time the app starts.
 */
actual class NotificationService(
    private val planner: ExpirationAlertPlanner,
) : ExpirationNotifier {

    private val timer = Timer("freshly-alerts", true)
    private val pending = ConcurrentHashMap<String, TimerTask>()

    actual override suspend fun scheduleExpirationAlert(
        item: PantryItem,
        daysBefore: Int,
        alertHour: Int,
        notifyIfLate: Boolean,
    ) {
        pending.remove(item.id)?.cancel()
        val now = currentEpochMillis()
        val alert = planner.plan(item, daysBefore, alertHour, now, notifyIfLate) ?: return

        val task = timer.schedule(delay = (alert.triggerAtMillis - now).coerceAtLeast(0)) {
            pending.remove(item.id)
            showInstantNotification(alert.title, alert.message)
        }
        pending[item.id] = task
    }

    actual override fun cancelNotification(itemId: String) {
        pending.remove(itemId)?.cancel()
    }

    actual override fun showInstantNotification(title: String, message: String) {
        DesktopTray.showMessage(title, message)
    }

    actual override suspend fun requestPermission(): Boolean = DesktopTray.isSupported
}
