package com.ecoalacena.app.data.notification

import com.ecoalacena.app.core.currentEpochMillis
import com.ecoalacena.app.domain.model.PantryItem
import java.awt.Color
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.schedule

/**
 * Desktop notifications are system tray messages (Notification Center on macOS, toasts on
 * Windows). A desktop app cannot wake itself up, so alerts are timers that fire while Freshly is
 * open; `PantryAlerts.keepInSync` plans them again every time the app starts.
 */
actual class NotificationService(
    private val planner: ExpirationAlertPlanner,
) : ExpirationNotifier {

    private val timer = Timer("freshly-alerts", true)
    private val pending = ConcurrentHashMap<String, TimerTask>()
    private val trayIcon: TrayIcon? by lazy { createTrayIcon() }

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
        trayIcon?.displayMessage(title, message, TrayIcon.MessageType.INFO)
    }

    actual override suspend fun requestPermission(): Boolean = isTraySupported()

    private fun isTraySupported(): Boolean = !GraphicsEnvironment.isHeadless() && SystemTray.isSupported()

    private fun createTrayIcon(): TrayIcon? {
        if (!isTraySupported()) return null
        return try {
            TrayIcon(trayImage(), "Freshly").apply {
                isImageAutoSize = true
                SystemTray.getSystemTray().add(this)
            }
        } catch (error: Exception) {
            null
        }
    }

    /** A small green circle with the app initial, drawn in code so it needs no image file. */
    private fun trayImage(): BufferedImage {
        val size = TRAY_ICON_SIZE
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.color = Color(0x1B, 0x4D, 0x3E)
        graphics.fillOval(0, 0, size, size)
        graphics.color = Color.WHITE
        graphics.font = graphics.font.deriveFont(size * 0.6f)
        val metrics = graphics.fontMetrics
        graphics.drawString("F", (size - metrics.stringWidth("F")) / 2, (size + metrics.ascent - metrics.descent) / 2)
        graphics.dispose()
        return image
    }

    private companion object {
        const val TRAY_ICON_SIZE = 32
    }
}
