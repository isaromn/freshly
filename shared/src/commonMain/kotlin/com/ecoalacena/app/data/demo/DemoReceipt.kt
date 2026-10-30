package com.ecoalacena.app.data.demo

import com.ecoalacena.app.core.i18n.AppLanguage

/**
 * A preloaded supermarket receipt for "demo mode": it has the same shape as Gemini's answer,
 * so it goes through the same mapping as a real scan but needs no network or API key.
 * Product names follow the language of the app.
 *
 * Shelf lives are chosen so the pantry shows every colour of the freshness traffic light
 * right after loading (chicken and bread expire soon, the rest are fresh).
 */
internal object DemoReceipt {

    fun json(language: AppLanguage): String {
        val names = when (language) {
            AppLanguage.ES -> listOf("Leche entera", "Manzanas", "Pechuga de pollo", "Pan tajado", "Queso campesino")
            AppLanguage.EN -> listOf("Whole milk", "Apples", "Chicken breast", "Sliced bread", "Farmer cheese")
        }
        val (milk, apples, chicken, bread, cheese) = names

        return """
            {
              "items": [
                {
                  "name": "$milk",
                  "category": "DAIRY",
                  "quantity": 2,
                  "unit": "l",
                  "estimatedDaysUntilExpiration": 6,
                  "confidence": 1.0,
                  "isOrganic": false
                },
                {
                  "name": "$apples",
                  "category": "FRUIT_VEG",
                  "quantity": 1.5,
                  "unit": "kg",
                  "estimatedDaysUntilExpiration": 12,
                  "confidence": 1.0,
                  "isOrganic": true
                },
                {
                  "name": "$chicken",
                  "category": "MEAT",
                  "quantity": 1.2,
                  "unit": "kg",
                  "estimatedDaysUntilExpiration": 2,
                  "confidence": 1.0,
                  "isOrganic": false
                },
                {
                  "name": "$bread",
                  "category": "BAKERY",
                  "quantity": 500,
                  "unit": "g",
                  "estimatedDaysUntilExpiration": 3,
                  "confidence": 1.0,
                  "isOrganic": false
                },
                {
                  "name": "$cheese",
                  "category": "DAIRY",
                  "quantity": 500,
                  "unit": "g",
                  "estimatedDaysUntilExpiration": 14,
                  "confidence": 1.0,
                  "isOrganic": false
                }
              ]
            }
        """.trimIndent()
    }
}
