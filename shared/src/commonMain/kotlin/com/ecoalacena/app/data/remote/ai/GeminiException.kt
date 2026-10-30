package com.ecoalacena.app.data.remote.ai

/**
 * Everything that can go wrong in a Gemini call. Services return these inside
 * `Result.failure`, so callers can react to each case (for example to show the user what to fix)
 * without reading error messages.
 */
sealed class GeminiException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** No API key configured: the request is never sent. */
    class MissingApiKey : GeminiException("Gemini API key is not configured.")

    /** The photo is over the inline request limit: the request is never sent. */
    class ImageTooLarge : GeminiException("Image is too large. Please use a smaller photo.")

    /** The request is not worth sending, for example a recipe with no ingredients. */
    class InvalidRequest(message: String) : GeminiException(message)

    /** Gemini could not be reached: no internet, DNS failure, timeout… */
    class Network(cause: Throwable) : GeminiException("Could not reach Gemini: ${cause.message}", cause)

    /** Gemini answered with an HTTP error (invalid key, unknown model, quota…). */
    class Api(val statusCode: Int, val details: String) :
        GeminiException("Gemini API error $statusCode: $details") {

        /** Google answers 400 "API key not valid" or 403 when the key is wrong or lacks access. */
        val isInvalidKey: Boolean
            get() = statusCode == 403 || "API key not valid" in details
    }

    /** Gemini refused to answer the request (safety filters). */
    class Blocked(reason: String) : GeminiException("The request was blocked by Gemini ($reason).")

    /** Gemini answered, but not with the JSON the app asked for (empty, broken or incomplete). */
    class InvalidResponse(message: String, cause: Throwable? = null) : GeminiException(message, cause)
}
