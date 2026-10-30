package com.ecoalacena.app.presentation.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher

@Composable
actual fun rememberCameraLauncher(
    onResult: (PlatformFile?) -> Unit,
    onError: () -> Unit,
): (() -> Unit)? {
    val launcher = rememberCameraPickerLauncher(
        onError = { onError() },
        onResult = { file -> onResult(file) },
    )
    return { launcher.launch() }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
