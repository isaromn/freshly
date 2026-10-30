package com.ecoalacena.app.domain.model

import com.ecoalacena.app.core.i18n.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

class FoodNamesTest {

    @Test
    fun translatesAKnownFoodAndKeepsThePluralAndTheCapitalization() {
        assertEquals("eggs", "huevos".localizedFoodName(AppLanguage.EN))
        assertEquals("Eggs", "Huevos".localizedFoodName(AppLanguage.EN))
        assertEquals("Egg", "Huevo".localizedFoodName(AppLanguage.EN))
        assertEquals("Leche", "Milk".localizedFoodName(AppLanguage.ES))
        assertEquals("tomatoes", "tomates".localizedFoodName(AppLanguage.EN))
        assertEquals("Strawberries", "Fresas".localizedFoodName(AppLanguage.EN))
        assertEquals("Limones", "Lemons".localizedFoodName(AppLanguage.ES))
        assertEquals("Peaches", "Duraznos".localizedFoodName(AppLanguage.EN))
        assertEquals("Bread", "Pan".localizedFoodName(AppLanguage.EN))
    }

    @Test
    fun ignoresAccentsWhenMatching() {
        assertEquals("Lemon", "Limón".localizedFoodName(AppLanguage.EN))
        assertEquals("lemon", "limon".localizedFoodName(AppLanguage.EN))
        assertEquals("Mushrooms", "Champiñones".localizedFoodName(AppLanguage.EN))
    }

    @Test
    fun keepsANameThatIsAlreadyInTheTargetLanguage() {
        assertEquals("huevos", "huevos".localizedFoodName(AppLanguage.ES))
        assertEquals("Eggs", "Eggs".localizedFoodName(AppLanguage.EN))
    }

    @Test
    fun keepsUnknownAndMultiWordNames() {
        assertEquals("Panela", "Panela".localizedFoodName(AppLanguage.EN))
        assertEquals("Leche entera", "Leche entera".localizedFoodName(AppLanguage.EN))
        assertEquals("Chocolatina", "Chocolatina".localizedFoodName(AppLanguage.EN))
    }
}
