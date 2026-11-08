package com.ecoalacena.app.presentation.scan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compost
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import com.ecoalacena.app.presentation.util.formatQuantity

/**
 * A product the scanner saw rotten or mouldy. It is not offered as pantry food: the card says what
 * is wrong, suggests a circular-economy use, and lets the user record it as wasted (compost) or
 * simply drop it from the list.
 */
@Composable
internal fun SpoiledItemCard(
    item: PantryItem,
    enabled: Boolean,
    onCompost: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = colors.errorContainer, contentColor = colors.onErrorContainer),
        border = BorderStroke(2.dp, colors.error),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = strings.spoiledTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${item.name} · ${formatQuantity(item.quantity)} ${strings.unitShort(item.unit)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            item.spoilageReason?.let { reason ->
                Text(text = reason, style = MaterialTheme.typography.bodyMedium)
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = strings.ecoAlternative(item.ecoDisposalTip ?: strings.spoiledDefaultTip),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCompost,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Compost, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(text = strings.registerAsCompost, modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    onClick = onDiscard,
                    enabled = enabled,
                    border = BorderStroke(1.dp, colors.onErrorContainer.copy(alpha = 0.6f)),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(text = strings.discard, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}
