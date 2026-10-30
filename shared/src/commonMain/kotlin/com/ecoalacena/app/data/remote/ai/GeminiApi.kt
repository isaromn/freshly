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

private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
private const val MAX_ERROR_DETAILS = 300

/**
 * Calls Gemini's `generateContent` endpoint and returns the text of the first answer.
 * Shared by every Gemini service so that error handling stays identical.
 *
 * @throws GeminiException if the key is missing, the API answers with an error,
 * the request is blocked, or the answer is empty.
 */
internal suspend fun HttpClient.requestGeminiText(
    config: GeminiConfig,
    request: GeminiRequest,
): String {
    if (config.apiKey.isBlank()) {
        throw GeminiException("Gemini API key is not configured.")
    }

    val response = post("$GEMINI_BASE_URL/models/${config.model}:generateContent") {
        header("x-goog-api-key", config.apiKey)
        contentType(ContentType.Application.Json)
        setBody(request)
    }

    if (!response.status.isSuccess()) {
        val details = response.bodyAsText().take(MAX_ERROR_DETAILS)
        throw GeminiException("Gemini API error ${response.status.value}: $details")
    }

    val envelope: GeminiGenerateContentResponse = response.body()
    envelope.promptFeedback?.blockReason?.let { reason ->
        throw GeminiException("The request was blocked by Gemini ($reason).")
    }

    return envelope.candidates
        .firstOrNull()
        ?.content
        ?.parts
        ?.firstNotNullOfOrNull { it.text }
        ?: throw GeminiException("Gemini returned an empty response.")
}

/** Some models still wrap JSON in ```json fences despite the instructions. */
internal fun String.stripJsonCodeFences(): String =
    trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
