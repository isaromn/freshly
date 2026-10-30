package com.ecoalacena.app.presentation.platform

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile

/** Desktop has no camera picker: the scan screen offers the file explorer instead. */
@Composable
actual fun rememberCameraLauncher(
    onResult: (PlatformFile?) -> Unit,
    onError: () -> Unit,
): (() -> Unit)? = null

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop has no system back button; navigation uses the on-screen back arrow.
}
