package com.ecoalacena.app.presentation.pantry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import com.ecoalacena.app.presentation.util.formatDecimal

/**
 * Confirms that [item] was eaten. The text says whether it will add to the impact figures
 * ([impactKg] is the weight that will be counted, or `null` when it will not count).
 */
@Composable
fun ConfirmConsumedDialog(
    item: PantryItem,
    impactKg: Double?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    ConfirmActionDialog(
        icon = Icons.Filled.Check,
        title = strings.confirmConsumedTitle(item.name),
        body = if (impactKg != null) {
            strings.confirmConsumedCounts(formatDecimal(impactKg, 2))
        } else {
            strings.confirmConsumedNoImpact
        },
        confirmLabel = strings.actionConsumed,
        accent = MaterialTheme.colorScheme.primary,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

/** Confirms that [item] was thrown away: it is removed and never counts towards the impact. */
@Composable
fun ConfirmDiscardedDialog(
    item: PantryItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    ConfirmActionDialog(
        icon = Icons.Filled.Delete,
        title = strings.confirmDiscardedTitle(item.name),
        body = strings.confirmDiscardedBody,
        confirmLabel = strings.actionDiscarded,
        accent = MaterialTheme.colorScheme.error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

@Composable
private fun ConfirmActionDialog(
    icon: ImageVector,
    title: String,
    body: String,
    confirmLabel: String,
    accent: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = { Icon(imageVector = icon, contentDescription = null) },
        iconContentColor = accent,
        title = { Text(text = title) },
        text = { Text(text = body) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = accent),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )
}
