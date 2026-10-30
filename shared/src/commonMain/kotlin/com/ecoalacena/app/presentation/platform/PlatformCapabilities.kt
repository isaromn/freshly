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

/** Handles the system back gesture/button where the platform has one (Android). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
