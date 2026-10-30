package com.ecoalacena.app

import androidx.compose.ui.window.ComposeUIViewController
import com.ecoalacena.app.data.remote.ai.GeminiConfig
import com.ecoalacena.app.data.remote.image.UnsplashConfig
import com.ecoalacena.app.di.initKoin
import com.ecoalacena.app.presentation.App
import platform.Foundation.NSBundle

private var isKoinStarted = false

fun MainViewController() = ComposeUIViewController(
    configure = { startKoinOnce() },
) {
    App()
}

/** Starts Koin the first time a view controller is created, reading the keys from Info.plist. */
private fun startKoinOnce() {
    if (isKoinStarted) return
    isKoinStarted = true

    initKoin(
        geminiConfig = GeminiConfig(apiKey = infoPlistValue("GEMINI_API_KEY")),
        unsplashConfig = UnsplashConfig(accessKey = infoPlistValue("UNSPLASH_ACCESS_KEY")),
    )
}

private fun infoPlistValue(key: String): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String ?: ""
