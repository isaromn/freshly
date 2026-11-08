package com.ecoalacena.app.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Shows one expiration alert when WorkManager runs it at the planned time. */
class ExpirationAlertWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return Result.failure()
        AndroidNotifications.post(
            context = applicationContext,
            notificationId = notificationIdOf(itemId),
            title = inputData.getString(KEY_TITLE).orEmpty(),
            message = inputData.getString(KEY_MESSAGE).orEmpty(),
        )
        return Result.success()
    }

    internal companion object {
        const val KEY_ITEM_ID = "itemId"
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
    }
}

/** The alert of an item always has the same id, so a new one replaces the old one. */
internal fun notificationIdOf(itemId: String): Int = itemId.hashCode()
