package com.ecoalacena.app.data.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ecoalacena.app.core.currentEpochMillis
import com.ecoalacena.app.domain.model.PantryItem
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Expiration alerts are one-off WorkManager jobs, one per item ("expiry-<id>"), so they still
 * arrive after the app is closed or the phone restarts.
 */
actual class NotificationService(
    private val context: Context,
    private val planner: ExpirationAlertPlanner,
) : ExpirationNotifier {

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    /** Each instant notification gets its own id, so several can be on screen at once. */
    private val instantIds = AtomicInteger(INSTANT_ID_START)

    actual override suspend fun scheduleExpirationAlert(
        item: PantryItem,
        daysBefore: Int,
        alertHour: Int,
        notifyIfLate: Boolean,
    ) {
        val now = currentEpochMillis()
        val alert = planner.plan(item, daysBefore, alertHour, now, notifyIfLate)
        if (alert == null) {
            workManager.cancelUniqueWork(workNameOf(item.id))
            return
        }

        val request = OneTimeWorkRequestBuilder<ExpirationAlertWorker>()
            .setInitialDelay((alert.triggerAtMillis - now).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ExpirationAlertWorker.KEY_ITEM_ID to item.id,
                    ExpirationAlertWorker.KEY_TITLE to alert.title,
                    ExpirationAlertWorker.KEY_MESSAGE to alert.message,
                ),
            )
            .build()
        workManager.enqueueUniqueWork(workNameOf(item.id), ExistingWorkPolicy.REPLACE, request)
    }

    actual override fun cancelNotification(itemId: String) {
        workManager.cancelUniqueWork(workNameOf(itemId))
        AndroidNotifications.cancel(context, notificationIdOf(itemId))
    }

    actual override fun showInstantNotification(title: String, message: String) {
        AndroidNotifications.post(context, instantIds.incrementAndGet(), title, message)
    }

    actual override suspend fun requestPermission(): Boolean = AndroidNotifications.canNotify(context)

    private fun workNameOf(itemId: String) = "expiry-$itemId"

    private companion object {
        const val INSTANT_ID_START = 1_000_000
    }
}
