package com.ecoalacena.app.presentation.pantry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.model.typicalCaloriesPerBasis
import com.ecoalacena.app.presentation.i18n.LocalAppStrings

/**
 * Small pill with the calories of a pantry item, e.g. "🔥 52 kcal / 100 g" or "🔥 95 kcal / pc".
 * Items saved without calories show the typical value of their category. Nothing is drawn when
 * not even that can be estimated (a unit without a known weight).
 */
@Composable
internal fun CaloriesBadge(item: PantryItem, modifier: Modifier = Modifier) {
    val calories = item.caloriesPerUnit ?: item.typicalCaloriesPerBasis() ?: return
    val strings = LocalAppStrings.current
    val text = strings.caloriesPer(calories, item.nutritionBasis)

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier.semantics { contentDescription = text },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}
