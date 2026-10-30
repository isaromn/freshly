package com.ecoalacena.app.data.remote.image

import com.ecoalacena.app.domain.recipe.RecipePhoto
import com.ecoalacena.app.domain.recipe.RecipePhotoFinder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

private const val UNSPLASH_SEARCH_URL = "https://api.unsplash.com/search/photos"

/**
 * Access key of the Unsplash API, supplied by each platform like the Gemini key.
 * An empty key turns photo search off. Do not commit real keys to the repository.
 */
data class UnsplashConfig(val accessKey: String)

/**
 * Searches Unsplash for a landscape photo of a dish. Any failure (no key, no network, API error,
 * no result) returns `null`, so the recipe falls back to its local illustration.
 *
 * Found photos are kept in memory by search text: the free Unsplash plan allows 50 searches an
 * hour, and the same recipes come back every time the recipes tab reloads.
 */
class UnsplashPhotoFinder(
    private val httpClient: HttpClient,
    private val config: UnsplashConfig,
) : RecipePhotoFinder {

    private val cache = mutableMapOf<String, RecipePhoto>()
    private val cacheLock = Mutex()

    override suspend fun findPhoto(query: String): RecipePhoto? {
        val key = query.trim().lowercase()
        if (config.accessKey.isBlank() || key.isEmpty()) return null

        cacheLock.withLock { cache[key] }?.let { return it }
        val photo = search(key) ?: return null
        cacheLock.withLock { cache[key] = photo }
        return photo
    }

    private suspend fun search(query: String): RecipePhoto? =
        try {
            val response = httpClient.get(UNSPLASH_SEARCH_URL) {
                header("Authorization", "Client-ID ${config.accessKey}")
                header("Accept-Version", "v1")
                parameter("query", query)
                parameter("per_page", 1)
                parameter("orientation", "landscape")
                parameter("content_filter", "high")
            }
            if (!response.status.isSuccess()) {
                null
            } else {
                response.body<UnsplashSearchResponse>().results.firstOrNull()?.let { photo ->
                    RecipePhoto(url = photo.urls.regular, credit = photo.user.name)
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            null
        }
}

@Serializable
internal data class UnsplashSearchResponse(val results: List<UnsplashPhoto> = emptyList())

@Serializable
internal data class UnsplashPhoto(val urls: UnsplashUrls, val user: UnsplashUser)

@Serializable
internal data class UnsplashUrls(val regular: String)

@Serializable
internal data class UnsplashUser(val name: String)
