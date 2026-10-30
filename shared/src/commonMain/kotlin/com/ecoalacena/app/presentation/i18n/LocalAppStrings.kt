package com.ecoalacena.app.presentation.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.core.i18n.EnglishStrings

/**
 * The texts for the current language. `App()` provides the system language; English is the
 * default so that previews and tests that don't provide strings still show something.
 */
val LocalAppStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }
