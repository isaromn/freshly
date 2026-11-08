package com.ecoalacena.app.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ecoalacena.app.core.i18n.currentStrings
import com.ecoalacena.app.shared.R

/** Posts notifications on the "Pantry Alerts" channel. Shared by the service and the alert worker. */
internal object AndroidNotifications {

    private const val CHANNEL_ID = "pantry_alerts"

    fun post(context: Context, notificationId: Int, title: String, message: String) {
        if (!canNotify(context)) return
        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_freshly)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (denied: SecurityException) {
            // The permission was revoked between the check and the call: nothing to show.
        }
    }

    fun cancel(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    /** Whether the user allows notifications (the runtime permission on Android 13+ and the app switch). */
    fun canNotify(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val strings = currentStrings()
        val channel = NotificationChannel(
            CHANNEL_ID,
            strings.notificationChannelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = strings.notificationChannelDescription }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Tapping the notification opens Freshly. */
    private fun openAppIntent(context: Context): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
