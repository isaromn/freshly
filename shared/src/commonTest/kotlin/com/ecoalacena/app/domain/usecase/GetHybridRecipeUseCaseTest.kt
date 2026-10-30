package com.ecoalacena.app.domain.usecase

import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.core.i18n.currentLanguage
import com.ecoalacena.app.data.remote.ai.GeminiConfig
import com.ecoalacena.app.data.remote.ai.GeminiException
import com.ecoalacena.app.data.remote.ai.GeminiRecipeService
import com.ecoalacena.app.data.remote.ai.geminiJson
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.model.RecipeSource
import com.ecoalacena.app.testing.FakePantryRepository
import com.ecoalacena.app.testing.TEST_NOW
import com.ecoalacena.app.testing.testItem
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class GetHybridRecipeUseCaseTest {

    @Test
    fun returnsTheAiRecipeFirstFollowedByLocalOnes() = runTest {
        val useCase = useCaseFor(
            testItem("carrot", "Zanahoria", expiresInDays = 2),
            testItem("apple", "Manzanas", expiresInDays = 1),
        )

        val result = useCase().getOrThrow()

        val first = result.recipes.first()
        assertEquals(RecipeSource.IS_AI, first.source)
        assertEquals("Sopa de zanahoria", first.title)
        assertEquals(listOf("2 zanahorias"), first.ingredients)
        assertTrue(result.recipes.drop(1).isNotEmpty())
        assertTrue(result.recipes.drop(1).all { it.source == RecipeSource.OFFLINE })
        assertNull(result.aiFailure)
        // Most urgent ingredient first.
        assertEquals(listOf("Manzanas", "Zanahoria"), result.ingredients.map { it.name })
    }

    @Test
    fun sendsTheIngredientsWithAmountAndTimeLeftToGemini() = runTest {
        val prompts = mutableListOf<String>()
        val useCase = useCaseFor(
            testItem("carrot", "Zanahoria", expiresInDays = 2, quantity = 2.0),
            onRequest = { prompts += it },
        )

        useCase().getOrThrow()

        assertEquals(1, prompts.size)
        assertTrue("Zanahoria: 2 kg, expires in 2 days" in prompts.single())
    }

    @Test
    fun fallsBackToLocalRecipesWhenGeminiFails() = runTest {
        val useCase = useCaseFor(
            testItem("carrot", "Zanahoria", expiresInDays = 2),
            status = HttpStatusCode.InternalServerError,
        )

        val result = useCase().getOrThrow()

        assertIs<GeminiException>(result.aiFailure)
        assertTrue(result.recipes.isNotEmpty())
        assertTrue(result.recipes.all { it.source == RecipeSource.OFFLINE })
    }

    @Test
    fun reportsAMissingApiKeyButStillSuggestsLocalRecipes() = runTest {
        val useCase = useCaseFor(
            testItem("carrot", "Zanahoria", expiresInDays = 2),
            apiKey = "",
        )

        val result = useCase().getOrThrow()

        assertTrue("API key" in result.aiFailure?.message.orEmpty())
        assertTrue(result.recipes.all { it.source == RecipeSource.OFFLINE })
    }

    @Test
    fun failsWhenThePantryIsEmpty() = runTest {
        val useCase = useCaseFor()

        assertIs<EmptyPantryException>(useCase().exceptionOrNull())
    }

    @Test
    fun neverCooksWithExpiredItems() = runTest {
        val useCase = useCaseFor(
            testItem("old", "Leche vencida", expiresInDays = -1),
            testItem("fresh", "Zanahoria", expiresInDays = 5),
        )

        val result = useCase().getOrThrow()

        assertEquals(listOf("Zanahoria"), result.ingredients.map { it.name })
    }

    @Test
    fun failsWhenEverythingInThePantryIsExpired() = runTest {
        val useCase = useCaseFor(testItem("old", "Leche vencida", expiresInDays = -3))

        assertIs<EmptyPantryException>(useCase().exceptionOrNull())
    }

    @Test
    fun prefersItemsThatExpireWithinThreeDays() = runTest {
        val useCase = useCaseFor(
            testItem("soon", "Zanahoria", expiresInDays = 2),
            testItem("later", "Papa", expiresInDays = 10),
        )

        val result = useCase().getOrThrow()

        assertEquals(listOf("Zanahoria"), result.ingredients.map { it.name })
    }

    @Test
    fun usesAnyFreshItemWhenNothingExpiresSoon() = runTest {
        val useCase = useCaseFor(
            testItem("later", "Papa", expiresInDays = 20),
            testItem("soon", "Zanahoria", expiresInDays = 10),
            testItem("undated", "Arroz", expiresInDays = null),
        )

        val result = useCase().getOrThrow()

        assertEquals(listOf("Zanahoria", "Papa", "Arroz"), result.ingredients.map { it.name })
    }

    @Test
    fun localRecipesFollowTheLanguageOfTheDevice() = runTest {
        val carrots = testItem("carrot", "Carrots", expiresInDays = 2)
        val english = useCaseFor(carrots, status = HttpStatusCode.InternalServerError, language = { AppLanguage.EN })
        val spanish = useCaseFor(carrots, status = HttpStatusCode.InternalServerError, language = { AppLanguage.ES })

        assertEquals(listOf("Homemade vegetable soup"), english().getOrThrow().recipes.map { it.title })
        assertEquals(listOf("Sopa casera de verduras"), spanish().getOrThrow().recipes.map { it.title })
    }

    private fun useCaseFor(
        vararg items: PantryItem,
        status: HttpStatusCode = HttpStatusCode.OK,
        apiKey: String = "test-key",
        language: () -> AppLanguage = ::currentLanguage,
        onRequest: (String) -> Unit = {},
    ): GetHybridRecipeUseCase {
        val engine = MockEngine { request ->
            onRequest(request.bodyText())
            respond(
                content = geminiBody(AI_RECIPE_JSON),
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(geminiJson) }
        }

        return GetHybridRecipeUseCase(
            repository = FakePantryRepository(items.toList()),
            recipeService = GeminiRecipeService(client, GeminiConfig(apiKey = apiKey)),
            nowMillis = { TEST_NOW },
            language = language,
        )
    }

    private fun HttpRequestData.bodyText(): String =
        when (val content = body) {
            is TextContent -> content.text
            is ByteArrayContent -> content.bytes().decodeToString()
            else -> ""
        }

    /** Wraps the JSON the model "wrote" in the envelope returned by `generateContent`. */
    private fun geminiBody(modelText: String): String {
        val escaped = modelText.replace("\"", "\\\"").replace("\n", "\\n")
        return """{"candidates":[{"content":{"parts":[{"text":"$escaped"}]}}]}"""
    }

    private companion object {
        const val AI_RECIPE_JSON =
            """{"title":"Sopa de zanahoria","ingredients":["2 zanahorias"],""" +
                """"steps":["Cortar","Hervir","Servir"],"prepTimeMinutes":25,"difficulty":"EASY"}"""
    }
}
