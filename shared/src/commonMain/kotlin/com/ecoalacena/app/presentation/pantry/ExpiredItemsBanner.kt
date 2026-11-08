package com.ecoalacena.app.presentation.pantry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ecoalacena.app.domain.model.disposalMethod
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import com.ecoalacena.app.presentation.util.formatQuantity

/**
 * Shown while there is expired food in the pantry. Expired food must not be eaten, so the banner
 * takes it out of the pantry as wasted food. It starts as one compact line; a tap unfolds which
 * food expired and what to do with each kind (compost, organic waste bin, recycling the container).
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
    var expanded by rememberSaveable { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = colors.errorContainer, contentColor = colors.onErrorContainer),
    ) {
        // The compact line: what happened, and the arrow that unfolds the details.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = strings.whatToDoWithThem) { expanded = !expanded }
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.expiredBannerTitle(items.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = strings.expiredBannerShort,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(
                onClick = onRemoveAll,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
            ) {
                Text(strings.removeShort, fontWeight = FontWeight.Bold)
            }
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(24.dp).rotate(arrowRotation),
            )
        }

        AnimatedVisibility(visible = expanded) {
            ExpiredDetails(items = items)
        }
    }
}

/** Which food expired, since when, and what to do with each kind. */
@Composable
private fun ExpiredDetails(items: List<PantryItemUi>) {
    val strings = LocalAppStrings.current
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = strings.expiredBannerBody, style = MaterialTheme.typography.bodyMedium)

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

        // Grouped by kind of food so each tip appears once.
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
    }
}
