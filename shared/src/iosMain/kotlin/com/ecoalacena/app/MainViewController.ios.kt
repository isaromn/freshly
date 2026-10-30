package com.ecoalacena.app

import androidx.compose.ui.window.ComposeUIViewController
import com.ecoalacena.app.data.remote.ai.GeminiConfig
import com.ecoalacena.app.di.initKoin
import com.ecoalacena.app.presentation.App
import platform.Foundation.NSBundle

private var isKoinStarted = false

fun MainViewController() = ComposeUIViewController(
    configure = { startKoinOnce() },
) {
    App()
}

/** Starts Koin the first time a view controller is created, reading the key from Info.plist. */
private fun startKoinOnce() {
    if (isKoinStarted) return
    isKoinStarted = true

    val apiKey = NSBundle.mainBundle.objectForInfoDictionaryKey("GEMINI_API_KEY") as? String ?: ""
    initKoin(geminiConfig = GeminiConfig(apiKey = apiKey))
}
