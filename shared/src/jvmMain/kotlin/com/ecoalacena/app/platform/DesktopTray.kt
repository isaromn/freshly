package com.ecoalacena.app.platform

import java.awt.Color
import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

/**
 * Freshly's icon in the macOS menu bar or the Windows/Linux system tray. While it is there the app
 * keeps running with its window closed, so expiration alerts still arrive; its menu reopens the
 * window or quits. Desktop notifications are shown as messages of this same icon.
 */
object DesktopTray {

    private var trayIcon: TrayIcon? = null

    /** `false` in headless environments and desktops without a tray: the window then quits the app. */
    val isSupported: Boolean
        get() = !GraphicsEnvironment.isHeadless() && SystemTray.isSupported()

    /**
     * Puts the icon in the tray with an "open" and a "quit" entry. Returns `false` when there is no
     * tray, in which case closing the window should quit the app as usual.
     */
    @Synchronized
    fun install(
        image: Image?,
        openLabel: String,
        quitLabel: String,
        onOpen: () -> Unit,
        onQuit: () -> Unit,
    ): Boolean {
        if (!isSupported) return false
        val icon = trayIcon ?: addIcon(image ?: fallbackImage()) ?: return false
        image?.let { icon.image = it }
        icon.popupMenu = PopupMenu().apply {
            add(MenuItem(openLabel).apply { addActionListener { onOpen() } })
            addSeparator()
            add(MenuItem(quitLabel).apply { addActionListener { onQuit() } })
        }
        // Double click (Windows/Linux) opens the window too.
        icon.actionListeners.forEach(icon::removeActionListener)
        icon.addActionListener { onOpen() }
        return true
    }

    /** Shows a notification from the tray icon, adding the icon first if needed. */
    @Synchronized
    fun showMessage(title: String, message: String) {
        if (!isSupported) return
        val icon = trayIcon ?: addIcon(fallbackImage()) ?: return
        icon.displayMessage(title, message, TrayIcon.MessageType.INFO)
    }

    /** Takes the icon out of the tray, so the app can exit cleanly. */
    @Synchronized
    fun remove() {
        trayIcon?.let { SystemTray.getSystemTray().remove(it) }
        trayIcon = null
    }

    private fun addIcon(image: Image): TrayIcon? =
        try {
            TrayIcon(image, "Freshly").apply {
                isImageAutoSize = true
                SystemTray.getSystemTray().add(this)
            }.also { trayIcon = it }
        } catch (error: Exception) {
            null
        }

    /** A small green circle with the app initial, used until the real logo is set. */
    private fun fallbackImage(): BufferedImage {
        val size = FALLBACK_ICON_SIZE
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

    private const val FALLBACK_ICON_SIZE = 32
}
