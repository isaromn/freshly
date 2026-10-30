package com.ecoalacena.app.presentation.pantry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ecoalacena.app.presentation.i18n.LocalAppStrings

/**
 * Floating "+" button that unfolds two smaller actions above it: scan a photo and add an item by
 * hand. The "+" turns into an "×" while the actions are visible, and picking one folds them again.
 */
@Composable
fun AddFoodSpeedDial(
    onScanClick: () -> Unit,
    onAddManuallyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val iconRotation by animateFloatAsState(targetValue = if (expanded) 45f else 0f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically { height -> height / 2 } + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + slideOutVertically { height -> height / 2 } + scaleOut(targetScale = 0.8f),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SpeedDialAction(
                    label = strings.fabScan,
                    icon = Icons.Filled.CameraAlt,
                    onClick = {
                        expanded = false
                        onScanClick()
                    },
                )
                SpeedDialAction(
                    label = strings.fabAddItem,
                    icon = Icons.Filled.Edit,
                    onClick = {
                        expanded = false
                        onAddManuallyClick()
                    },
                )
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = if (expanded) strings.fabCloseMenu else strings.fabOpenMenu,
                modifier = Modifier.rotate(iconRotation),
            )
        }
    }
}

/** A mini floating button with its label to the left, lined up with the main button's center. */
@Composable
private fun SpeedDialAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        // Centers the 40 dp mini button over the 56 dp main one.
        modifier = Modifier.padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 2.dp,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            // The label next to the button already names the action.
            Icon(imageVector = icon, contentDescription = null)
        }
    }
}
