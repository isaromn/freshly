package com.ecoalacena.app.presentation.shopping

import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.domain.model.ShoppingItem
import com.ecoalacena.app.presentation.util.formatQuantity

/**
 * The shopping list as plain text, ready to paste into WhatsApp or any messaging app:
 *
 * ```
 * 🛒 *Freshly - Shopping List:*
 * - Milk (2 L)
 * - Tomatoes (1 kg)
 * - Whole Wheat Bread
 * ```
 * The asterisks make the title bold in WhatsApp; other apps show them as they are.
 */
fun formatShoppingList(items: List<ShoppingItem>, strings: AppStrings): String =
    buildString {
        append(strings.shoppingShareTitle)
        items.forEach { item ->
            append("\n- ").append(item.name)
            val quantity = item.quantity
            val unit = item.unit
            if (quantity != null && quantity > 0.0 && unit != null) {
                append(" (").append(formatQuantity(quantity)).append(' ').append(strings.unitShort(unit)).append(')')
            }
        }
    }
