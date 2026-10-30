package com.ecoalacena.app.presentation.util

import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.data.remote.ai.GeminiException

/**
 * Turns an error from a Gemini call into a message the user can act on, in the language of
 * [strings]. Errors with nothing for the user to fix (broken answers, unknown failures) use
 * [fallback].
 */
fun Throwable?.toGeminiUserMessage(strings: AppStrings, fallback: String): String =
    when (this) {
        is GeminiException.MissingApiKey -> strings.geminiNoKey
        is GeminiException.ImageTooLarge -> strings.geminiImageTooLarge
        is GeminiException.Network -> strings.geminiNoConnection
        is GeminiException.Api -> when {
            isInvalidKey -> strings.geminiInvalidKey
            statusCode == 404 -> strings.geminiModelUnavailable
            statusCode == 429 -> strings.geminiRateLimit
            else -> fallback
        }
        else -> fallback
    }
