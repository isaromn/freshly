package com.ecoalacena.app.core.i18n

/** Languages the app is translated into. English is the fallback for every other language. */
enum class AppLanguage(
    val code: String,
    /** Language name used in prompts, so that the AI answers in the same language as the UI. */
    val aiName: String,
) {
    ES(code = "es", aiName = "Spanish"),
    EN(code = "en", aiName = "English"),
    ;

    companion object {

        /**
         * Spanish if [code] is a Spanish language tag (`es`, `es-CO`, `es_MX`…);
         * English for any other language or when the code is unknown.
         */
        fun fromSystemCode(code: String?): AppLanguage {
            val normalized = code?.trim()?.lowercase().orEmpty()
            val isSpanish = normalized == "es" || normalized.startsWith("es-") || normalized.startsWith("es_")
            return if (isSpanish) ES else EN
        }
    }
}

/** The language the app should use right now, following the system language. */
fun currentLanguage(): AppLanguage = AppLanguage.fromSystemCode(systemLanguageCode())
