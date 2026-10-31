package com.ecoalacena.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ecoalacena.app.data.remote.ai.GeminiConfig
import com.ecoalacena.app.data.remote.image.UnsplashConfig
import com.ecoalacena.app.di.initKoin
import com.ecoalacena.app.presentation.App
import com.ecoalacena.app.presentation.appLogoPainter
import java.awt.Dimension
import java.io.File
import java.util.Properties

fun main() {
    initKoin(
        geminiConfig = GeminiConfig(apiKey = resolveSecret("GEMINI_API_KEY")),
        unsplashConfig = UnsplashConfig(accessKey = resolveSecret("UNSPLASH_ACCESS_KEY")),
    )

    application {
        // Opens maximized so the layout uses the whole monitor. The size is only the one the
        // window goes back to when the user restores it.
        val windowState = rememberWindowState(
            placement = WindowPlacement.Maximized,
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(width = 1280.dp, height = 800.dp),
        )

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Freshly",
            icon = appLogoPainter(),
        ) {
            // Stops the user from shrinking the window until the layout stops being usable.
            LaunchedEffect(window) {
                window.minimumSize = Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                App()
            }
        }
    }
}

private const val MIN_WINDOW_WIDTH = 900
private const val MIN_WINDOW_HEIGHT = 600

/**
 * Looks for a key ([name], e.g. `GEMINI_API_KEY`), in this order: the environment variable,
 * the system property, and `local.properties` in the working directory or up to two parent
 * directories. Returns an empty string if none is found.
 */
private fun resolveSecret(name: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: System.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: readFromLocalProperties(name)
        ?: ""

private fun readFromLocalProperties(name: String): String? {
    val workingDirectory = File(System.getProperty("user.dir"))

    return generateSequence(workingDirectory) { it.parentFile }
        .take(3)
        .map { directory -> File(directory, "local.properties") }
        .firstOrNull { it.isFile }
        ?.let { file ->
            Properties()
                .apply { file.inputStream().use { stream -> load(stream) } }
                .getProperty(name)
        }
        ?.takeIf { it.isNotBlank() }
}
