package com.ecoalacena.app.data.remote.ai

/**
 * Configuration for [GeminiVisionService].
 *
 * Supplied by each platform when calling `initKoin`, so the API key is never
 * hard-coded in shared code. Do not commit real keys to the repository.
 *
 * @param model Gemini model code. `gemini-1.5-flash` was shut down by Google, so check
 * https://ai.google.dev/gemini-api/docs/models for the current stable Flash models.
 */
data class GeminiConfig(
    val apiKey: String,
    val model: String = DEFAULT_GEMINI_MODEL,
)

const val DEFAULT_GEMINI_MODEL = "gemini-3.5-flash-lite"
