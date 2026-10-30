package com.ecoalacena.app.domain.model

import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.core.i18n.currentLanguage

/**
 * Human-readable amount such as "2 kg" or "3 u." ("3 pcs" in English), used in recipes.
 *
 * @param language language of the unit text; the device language by default.
 */
internal fun PantryItem.amountText(language: AppLanguage = currentLanguage()): String {
    val amount = if (quantity % 1.0 == 0.0) quantity.toLong().toString() else quantity.toString()
    val unitText = when (unit.lowercase()) {
        "unit" -> when (language) {
            AppLanguage.ES -> "u."
            AppLanguage.EN -> "pcs"
        }

        "pack" -> when (language) {
            AppLanguage.ES -> "paq."
            AppLanguage.EN -> "pack"
        }

        else -> unit
    }
    return "$amount $unitText"
}
