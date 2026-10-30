package com.ecoalacena.app.presentation.util

import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.data.remote.ai.GeminiException

/**
 * Turns an error from a Gemini call into a message the user can act on, in the language of
 * [strings]. Unknown errors (network failures, parsing problems…) use [fallback].
 */
fun Throwable?.toGeminiUserMessage(strings: AppStrings, fallback: String): String {
    val details = (this as? GeminiException)?.message.orEmpty()
    return when {
        "API key is not configured" in details -> strings.geminiNoKey
        "API key not valid" in details || "error 403" in details -> strings.geminiInvalidKey
        "error 404" in details -> strings.geminiModelUnavailable
        "error 429" in details -> strings.geminiRateLimit
        "too large" in details -> strings.geminiImageTooLarge
        else -> fallback
    }
}
