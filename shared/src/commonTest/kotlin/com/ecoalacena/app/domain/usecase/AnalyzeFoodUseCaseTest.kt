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
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

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
    fun failsWithGeminiExceptionOnHttpError() = runTest {
        val useCase = useCaseReturning(
            body = """{"error":{"message":"quota"}}""",
            status = HttpStatusCode.TooManyRequests,
        )

        val error = useCase(imageBytes).exceptionOrNull()

        assertIs<GeminiException>(error)
        assertTrue("429" in error.message.orEmpty())
    }

    @Test
    fun failsWhenApiKeyIsBlank() = runTest {
        val useCase = useCaseReturning(geminiBody("""{"items":[]}"""), apiKey = "")

        val error = useCase(imageBytes).exceptionOrNull()

        assertIs<GeminiException>(error)
        assertTrue("API key" in error.message.orEmpty())
    }

    private fun useCaseReturning(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        apiKey: String = "test-key",
        language: AppLanguage = AppLanguage.ES,
    ): AnalyzeFoodUseCase {
        val engine = MockEngine {
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
