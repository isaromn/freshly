package com.ecoalacena.app.presentation.platform

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile

/**
 * Returns a function that opens the device camera and delivers the photo to [onResult]
 * (`null` if the user cancelled), or `null` on platforms without a camera picker (desktop).
 *
 * FileKit only provides the camera picker on Android and iOS, so each platform supplies its own
 * implementation. [onError] is called when the camera cannot be opened.
 */
@Composable
expect fun rememberCameraLauncher(
    onResult: (PlatformFile?) -> Unit,
    onError: () -> Unit,
): (() -> Unit)?

/**
 * Returns a function that asks for permission to show notifications and reports the answer to
 * [onResult]: the runtime dialog on Android 13+, the system prompt on iOS (only the first time),
 * and whether the system tray is available on desktop.
 */
@Composable
expect fun rememberNotificationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit

/** Handles the system back gesture/button where the platform has one (Android). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
