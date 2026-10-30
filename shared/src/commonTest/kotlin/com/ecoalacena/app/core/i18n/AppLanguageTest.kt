package com.ecoalacena.app.core.i18n

import com.ecoalacena.app.core.formatDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {

    @Test
    fun spanishSystemLanguagesMapToSpanish() {
        assertEquals(AppLanguage.ES, AppLanguage.fromSystemCode("es"))
        assertEquals(AppLanguage.ES, AppLanguage.fromSystemCode("es-CO"))
        assertEquals(AppLanguage.ES, AppLanguage.fromSystemCode("es_MX"))
        assertEquals(AppLanguage.ES, AppLanguage.fromSystemCode(" ES "))
    }

    @Test
    fun everyOtherLanguageFallsBackToEnglish() {
        assertEquals(AppLanguage.EN, AppLanguage.fromSystemCode("en-US"))
        assertEquals(AppLanguage.EN, AppLanguage.fromSystemCode("fr"))
        assertEquals(AppLanguage.EN, AppLanguage.fromSystemCode("pt-BR"))
        assertEquals(AppLanguage.EN, AppLanguage.fromSystemCode(""))
        assertEquals(AppLanguage.EN, AppLanguage.fromSystemCode(null))
    }

    @Test
    fun stringsAreTranslatedInBothLanguages() {
        val es = stringsFor(AppLanguage.ES)
        val en = stringsFor(AppLanguage.EN)

        assertEquals("Alacena", es.tabPantry)
        assertEquals("Pantry", en.tabPantry)
        assertEquals("Escáner IA", es.tabScanner)
        assertEquals("AI Scanner", en.tabScanner)
        assertEquals("Recetas Inteligentes", es.tabRecipes)
        assertEquals("Smart Recipes", en.tabRecipes)
        assertEquals("Impacto Ecológico", es.tabImpact)
        assertEquals("Eco Impact", en.tabImpact)
        assertEquals("Modo Demo", es.demoMode)
        assertEquals("Demo Mode", en.demoMode)
        assertEquals("Nombre del producto", es.fieldName)
        assertEquals("Product name", en.fieldName)
        assertEquals("Guardar", es.save)
        assertEquals("Save", en.save)
        assertEquals("Cancelar", es.cancel)
        assertEquals("Cancel", en.cancel)
    }

    @Test
    fun datesFollowTheOrderOfEachLanguage() {
        // 2023-11-14T22:13:20Z
        val millis = 1_700_000_000_000L

        assertEquals("14/11/2023", formatDate(millis, AppLanguage.ES))
        assertEquals("11/14/2023", formatDate(millis, AppLanguage.EN))
    }
}
