package com.ecoalacena.app.presentation.recipes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ecoalacena.app.domain.model.RecipeMode
import com.ecoalacena.app.presentation.i18n.LocalAppStrings

/**
 * Two chips to choose how recipes pick their ingredients, with one line under them that says what
 * the selected mode does. Both chips share the width so they look like tabs.
 */
@Composable
internal fun RecipeModeSelector(
    selected: RecipeMode,
    enabled: Boolean,
    onSelect: (RecipeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecipeMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    enabled = enabled,
                    label = {
                        // Two lines at most, so long labels on narrow phones wrap instead of being cut.
                        Text(
                            text = mode.label(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        AnimatedContent(
            targetState = selected,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { mode ->
            Text(
                text = when (mode) {
                    RecipeMode.EXPIRING_SOON -> strings.modeExpiringSoonHint
                    RecipeMode.AVAILABLE_PANTRY -> strings.modeAvailablePantryHint
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecipeMode.label(): String {
    val strings = LocalAppStrings.current
    return when (this) {
        RecipeMode.EXPIRING_SOON -> strings.modeExpiringSoon
        RecipeMode.AVAILABLE_PANTRY -> strings.modeAvailablePantry
    }
}
