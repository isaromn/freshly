package com.ecoalacena.app.core.i18n

/** Language code from the device or system settings (e.g. `es`, `es-CO`, `en-US`); `null` if unknown. */
expect fun systemLanguageCode(): String?
