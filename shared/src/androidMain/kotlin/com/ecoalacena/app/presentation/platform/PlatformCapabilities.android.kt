package com.ecoalacena.app.presentation.platform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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

/** Android 13+ asks for `POST_NOTIFICATIONS` at runtime; older versions only have the app switch. */
@Composable
actual fun rememberNotificationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        currentOnResult(granted && NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    return {
        val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsRuntimePermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            currentOnResult(NotificationManagerCompat.from(context).areNotificationsEnabled())
        }
    }
}
