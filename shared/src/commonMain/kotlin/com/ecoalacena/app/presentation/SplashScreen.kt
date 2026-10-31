package com.ecoalacena.app.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import ecoalacenakmp.shared.generated.resources.Res
import ecoalacenakmp.shared.generated.resources.logoNevera
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/** How long the splash stays on screen, animation included. */
private const val SPLASH_DURATION_MILLIS = 1_600L

private val LOGO_SIZE = 180.dp

/** The logo starts at about the size of the Android system splash icon, then grows to full size. */
private const val INITIAL_LOGO_SCALE = 0.55f

/**
 * Shown every time the app starts: the logo grows softly, then the name and the tagline appear.
 * [onFinished] is called once, when it is time to show the app.
 *
 * It picks up where the Android system splash leaves off: same background color, and the logo
 * starts at the size the system shows it, already visible, so the hand-over does not flicker.
 */
@Composable
internal fun SplashScreen(onFinished: () -> Unit) {
    val strings = LocalAppStrings.current
    val currentOnFinished by rememberUpdatedState(onFinished)

    val logoScale = remember { Animatable(INITIAL_LOGO_SCALE) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoScale.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing)) }
        launch { textAlpha.animateTo(1f, tween(durationMillis = 500, delayMillis = 350)) }
        delay(SPLASH_DURATION_MILLIS)
        currentOnFinished()
    }

    // The logo stays exactly in the center, where the system splash draws it, and the text sits
    // below it, so nothing moves when the app takes over.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.logoNevera),
            contentDescription = "Logo Freshly",
            modifier = Modifier
                .size(LOGO_SIZE)
                .graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                },
        )

        Column(
            modifier = Modifier
                // Centered like the logo, then moved down below it (half the logo plus a gap).
                .offset(y = LOGO_SIZE / 2 + 56.dp)
                .graphicsLayer { alpha = textAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Freshly",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = strings.pantrySubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The Freshly logo, for platform windows that need an icon (the desktop window and dock). */
@Composable
fun appLogoPainter(): Painter = painterResource(Res.drawable.logoNevera)
