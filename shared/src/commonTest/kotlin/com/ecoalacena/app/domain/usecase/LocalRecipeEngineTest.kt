package com.ecoalacena.app.domain.usecase

import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.domain.model.RecipeSource
import com.ecoalacena.app.domain.recipe.LocalRecipeEngine
import com.ecoalacena.app.testing.testItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalRecipeEngineTest {

    // Fixed language: the tests must not depend on the language of the machine that runs them.
    private val engine = LocalRecipeEngine(language = { AppLanguage.ES })

    @Test
    fun returnsNothingWhenThereAreNoIngredients() {
        assertTrue(engine.suggest(emptyList()).isEmpty())
    }

    @Test
    fun suggestsVegetableSoupAndListsTheUsedItemsWithTheirAmounts() {
        val recipes = engine.suggest(
            listOf(
                testItem("1", "Zanahoria", quantity = 2.0),
                testItem("2", "Cebolla"),
            ),
        )

        val soup = recipes.first { it.title == "Sopa casera de verduras" }

        assertEquals(RecipeSource.OFFLINE, soup.source)
        assertTrue("Zanahoria (2 kg)" in soup.ingredients)
        assertTrue("Cebolla (1 kg)" in soup.ingredients)
        assertTrue(soup.steps.isNotEmpty())
    }

    @Test
    fun matchesPluralsAndAccentedNames() {
        val recipes = engine.suggest(
            ingredients = listOf(testItem("1", "Manzanas"), testItem("2", "Pimentón rojo")),
            limit = 10,
        )
        val titles = recipes.map { it.title }

        assertTrue("Ensalada de frutas" in titles)
        assertTrue("Sopa casera de verduras" in titles)
    }

    @Test
    fun doesNotMatchPartialWords() {
        // "Panela" must not be mistaken for "pan".
        val recipes = engine.suggest(listOf(testItem("1", "Panela")))

        assertEquals("local-generic", recipes.single().id)
    }

    @Test
    fun fallsBackToGenericRecipeWhenNothingMatches() {
        val recipes = engine.suggest(listOf(testItem("1", "Chocolatina")))

        assertEquals("local-generic", recipes.single().id)
        assertEquals(RecipeSource.OFFLINE, recipes.single().source)
    }

    @Test
    fun requiredIngredientGatesATemplate() {
        val withoutMilk = engine.suggest(listOf(testItem("1", "Manzana")), limit = 10)
        val withMilk = engine.suggest(
            ingredients = listOf(testItem("1", "Manzana"), testItem("2", "Leche")),
            limit = 10,
        )

        assertTrue(withoutMilk.none { it.title == "Batido cremoso" })
        assertTrue(withMilk.any { it.title == "Batido cremoso" })
    }

    @Test
    fun respectsTheLimit() {
        val recipes = engine.suggest(
            ingredients = listOf(
                testItem("1", "Zanahoria"),
                testItem("2", "Manzana"),
                testItem("3", "Huevo"),
            ),
            limit = 1,
        )

        assertEquals(1, recipes.size)
    }

    @Test
    fun writesTheRecipeInEnglishWhenAsked() {
        val recipes = engine.suggest(
            ingredients = listOf(testItem("1", "Carrots", quantity = 2.0), testItem("2", "Onion")),
            outputLanguage = AppLanguage.EN,
        )

        val soup = recipes.first { it.id == "local-vegetable-soup" }

        assertEquals("Homemade vegetable soup", soup.title)
        assertTrue("Carrots (2 kg)" in soup.ingredients)
        assertTrue("1 tablespoon of oil" in soup.ingredients)
    }

    @Test
    fun usesTheDeviceLanguageByDefault() {
        val english = LocalRecipeEngine(language = { AppLanguage.EN })

        val recipes = english.suggest(listOf(testItem("1", "Zanahoria")))

        assertEquals("Homemade vegetable soup", recipes.first { it.id == "local-vegetable-soup" }.title)
    }

    @Test
    fun matchesSpanishAndEnglishNamesWhateverTheOutputLanguage() {
        val items = listOf(testItem("1", "Blueberries"), testItem("2", "Milk"))

        val inSpanish = engine.suggest(items, limit = 10, outputLanguage = AppLanguage.ES)
        val inEnglish = engine.suggest(items, limit = 10, outputLanguage = AppLanguage.EN)

        assertTrue(inSpanish.any { it.title == "Batido cremoso" })
        assertTrue(inEnglish.any { it.title == "Creamy smoothie" })
    }

    @Test
    fun unitsAndTheGenericRecipeFollowTheOutputLanguage() {
        val items = listOf(testItem("1", "Chocolatina", quantity = 3.0, unit = "unit"))

        val spanish = engine.suggest(items, outputLanguage = AppLanguage.ES).single()
        val english = engine.suggest(items, outputLanguage = AppLanguage.EN).single()

        assertEquals("Salteado de aprovechamiento", spanish.title)
        assertTrue("Chocolatina (3 u.)" in spanish.ingredients)
        assertEquals("Use-it-up stir-fry", english.title)
        assertTrue("Chocolatina (3 pcs)" in english.ingredients)
    }

    @Test
    fun ingredientNamesAreShownInTheOutputLanguage() {
        val items = listOf(testItem("1", "huevos"))

        val english = engine.suggest(items, outputLanguage = AppLanguage.EN).first { it.id == "local-egg-scramble" }
        val spanish = engine.suggest(items, outputLanguage = AppLanguage.ES).first { it.id == "local-egg-scramble" }

        assertTrue("eggs (1 kg)" in english.ingredients)
        assertTrue("huevos (1 kg)" in spanish.ingredients)
    }

    @Test
    fun theRecipeIdsDoNotDependOnTheLanguage() {
        val items = listOf(testItem("1", "Zanahoria"))

        val spanishIds = engine.suggest(items, outputLanguage = AppLanguage.ES).map { it.id }
        val englishIds = engine.suggest(items, outputLanguage = AppLanguage.EN).map { it.id }

        assertEquals(spanishIds, englishIds)
    }
}
