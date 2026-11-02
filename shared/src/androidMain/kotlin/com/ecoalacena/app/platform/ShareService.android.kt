package com.ecoalacena.app.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/** Opens the Android share sheet (`ACTION_SEND`, `text/plain`), so the user picks WhatsApp, Messages… */
actual class ShareService(private val context: Context) {

    actual fun shareText(text: String): ShareResult {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        // The context is the application, not an activity: the chooser needs its own task.
        val chooser = Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(chooser)
            ShareResult.SHARED
        } catch (error: ActivityNotFoundException) {
            ShareResult.FAILED
        }
    }
}
