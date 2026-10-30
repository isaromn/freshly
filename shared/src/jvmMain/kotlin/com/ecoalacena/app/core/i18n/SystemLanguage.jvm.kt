package com.ecoalacena.app.core.i18n

import java.util.Locale

actual fun systemLanguageCode(): String? = Locale.getDefault().language
