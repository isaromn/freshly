package com.ecoalacena.app.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.ui.graphics.vector.ImageVector
import com.ecoalacena.app.domain.model.Category

// The text of a category is language dependent: use `AppStrings.categoryLabel(category)`.

val Category.icon: ImageVector
    get() = when (this) {
        Category.FRUITS_VEGETABLES -> Icons.Filled.Eco
        Category.DAIRY -> Icons.Filled.Icecream
        Category.MEAT_FISH -> Icons.Filled.SetMeal
        Category.BAKERY -> Icons.Filled.BakeryDining
        Category.PANTRY_STAPLE -> Icons.Filled.Inventory2
        Category.BEVERAGES -> Icons.Filled.LocalDrink
        Category.OTHER -> Icons.Filled.MoreHoriz
    }
