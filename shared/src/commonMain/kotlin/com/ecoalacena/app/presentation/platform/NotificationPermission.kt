package com.ecoalacena.app.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.ecoalacena.app.data.notification.ExpirationNotifier
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Permission request for platforms that can ask without an activity (iOS, desktop): it calls
 * [ExpirationNotifier.requestPermission] in a coroutine.
 */
@Composable
internal fun rememberNotifierPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit {
    val notifier = koinInject<ExpirationNotifier>()
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
    return remember(notifier, scope) {
        { scope.launch { currentOnResult(notifier.requestPermission()) } }
    }
}
