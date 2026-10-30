package com.ecoalacena.app.domain.usecase

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.data.remote.ai.GeminiConfig
import com.ecoalacena.app.data.remote.ai.GeminiException
import com.ecoalacena.app.data.remote.ai.GeminiVisionService
import com.ecoalacena.app.data.remote.ai.geminiJson
import com.ecoalacena.app.domain.model.Category
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AnalyzeFoodUseCaseTest {

    private val imageBytes = byteArrayOf(1, 2, 3)

    @Test
    fun mapsGeminiResultToPantryItemWithSuggestedExpiration() = runTest {
        val useCase = useCaseReturning(
            geminiBody(
                """{"items":[{"name":"Leche entera","category":"DAIRY","quantity":1.0,"unit":"l","estimatedDaysUntilExpiration":7,"isOrganic":true}]}""",
            ),
        )

        val items = useCase(imageBytes).getOrThrow()

        assertEquals(1, items.size)
        val item = items.single()
        assertEquals("Leche entera", item.name)
        assertEquals(Category.DAIRY, item.category)
        assertEquals(1.0, item.quantity)
        assertEquals("l", item.unit)
        assertTrue(item.isOrganic)
        assertEquals(NOW + 7 * MILLIS_PER_DAY, item.expirationDate)
        assertTrue(item.id.isNotBlank())
    }

    @Test
    fun normalizesIncompleteOrUnexpectedValues() = runTest {
        val useCase = useCaseReturning(
            geminiBody(
                """{"items":[{"name":"  Pollo ","category":"frozen","quantity":0,"unit":" KG ","isOrganic":false}]}""",
            ),
        )

        val item = useCase(imageBytes).getOrThrow().single()

        assertEquals("Pollo", item.name)
        assertEquals(1.0, item.quantity)
        assertEquals("kg", item.unit)
        // The unknown category "frozen" is replaced by the one the name points to...
        assertEquals(Category.MEAT_FISH, item.category)
        // ...and, with no estimate from the model, the expiration date comes from the shelf-life rules.
        assertEquals(NOW + 5 * MILLIS_PER_DAY, item.expirationDate)
    }

    @Test
    fun aProductThatIsNotRecognisedStaysOtherWithTheDefaultShelfLife() = runTest {
        val useCase = useCaseReturning(
            geminiBody("""{"items":[{"name":"Xyzzy","category":"frozen","quantity":1,"unit":"unit"}]}"""),
        )

        val item = useCase(imageBytes).getOrThrow().single()

        assertEquals(Category.OTHER, item.category)
        assertEquals(NOW + 30 * MILLIS_PER_DAY, item.expirationDate)
    }

    @Test
    fun mapsEnglishReceiptProductsToTheCategoriesOfTheApp() = runTest {
        val useCase = useCaseReturning(
            geminiBody(
                """{"items":[
                    {"name":"Milk","category":"unknown","quantity":1,"unit":"l"},
                    {"name":"Whole Wheat Bread","category":"unknown","quantity":1,"unit":"unit"},
                    {"name":"Eggs","category":"unknown","quantity":12,"unit":"unit"},
                    {"name":"Blueberries","category":"unknown","quantity":0.5,"unit":"kg"}
                ]}""",
            ),
        )

        val items = useCase(imageBytes).getOrThrow()

        assertEquals(
            listOf(Category.DAIRY, Category.BAKERY, Category.DAIRY, Category.FRUITS_VEGETABLES),
            items.map { it.category },
        )
        assertEquals(
            listOf(14L, 7L, 30L, 5L).map { days -> NOW + days * MILLIS_PER_DAY },
            items.map { it.expirationDate },
        )
    }

    @Test
    fun theCategoryFromTheModelWinsWhenItIsUsable() = runTest {
        val useCase = useCaseReturning(
            geminiBody(
                """{"items":[{"name":"Chocolate milk","category":"BEVERAGES","quantity":1,"unit":"l"}]}""",
            ),
        )

        assertEquals(Category.BEVERAGES, useCase(imageBytes).getOrThrow().single().category)
    }

    @Test
    fun demoReceiptUsesTheNamesOfTheAppLanguage() {
        val english = useCaseReturning(geminiBody("""{"items":[]}"""), language = AppLanguage.EN)
        val spanish = useCaseReturning(geminiBody("""{"items":[]}"""), language = AppLanguage.ES)

        assertEquals(
            listOf("Whole milk", "Apples", "Chicken breast", "Sliced bread", "Farmer cheese"),
            english.demoReceipt().getOrThrow().map { it.name },
        )
        assertEquals(
            listOf("Leche entera", "Manzanas", "Pechuga de pollo", "Pan tajado", "Queso campesino"),
            spanish.demoReceipt().getOrThrow().map { it.name },
        )
    }

    @Test
    fun dropsItemsWithBlankNames() = runTest {
        val useCase = useCaseReturning(
            geminiBody("""{"items":[{"name":"   ","category":"OTHER","quantity":1,"unit":"unit"}]}"""),
        )

        assertTrue(useCase(imageBytes).getOrThrow().isEmpty())
    }

    @Test
    fun parsesJsonWrappedInCodeFences() = runTest {
        val useCase = useCaseReturning(
            geminiBody(
                "```json\n" +
                    """{"items":[{"name":"Manzanas","category":"FRUITS_VEGETABLES","quantity":6,"unit":"unit","estimatedDaysUntilExpiration":30}]}""" +
                    "\n```",
            ),
        )

        val item = useCase(imageBytes).getOrThrow().single()

        assertEquals("Manzanas", item.name)
        assertEquals(Category.FRUITS_VEGETABLES, item.category)
    }

    @Test
    fun mapsTheShortCategoryCodesOfTheVisionPrompt() = runTest {
        val useCase = useCaseReturning(
            geminiBody(
                """{"items":[""" +
                    """{"name":"Manzana roja","category":"FRUIT_VEG","quantity":4,"unit":"unit","estimatedDaysUntilExpiration":7,"confidence":0.95},""" +
                    """{"name":"Pechuga de pollo","category":"MEAT","quantity":1,"unit":"kg","estimatedDaysUntilExpiration":3,"confidence":0.9},""" +
                    """{"name":"Jugo de naranja","category":"BEVERAGE","quantity":1,"unit":"l","estimatedDaysUntilExpiration":10,"confidence":0.9}]}""",
            ),
        )

        val items = useCase(imageBytes).getOrThrow()

        assertEquals(
            listOf(Category.FRUITS_VEGETABLES, Category.MEAT_FISH, Category.BEVERAGES),
            items.map { it.category },
        )
        assertEquals(4.0, items.first().quantity)
    }

    @Test
    fun leavesOutItemsTheModelIsNotSureAbout() = runTest {
        val useCase = useCaseReturning(
            geminiBody(
                """{"items":[""" +
                    """{"name":"Banano","category":"FRUIT_VEG","quantity":3,"unit":"unit","estimatedDaysUntilExpiration":2,"confidence":0.9},""" +
                    """{"name":"Algo borroso","category":"OTHER","quantity":1,"unit":"unit","estimatedDaysUntilExpiration":5,"confidence":0.1}]}""",
            ),
        )

        val items = useCase(imageBytes).getOrThrow()

        assertEquals(listOf("Banano"), items.map { it.name })
    }

    @Test
    fun failsWithAnApiErrorOnHttpError() = runTest {
        val useCase = useCaseReturning(
            body = """{"error":{"message":"quota"}}""",
            status = HttpStatusCode.TooManyRequests,
        )

        val error = useCase(imageBytes).exceptionOrNull()

        assertIs<GeminiException.Api>(error)
        assertEquals(429, error.statusCode)
    }

    @Test
    fun failsWithMissingApiKeyWithoutCallingGemini() = runTest {
        val useCase = useCaseReturning(
            geminiBody("""{"items":[]}"""),
            apiKey = "",
            onRequest = { error("Gemini must not be called without an API key") },
        )

        assertIs<GeminiException.MissingApiKey>(useCase(imageBytes).exceptionOrNull())
    }

    @Test
    fun failsWithANetworkErrorWhenGeminiCannotBeReached() = runTest {
        val useCase = useCaseReturning(
            geminiBody("""{"items":[]}"""),
            onRequest = { throw IllegalStateException("Unable to resolve host") },
        )

        assertIs<GeminiException.Network>(useCase(imageBytes).exceptionOrNull())
    }

    @Test
    fun failsWithAnInvalidResponseWhenTheJsonDoesNotMatchTheSchema() = runTest {
        val useCase = useCaseReturning(geminiBody("""Sorry, I can't see any food"""))

        assertIs<GeminiException.InvalidResponse>(useCase(imageBytes).exceptionOrNull())
    }

    @Test
    fun requestsStructuredOutputWithTheAllowedCodes() = runTest {
        var requestBody = ""
        val useCase = useCaseReturning(
            geminiBody("""{"items":[]}"""),
            onRequest = { requestBody = it },
        )

        useCase(imageBytes)

        val schema = geminiJson.parseToJsonElement(requestBody)
            .jsonObject.getValue("generationConfig")
            .jsonObject
        assertEquals("application/json", schema.getValue("responseMimeType").jsonPrimitive.content)
        val item = schema.getValue("responseSchema").jsonObject
            .getValue("properties").jsonObject
            .getValue("items").jsonObject
            .getValue("items").jsonObject
        val properties = item.getValue("properties").jsonObject
        assertEquals(
            listOf("FRUIT_VEG", "DAIRY", "MEAT", "BAKERY", "PANTRY_STAPLE", "BEVERAGE", "OTHER"),
            properties.getValue("category").jsonObject.getValue("enum").jsonArray.map { it.jsonPrimitive.content },
        )
        assertEquals(
            listOf("kg", "g", "l", "ml", "unit", "pack"),
            properties.getValue("unit").jsonObject.getValue("enum").jsonArray.map { it.jsonPrimitive.content },
        )
        assertTrue("confidence" in item.getValue("required").jsonArray.map { it.jsonPrimitive.content })
    }

    private fun useCaseReturning(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        apiKey: String = "test-key",
        language: AppLanguage = AppLanguage.ES,
        onRequest: (String) -> Unit = {},
    ): AnalyzeFoodUseCase {
        val engine = MockEngine { request ->
            onRequest((request.body as TextContent).text)
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(geminiJson) }
        }
        val service = GeminiVisionService(client, GeminiConfig(apiKey = apiKey))
        return AnalyzeFoodUseCase(service, nowMillis = { NOW }, language = { language })
    }

    /** Wraps the JSON the model "wrote" in the envelope returned by `generateContent`. */
    private fun geminiBody(modelText: String): String {
        val escaped = modelText.replace("\"", "\\\"").replace("\n", "\\n")
        return """{"candidates":[{"content":{"parts":[{"text":"$escaped"}]}}]}"""
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
