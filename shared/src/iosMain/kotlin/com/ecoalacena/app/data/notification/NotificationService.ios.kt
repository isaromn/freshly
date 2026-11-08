package com.ecoalacena.app.data.notification

import com.ecoalacena.app.core.currentEpochMillis
import com.ecoalacena.app.domain.model.PantryItem
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

/** Local notifications through `UNUserNotificationCenter`; expiration alerts use calendar triggers. */
actual class NotificationService(
    private val planner: ExpirationAlertPlanner,
) : ExpirationNotifier {

    private val center: UNUserNotificationCenter
        get() = UNUserNotificationCenter.currentNotificationCenter()

    // The center keeps its delegate weakly: this reference keeps it alive.
    private val foregroundPresenter = ForegroundPresenter()

    init {
        center.delegate = foregroundPresenter
    }

    actual override suspend fun scheduleExpirationAlert(
        item: PantryItem,
        daysBefore: Int,
        alertHour: Int,
        notifyIfLate: Boolean,
    ) {
        val alert = planner.plan(item, daysBefore, alertHour, currentEpochMillis(), notifyIfLate)
        if (alert == null) {
            center.removePendingNotificationRequestsWithIdentifiers(listOf(identifierOf(item.id)))
            return
        }

        val date = NSDate.dateWithTimeIntervalSince1970(alert.triggerAtMillis / MILLIS_PER_SECOND)
        val units = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
            NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond
        val components = NSCalendar.currentCalendar.components(units, fromDate = date)
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
        add(identifierOf(item.id), alert.title, alert.message, trigger)
    }

    actual override fun cancelNotification(itemId: String) {
        val identifiers = listOf(identifierOf(itemId))
        center.removePendingNotificationRequestsWithIdentifiers(identifiers)
        center.removeDeliveredNotificationsWithIdentifiers(identifiers)
    }

    actual override fun showInstantNotification(title: String, message: String) {
        // No trigger: the system delivers it right away.
        add(NSUUID().UUIDString, title, message, trigger = null)
    }

    actual override suspend fun requestPermission(): Boolean =
        suspendCancellableCoroutine { continuation ->
            center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
                if (continuation.isActive) continuation.resume(granted)
            }
        }

    private fun add(identifier: String, title: String, message: String, trigger: UNNotificationTrigger?) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
            setSound(UNNotificationSound.defaultSound)
        }
        val request = UNNotificationRequest.requestWithIdentifier(identifier, content, trigger)
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    private fun identifierOf(itemId: String) = "expiry-$itemId"

    private companion object {
        const val MILLIS_PER_SECOND = 1000.0
    }
}

/** iOS hides notifications while the app is open unless its delegate asks to show them. */
private class ForegroundPresenter : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or
                UNNotificationPresentationOptionSound,
        )
    }
}
