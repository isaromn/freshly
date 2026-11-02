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
                  "caloriesPerUnit": 64,
                  "proteinGrams": 3.3,
                  "carbsGrams": 4.8,
                  "fatGrams": 3.6,
                  "isOrganic": false
                },
                {
                  "name": "$apples",
                  "category": "FRUIT_VEG",
                  "quantity": 1.5,
                  "unit": "kg",
                  "estimatedDaysUntilExpiration": 12,
                  "confidence": 1.0,
                  "caloriesPerUnit": 52,
                  "proteinGrams": 0.3,
                  "carbsGrams": 14,
                  "fatGrams": 0.2,
                  "isOrganic": true
                },
                {
                  "name": "$chicken",
                  "category": "MEAT",
                  "quantity": 1.2,
                  "unit": "kg",
                  "estimatedDaysUntilExpiration": 2,
                  "confidence": 1.0,
                  "caloriesPerUnit": 120,
                  "proteinGrams": 22.5,
                  "carbsGrams": 0,
                  "fatGrams": 2.6,
                  "isOrganic": false
                },
                {
                  "name": "$bread",
                  "category": "BAKERY",
                  "quantity": 500,
                  "unit": "g",
                  "estimatedDaysUntilExpiration": 3,
                  "confidence": 1.0,
                  "caloriesPerUnit": 265,
                  "proteinGrams": 9,
                  "carbsGrams": 49,
                  "fatGrams": 3.2,
                  "isOrganic": false
                },
                {
                  "name": "$cheese",
                  "category": "DAIRY",
                  "quantity": 500,
                  "unit": "g",
                  "estimatedDaysUntilExpiration": 14,
                  "confidence": 1.0,
                  "caloriesPerUnit": 264,
                  "proteinGrams": 17,
                  "carbsGrams": 3,
                  "fatGrams": 21,
                  "isOrganic": false
                }
              ]
            }
        """.trimIndent()
    }
}
