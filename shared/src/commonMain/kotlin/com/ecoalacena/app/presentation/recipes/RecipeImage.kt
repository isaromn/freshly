package com.ecoalacena.app.presentation.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.Recipe
import com.ecoalacena.app.presentation.util.icon

/**
 * Header picture of a recipe. Every recipe gets an illustration drawn from its category (colors
 * and icon from the theme, so it works offline and in dark mode). When the recipe has an
 * [Recipe.imageUrl], the photo is loaded on top of it; while it loads, or if it fails, the
 * illustration stays visible.
 */
@Composable
internal fun RecipeHeaderImage(
    recipe: Recipe,
    modifier: Modifier = Modifier,
) {
    val (container, accent) = recipe.category.headerColors()

    Box(
        modifier = modifier
            .clipToBounds()
            .background(Brush.linearGradient(listOf(container, accent.copy(alpha = 0.55f)))),
        contentAlignment = Alignment.Center,
    ) {
        val icon = recipe.category.icon
        val tint = MaterialTheme.colorScheme.contentColorFor(container)

        // Two big, faded copies of the icon in the corners, and a solid one in the middle.
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint.copy(alpha = 0.12f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-24).dp, y = (-24).dp)
                .size(140.dp)
                .rotate(-20f),
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint.copy(alpha = 0.12f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 28.dp, y = 28.dp)
                .size(160.dp)
                .rotate(15f),
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint.copy(alpha = 0.85f),
            modifier = Modifier.size(64.dp),
        )

        recipe.imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Background and accent of the illustration, taken from the theme so both modes look right. */
@Composable
private fun Category.headerColors(): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        Category.FRUITS_VEGETABLES -> colors.primaryContainer to colors.primary
        Category.DAIRY -> colors.secondaryContainer to colors.secondary
        Category.MEAT_FISH -> colors.errorContainer to colors.error
        Category.BAKERY -> colors.tertiaryContainer to colors.tertiary
        Category.BEVERAGES -> colors.secondaryContainer to colors.primary
        Category.PANTRY_STAPLE -> colors.tertiaryContainer to colors.secondary
        Category.OTHER -> colors.surfaceContainerHighest to colors.primary
    }
}
