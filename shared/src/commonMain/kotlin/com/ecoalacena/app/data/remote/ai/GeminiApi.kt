package com.ecoalacena.app.data.remote.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
private const val MAX_ERROR_DETAILS = 300

/**
 * Calls Gemini's `generateContent` endpoint and returns the text of the first answer.
 * Shared by every Gemini service so that error handling stays identical.
 *
 * @throws GeminiException.MissingApiKey if there is no key (nothing is sent).
 * @throws GeminiException.Network if Gemini cannot be reached.
 * @throws GeminiException.Api if Gemini answers with an HTTP error.
 * @throws GeminiException.Blocked if Gemini refuses the request.
 * @throws GeminiException.InvalidResponse if the answer is empty or not the expected envelope.
 */
internal suspend fun HttpClient.requestGeminiText(
    config: GeminiConfig,
    request: GeminiRequest,
): String {
    if (config.apiKey.isBlank()) throw GeminiException.MissingApiKey()

    val response = try {
        post("$GEMINI_BASE_URL/models/${config.model}:generateContent") {
            header("x-goog-api-key", config.apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        throw GeminiException.Network(error)
    }

    if (!response.status.isSuccess()) {
        val details = response.bodyAsText().take(MAX_ERROR_DETAILS)
        throw GeminiException.Api(statusCode = response.status.value, details = details)
    }

    val envelope = try {
        response.body<GeminiGenerateContentResponse>()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        throw GeminiException.InvalidResponse("Unexpected answer from Gemini: ${error.message}", error)
    }

    envelope.promptFeedback?.blockReason?.let { reason -> throw GeminiException.Blocked(reason) }

    return envelope.candidates
        .firstOrNull()
        ?.content
        ?.parts
        ?.firstNotNullOfOrNull { it.text }
        ?: throw GeminiException.InvalidResponse("Gemini returned an empty response.")
}

/**
 * Decodes the JSON the model wrote (the structured output) into [T].
 *
 * @throws GeminiException.InvalidResponse if it does not match the expected schema.
 */
internal inline fun <reified T> decodeGeminiJson(text: String): T =
    try {
        geminiJson.decodeFromString<T>(text.stripJsonCodeFences())
    } catch (error: Exception) {
        throw GeminiException.InvalidResponse("Could not parse Gemini response: ${error.message}", error)
    }

/** Some models still wrap JSON in ```json fences despite the instructions. */
internal fun String.stripJsonCodeFences(): String =
    trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
