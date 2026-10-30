package com.ecoalacena.app.data.remote.image

import com.ecoalacena.app.data.remote.ai.geminiJson
import com.ecoalacena.app.domain.recipe.RecipePhoto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class UnsplashPhotoFinderTest {

    @Test
    fun returnsTheFirstPhotoWithItsAuthor() = runTest {
        var request: HttpRequestData? = null
        val finder = finderFor(onRequest = { request = it })

        val photo = finder.findPhoto("carrot soup")

        assertEquals(RecipePhoto(url = "https://images.unsplash.com/soup", credit = "Ana Pérez"), photo)
        assertEquals("carrot soup", request?.url?.parameters?.get("query"))
        assertEquals("Client-ID test-key", request?.headers?.get("Authorization"))
    }

    @Test
    fun theSameSearchIsOnlySentOnce() = runTest {
        var requests = 0
        val finder = finderFor(onRequest = { requests++ })

        finder.findPhoto("carrot soup")
        val second = finder.findPhoto("  Carrot Soup ")

        assertEquals(1, requests)
        assertEquals("https://images.unsplash.com/soup", second?.url)
    }

    @Test
    fun withoutAKeyItDoesNotCallTheNetwork() = runTest {
        val finder = finderFor(accessKey = "", onRequest = { error("Unsplash must not be called without a key") })

        assertNull(finder.findPhoto("carrot soup"))
    }

    @Test
    fun anApiErrorMeansNoPhoto() = runTest {
        val finder = finderFor(status = HttpStatusCode.Unauthorized)

        assertNull(finder.findPhoto("carrot soup"))
    }

    @Test
    fun noResultsMeansNoPhoto() = runTest {
        val finder = finderFor(body = """{"results":[]}""")

        assertNull(finder.findPhoto("carrot soup"))
    }

    private fun finderFor(
        accessKey: String = "test-key",
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = SEARCH_RESPONSE,
        onRequest: (HttpRequestData) -> Unit = {},
    ): UnsplashPhotoFinder {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(geminiJson) }
        }
        return UnsplashPhotoFinder(client, UnsplashConfig(accessKey))
    }

    private companion object {
        const val SEARCH_RESPONSE =
            """{"total":1,"results":[{"id":"x","urls":{"regular":"https://images.unsplash.com/soup",""" +
                """"small":"s"},"user":{"name":"Ana Pérez","username":"ana"}}]}"""
    }
}
