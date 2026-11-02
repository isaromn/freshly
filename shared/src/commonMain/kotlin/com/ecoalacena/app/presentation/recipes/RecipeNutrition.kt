package com.ecoalacena.app.presentation.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ecoalacena.app.domain.model.Recipe
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import com.ecoalacena.app.presentation.util.formatDecimal

/**
 * Nutrition of one serving: calories, protein, carbs and fat as four tinted tiles, with the number
 * of servings and a note that it is an estimate. Nothing is drawn for recipes without nutrition.
 */
@Composable
internal fun NutritionCard(recipe: Recipe, modifier: Modifier = Modifier) {
    val calories = recipe.caloriesPerServing ?: return
    val strings = LocalAppStrings.current
    val colors = MaterialTheme.colorScheme

    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = strings.nutritionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                )
                Text(
                    text = strings.perServing(recipe.servings),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NutrientTile(
                    icon = Icons.Filled.LocalFireDepartment,
                    value = "$calories",
                    unit = "kcal",
                    label = strings.calories,
                    container = colors.tertiaryContainer,
                    content = colors.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                )
                NutrientTile(
                    icon = Icons.Filled.FitnessCenter,
                    value = recipe.proteinGrams.gramsText(),
                    unit = "g",
                    label = strings.protein,
                    container = colors.primaryContainer,
                    content = colors.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                NutrientTile(
                    icon = Icons.Filled.Grain,
                    value = recipe.carbsGrams.gramsText(),
                    unit = "g",
                    label = strings.carbs,
                    container = colors.secondaryContainer,
                    content = colors.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                NutrientTile(
                    icon = Icons.Filled.WaterDrop,
                    value = recipe.fatGrams.gramsText(),
                    unit = "g",
                    label = strings.fat,
                    container = colors.errorContainer,
                    content = colors.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = strings.nutritionDisclaimer,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** "12" for whole numbers, "12.5" otherwise, and "–" when the value is unknown. */
private fun Float?.gramsText(): String =
    when {
        this == null -> "–"
        this % 1f == 0f -> toInt().toString()
        else -> formatDecimal(toDouble(), 1)
    }

@Composable
private fun NutrientTile(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(text = unit, style = MaterialTheme.typography.labelSmall)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
