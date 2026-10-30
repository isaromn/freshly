package com.ecoalacena.app.presentation.recipes

import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.core.i18n.SpanishStrings
import com.ecoalacena.app.data.remote.ai.GeminiConfig
import com.ecoalacena.app.data.remote.ai.GeminiRecipeService
import com.ecoalacena.app.domain.model.RecipeSource
import com.ecoalacena.app.domain.usecase.GetHybridRecipeUseCase
import com.ecoalacena.app.testing.FakePantryRepository
import com.ecoalacena.app.testing.TEST_NOW
import com.ecoalacena.app.testing.testItem
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * These tests use a blank Gemini API key: the AI call then fails immediately, without any
 * network access, so the whole flow runs synchronously and only local recipes are produced.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshBuildsLocalRecipesAndExplainsWhyThereIsNoAiRecipe() {
        val viewModel = viewModelFor(FakePantryRepository(listOf(testItem("1", "Zanahoria", expiresInDays = 2))))

        viewModel.onIntent(RecipeIntent.Refresh)

        val state = viewModel.state.value
        assertEquals(RecipeStatus.CONTENT, state.status)
        assertTrue(state.recipes.isNotEmpty())
        assertTrue(state.recipes.all { it.source == RecipeSource.OFFLINE })
        assertEquals(listOf("Zanahoria"), state.usedIngredients)
        assertNull(state.selectedRecipe)
        assertTrue("clave de Gemini" in state.aiNotice.orEmpty())
        assertTrue("recetas locales" in state.aiNotice.orEmpty())
    }

    @Test
    fun refreshWithAnEmptyPantryShowsTheEmptyState() {
        val viewModel = viewModelFor(FakePantryRepository())

        viewModel.onIntent(RecipeIntent.Refresh)

        assertEquals(RecipeStatus.EMPTY_PANTRY, viewModel.state.value.status)
        assertTrue(viewModel.state.value.recipes.isEmpty())
    }

    @Test
    fun openingAndClosingARecipe() {
        val viewModel = viewModelFor(
            FakePantryRepository(
                listOf(
                    testItem("1", "Zanahoria", expiresInDays = 2),
                    testItem("2", "Manzana", expiresInDays = 2),
                ),
            ),
        )
        viewModel.onIntent(RecipeIntent.Refresh)
        val recipes = viewModel.state.value.recipes
        assertTrue(recipes.size >= 2)
        assertNull(viewModel.state.value.selectedRecipe)

        viewModel.onIntent(RecipeIntent.OpenRecipe(recipes[1].id))
        assertEquals(recipes[1], viewModel.state.value.selectedRecipe)

        viewModel.onIntent(RecipeIntent.CloseRecipe)
        assertNull(viewModel.state.value.selectedRecipe)
    }

    @Test
    fun stepsAreTickedOffAndUnticked() {
        val viewModel = viewModelFor(FakePantryRepository(listOf(testItem("1", "Zanahoria", expiresInDays = 2))))
        viewModel.onIntent(RecipeIntent.Refresh)
        val recipe = viewModel.state.value.recipes.first()
        assertTrue(recipe.steps.size >= 2)

        viewModel.onIntent(RecipeIntent.ToggleStep(recipe.id, 0))
        viewModel.onIntent(RecipeIntent.ToggleStep(recipe.id, 1))
        assertEquals(setOf(0, 1), viewModel.state.value.completedStepsOf(recipe.id))

        viewModel.onIntent(RecipeIntent.ToggleStep(recipe.id, 0))
        assertEquals(setOf(1), viewModel.state.value.completedStepsOf(recipe.id))
    }

    @Test
    fun stepOutsideTheRecipeIsIgnored() {
        val viewModel = viewModelFor(FakePantryRepository(listOf(testItem("1", "Zanahoria", expiresInDays = 2))))
        viewModel.onIntent(RecipeIntent.Refresh)
        val recipe = viewModel.state.value.recipes.first()

        viewModel.onIntent(RecipeIntent.ToggleStep(recipe.id, recipe.steps.size))

        assertTrue(viewModel.state.value.completedStepsOf(recipe.id).isEmpty())
    }

    @Test
    fun screenShownLoadsOnFirstVisitAndReloadsOnlyWhenThePantryChanged() = runTest {
        val repository = FakePantryRepository(listOf(testItem("1", "Zanahoria", expiresInDays = 2)))
        val viewModel = viewModelFor(repository)

        viewModel.onIntent(RecipeIntent.ScreenShown)
        assertEquals(RecipeStatus.CONTENT, viewModel.state.value.status)
        assertEquals(listOf("Zanahoria"), viewModel.state.value.usedIngredients)

        // Tick a step: showing the screen again with the same pantry must not reload (and lose it).
        val firstId = viewModel.state.value.recipes.first().id
        viewModel.onIntent(RecipeIntent.ToggleStep(firstId, 0))
        viewModel.onIntent(RecipeIntent.ScreenShown)
        assertEquals(setOf(0), viewModel.state.value.completedStepsOf(firstId))

        // A new item changes the ingredients, so the recipes are rebuilt.
        repository.upsert(testItem("2", "Cebolla", expiresInDays = 1))
        viewModel.onIntent(RecipeIntent.ScreenShown)
        assertEquals(listOf("Cebolla", "Zanahoria"), viewModel.state.value.usedIngredients)
    }

    @Test
    fun usedIngredientsAreShownInTheLanguageOfTheDevice() {
        val repository = FakePantryRepository(listOf(testItem("1", "huevos", expiresInDays = 2)))

        val english = viewModelFor(repository, language = { AppLanguage.EN })
        english.onIntent(RecipeIntent.Refresh)
        val spanish = viewModelFor(repository, language = { AppLanguage.ES })
        spanish.onIntent(RecipeIntent.Refresh)

        assertEquals(listOf("eggs"), english.state.value.usedIngredients)
        assertEquals(listOf("huevos"), spanish.state.value.usedIngredients)
    }

    private fun viewModelFor(
        repository: FakePantryRepository,
        language: () -> AppLanguage = { AppLanguage.ES },
    ): RecipeViewModel {
        // The engine must never be reached: a blank API key fails before any request is sent.
        val client = HttpClient(MockEngine { error("Gemini must not be called without an API key") })
        val useCase = GetHybridRecipeUseCase(
            repository = repository,
            recipeService = GeminiRecipeService(client, GeminiConfig(apiKey = "")),
            nowMillis = { TEST_NOW },
        )
        return RecipeViewModel(getRecipes = useCase, strings = { SpanishStrings }, language = language)
    }
}
