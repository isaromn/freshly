package com.ecoalacena.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ecoalacena.app.data.remote.ai.GeminiConfig
import com.ecoalacena.app.di.initKoin
import com.ecoalacena.app.presentation.App
import java.awt.Dimension
import java.io.File
import java.util.Properties

fun main() {
    initKoin(geminiConfig = GeminiConfig(apiKey = resolveGeminiApiKey()))

    application {
        // A regular computer window, centered on the screen. It is wide enough for the side
        // navigation and the multi-column layouts.
        val windowState = rememberWindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(width = 1280.dp, height = 800.dp),
        )

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "EcoAlacenaKMP",
        ) {
            // Stops the user from shrinking the window until the layout stops being usable.
            LaunchedEffect(window) {
                window.minimumSize = Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT)
            }

            App()
        }
    }
}

private const val MIN_WINDOW_WIDTH = 900
private const val MIN_WINDOW_HEIGHT = 600

/**
 * Looks for the Gemini API key, in this order: the `GEMINI_API_KEY` environment variable,
 * the `GEMINI_API_KEY` system property, and `local.properties` in the working directory
 * or up to two parent directories. Returns an empty string if none is found.
 */
private fun resolveGeminiApiKey(): String =
    System.getenv("GEMINI_API_KEY")?.takeIf { it.isNotBlank() }
        ?: System.getProperty("GEMINI_API_KEY")?.takeIf { it.isNotBlank() }
        ?: readKeyFromLocalProperties()
        ?: ""

private fun readKeyFromLocalProperties(): String? {
    val workingDirectory = File(System.getProperty("user.dir"))

    return generateSequence(workingDirectory) { it.parentFile }
        .take(3)
        .map { directory -> File(directory, "local.properties") }
        .firstOrNull { it.isFile }
        ?.let { file ->
            Properties()
                .apply { file.inputStream().use { stream -> load(stream) } }
                .getProperty("GEMINI_API_KEY")
        }
        ?.takeIf { it.isNotBlank() }
}
