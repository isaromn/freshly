package com.ecoalacena.app.core.i18n

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

/** The user's first preferred language, e.g. `es-CO`; this is what the iOS settings show. */
actual fun systemLanguageCode(): String? = NSLocale.preferredLanguages.firstOrNull() as? String
