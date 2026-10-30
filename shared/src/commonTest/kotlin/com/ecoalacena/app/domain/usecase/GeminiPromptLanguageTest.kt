package com.ecoalacena.app.domain.usecase

import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.data.remote.ai.GeminiConfig
import com.ecoalacena.app.data.remote.ai.GeminiRecipeService
import com.ecoalacena.app.data.remote.ai.GeminiVisionService
import com.ecoalacena.app.data.remote.ai.geminiJson
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** The AI must answer in the language of the device, so the language has to reach the prompts. */
class GeminiPromptLanguageTest {

    @Test
    fun recipePromptAsksForEnglishWhenTheDeviceIsInEnglish() = runTest {
        val body = recipeRequestBody { AppLanguage.EN }

        assertTrue("Write the title, the ingredients and the steps in English." in body)
        assertTrue("(optional)" in body)
        assertTrue("2 apples" in body)
        assertFalse("Write the title, the ingredients and the steps in Spanish." in body)
    }

    @Test
    fun recipePromptAsksForSpanishWhenTheDeviceIsInSpanish() = runTest {
        val body = recipeRequestBody { AppLanguage.ES }

        assertTrue("Write the title, the ingredients and the steps in Spanish." in body)
        assertTrue("(opcional)" in body)
        assertTrue("2 manzanas" in body)
        assertFalse("Write the title, the ingredients and the steps in English." in body)
    }

    @Test
    fun recipeLanguageIsReadOnEveryRequest() = runTest {
        var language = AppLanguage.EN
        val bodies = mutableListOf<String>()
        val service = GeminiRecipeService(
            httpClient = clientCapturing(bodies, RECIPE_JSON),
            config = GeminiConfig(apiKey = "key"),
            language = { language },
        )

        service.generateRecipe(listOf("- Milk: 1 l, expires in 2 days"))
        language = AppLanguage.ES
        service.generateRecipe(listOf("- Milk: 1 l, expires in 2 days"))

        assertTrue("Write the title, the ingredients and the steps in English." in bodies[0])
        assertTrue("Write the title, the ingredients and the steps in Spanish." in bodies[1])
    }

    @Test
    fun scannerAsksForProductNamesInTheLanguageOfTheApp() = runTest {
        assertTrue("Write the product names in English." in scanRequestBody { AppLanguage.EN })
        assertTrue("Write the product names in Spanish." in scanRequestBody { AppLanguage.ES })
    }

    @Test
    fun scannerPromptCoversReceiptsInBothLanguages() = runTest {
        val body = scanRequestBody { AppLanguage.ES }

        assertTrue("Spanish or in English" in body)
        assertTrue("Whole Wheat Bread" in body)
        assertTrue("Blueberries" in body)
        assertTrue("Eggs" in body)
        assertTrue("DZ = dozen" in body)
    }

    private suspend fun recipeRequestBody(language: () -> AppLanguage): String {
        val bodies = mutableListOf<String>()
        GeminiRecipeService(clientCapturing(bodies, RECIPE_JSON), GeminiConfig(apiKey = "key"), language)
            .generateRecipe(listOf("- Milk: 1 l, expires in 2 days"))
        return bodies.single()
    }

    private suspend fun scanRequestBody(language: () -> AppLanguage): String {
        val bodies = mutableListOf<String>()
        GeminiVisionService(clientCapturing(bodies, """{"items":[]}"""), GeminiConfig(apiKey = "key"), language)
            .analyzeImage(byteArrayOf(1, 2, 3))
        return bodies.single()
    }

    /** A fake Gemini that records every request body and answers with [modelJson]. */
    private fun clientCapturing(bodies: MutableList<String>, modelJson: String): HttpClient {
        val engine = MockEngine { request ->
            bodies += request.bodyText()
            respond(
                content = geminiBody(modelJson),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) { json(geminiJson) }
        }
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
        const val RECIPE_JSON =
            """{"title":"Rice pudding","ingredients":["1 l milk"],""" +
                """"steps":["Boil","Stir","Serve"],"prepTimeMinutes":25,"difficulty":"EASY"}"""
    }
}
