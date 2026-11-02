package com.ecoalacena.app.platform

/** What happened to a text the user shared. */
enum class ShareResult {
    /** The system share sheet opened (Android, iOS). */
    SHARED,

    /** The text was copied to the clipboard (desktop): the UI should say so. */
    COPIED_TO_CLIPBOARD,

    FAILED,
}

/**
 * Shares plain text with the tools of each platform: the share sheet on Android and iOS (WhatsApp,
 * Messages, mail…), and the clipboard on desktop, where there is no share sheet.
 */
expect class ShareService {
    fun shareText(text: String): ShareResult
}
