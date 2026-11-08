package com.ecoalacena.app.presentation.pantry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecoalacena.app.domain.model.disposalMethod
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import com.ecoalacena.app.presentation.util.formatQuantity

/**
 * Shown while there is expired food in the pantry. Expired food must not be eaten, so the banner
 * lists it, says what to do with each kind (compost, organic waste bin, recycling the container)
 * and takes it all out of the pantry as wasted food.
 */
@Composable
internal fun ExpiredItemsBanner(
    items: List<PantryItemUi>,
    onRemoveAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val strings = LocalAppStrings.current
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.errorContainer, contentColor = colors.onErrorContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(22.dp))
                Text(
                    text = strings.expiredBannerTitle(items.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(text = strings.expiredBannerBody, style = MaterialTheme.typography.bodyMedium)

            // Which food expired, and since when.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEach { ui ->
                    val item = ui.item
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "• ${item.name} · ${formatQuantity(item.quantity)} ${strings.unitShort(item.unit)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = strings.expiredLabel(-(ui.daysLeft ?: 0)),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.onErrorContainer.copy(alpha = 0.2f))

            // What to do with them, grouped by kind of food so each tip appears once.
            Text(
                text = strings.whatToDoWithThem,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            items.groupBy { it.item.category.disposalMethod }.forEach { (method, group) ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = colors.surface.copy(alpha = 0.55f),
                    contentColor = colors.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = strings.disposalTitle(method),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = group.joinToString(", ") { it.item.name },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurfaceVariant,
                        )
                        Text(text = strings.disposalTip(method), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Button(
                onClick = onRemoveAll,
                colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = strings.registerAllAsWasted, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
