package com.ecoalacena.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Both schemes give every role the SAME meaning, so a screen can use a role without caring about the theme:
//   primary   = food that is fine / main actions     secondary = calm neutral accent (teal)
//   tertiary  = warning (expiring soon)              error     = problem (expired, discarded)
private val ecoLightColorScheme = lightColorScheme(
    primary = SAGE_GREEN,
    onPrimary = SNOW_WHITE,
    primaryContainer = SAGE_CONTAINER,
    onPrimaryContainer = SAGE_GREEN,
    secondary = SEA_TEAL,
    onSecondary = SNOW_WHITE,
    secondaryContainer = SEA_TEAL_CONTAINER,
    onSecondaryContainer = ON_SEA_TEAL_CONTAINER,
    tertiary = AMBER,
    onTertiary = CHARCOAL,
    tertiaryContainer = AMBER_CONTAINER,
    onTertiaryContainer = CHARCOAL,
    background = WARM_BEIGE,
    onBackground = CHARCOAL,
    surface = WARM_BEIGE,
    onSurface = CHARCOAL,
    surfaceVariant = BEIGE_MID,
    onSurfaceVariant = CHARCOAL.copy(alpha = 0.7f),
    surfaceDim = BEIGE_HIGH,
    surfaceBright = WARM_BEIGE,
    surfaceContainerLowest = SNOW_WHITE,
    surfaceContainerLow = BEIGE_LOW,
    surfaceContainer = BEIGE_MID,
    surfaceContainerHigh = BEIGE_HIGH,
    surfaceContainerHighest = BEIGE_HIGHEST,
    outline = CHARCOAL.copy(alpha = 0.4f),
    outlineVariant = CHARCOAL.copy(alpha = 0.15f),
    error = TERRACOTTA,
    onError = SNOW_WHITE,
    errorContainer = TERRACOTTA_CONTAINER,
    onErrorContainer = CHARCOAL,
)

private val ecoDarkColorScheme = darkColorScheme(
    primary = FOOD_MINT_ACCENT,
    onPrimary = ON_PRIMARY_DARK,
    primaryContainer = PRIMARY_CONTAINER_DARK,
    onPrimaryContainer = ON_PRIMARY_CONTAINER_DARK,

    secondary = CO2_TEAL_ACCENT,
    onSecondary = ON_SECONDARY_DARK,
    secondaryContainer = SECONDARY_CONTAINER_DARK,
    onSecondaryContainer = ON_SECONDARY_CONTAINER_DARK,

    tertiary = MONEY_GOLD_ACCENT,
    onTertiary = ON_TERTIARY_DARK,
    tertiaryContainer = TERTIARY_CONTAINER_DARK,
    onTertiaryContainer = ON_TERTIARY_CONTAINER_DARK,

    background = NIGHT_BACKGROUND,
    onBackground = LINEN_WHITE,

    surface = NIGHT_SURFACE,
    onSurface = LINEN_WHITE,
    surfaceVariant = NIGHT_SURFACE_VARIANT,
    onSurfaceVariant = MUTED_SLATE_TEXT,
    surfaceTint = FOOD_MINT_ACCENT,
    inverseSurface = LINEN_WHITE,
    inverseOnSurface = INVERSE_ON_SURFACE_DARK,

    // Without these, Material falls back to its purple-tinted defaults: cards, navigation bar
    // and dialogs would not match the green palette in dark mode.
    surfaceDim = NIGHT_BACKGROUND,
    surfaceBright = NIGHT_CONTAINER_HIGHEST,
    surfaceContainerLowest = NIGHT_CONTAINER_LOWEST,
    surfaceContainerLow = NIGHT_SURFACE,
    surfaceContainer = NIGHT_SURFACE_VARIANT,
    surfaceContainerHigh = NIGHT_SURFACE_HIGH,
    surfaceContainerHighest = NIGHT_CONTAINER_HIGHEST,

    error = TERRACOTTA_ALERT_ACCENT,
    onError = ON_ERROR_DARK,
    errorContainer = ERROR_CONTAINER_DARK,
    onErrorContainer = ON_ERROR_CONTAINER_DARK,

    outline = OUTLINE_DARK,
    outlineVariant = OUTLINE_VARIANT_DARK,
    scrim = SCRIM_DARK,
)

private val ecoShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun EcoAlacenaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ecoDarkColorScheme else ecoLightColorScheme,
        shapes = ecoShapes,
        content = content,
    )
}
