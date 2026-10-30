package com.ecoalacena.app.core.i18n

import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.RecipeDifficulty

/**
 * Every user-facing text of the app. Implemented once per language so the compiler
 * guarantees that no text is missing in either language.
 */
interface AppStrings {
    val language: AppLanguage

    // ---- Navigation ----
    val tabPantry: String
    val tabScanner: String
    val tabRecipes: String
    val tabImpact: String

    /** Shorter labels for the bottom navigation bar on narrow screens. */
    val tabScannerShort: String
    val tabRecipesShort: String
    val tabImpactShort: String

    // ---- Common ----
    val back: String
    val retry: String
    val cancel: String
    val save: String
    val ok: String
    val organic: String
    val demoMode: String
    fun days(count: Int): String
    fun categoryLabel(category: Category): String

    /** Compact unit shown next to a quantity, e.g. "2 kg" or "3 u.". */
    fun unitShort(unit: String): String

    /** Full unit name for pickers, e.g. "units". */
    fun unitName(unit: String): String

    // ---- Pantry ----
    val pantryTitle: String
    val pantrySubtitle: String
    val avoidedWasteTitle: String
    val allGood: String
    fun expiringSummary(expiringSoon: Int, expired: Int): String
    fun filterAll(total: Int): String
    fun freshLabel(daysLeft: Int): String
    fun expiringSoonLabel(daysLeft: Int): String
    fun expiredLabel(daysAgo: Int): String
    val noDateLabel: String
    val actionConsumed: String
    val actionDiscarded: String
    val emptyPantryTitle: String
    val emptyPantryHint: String
    val scanNow: String
    val noItemsInCategory: String
    val fabScan: String
    val fabAddItem: String
    val fabOpenMenu: String
    val fabCloseMenu: String
    fun consumedExpiredMessage(name: String): String
    fun consumedSavedMessage(name: String, kg: String): String
    fun consumedMessage(name: String): String
    val consumeError: String
    fun discardedMessage(name: String): String
    val discardError: String
    fun savedMessage(name: String): String
    fun savedWithEstimateMessage(name: String, date: String): String
    val saveError: String

    // ---- Consumed / discarded confirmation ----
    fun confirmConsumedTitle(name: String): String
    fun confirmConsumedCounts(kg: String): String
    val confirmConsumedNoImpact: String
    fun confirmDiscardedTitle(name: String): String
    val confirmDiscardedBody: String

    // ---- Add / edit item dialog ----
    val addItemTitle: String
    val editItemTitle: String
    val fieldName: String
    val fieldCategory: String
    val fieldQuantity: String
    val fieldUnit: String
    val fieldExpiration: String
    val noExpirationDate: String
    val clearDate: String
    val fieldOrganic: String
    val nameRequired: String
    val quantityInvalid: String

    // ---- Scanner ----
    val scanPickTitle: String
    val scanPickDescription: String
    val takePhoto: String
    val pickFromGallery: String
    val pickImage: String
    val demoLoadButton: String
    val demoHint: String
    val demoUseButton: String
    val analyzing: String
    val analyzingHint: String
    val chooseAnother: String
    val imagePreview: String
    fun foundProducts(count: Int): String
    val reviewHint: String
    val oneDayLess: String
    val oneDayMore: String
    val editQuantity: String
    val lessQuantity: String
    val moreQuantity: String
    val changeExpirationDate: String
    val removeProduct: String
    val noExpirationLabel: String
    val expiresToday: String
    fun expiresIn(days: Int): String
    fun addToPantry(count: Int): String
    val saving: String
    val discardAndChoose: String
    val readImageError: String
    val openPickerError: String
    val openCameraError: String
    val noFoodFound: String
    val analyzeError: String
    val saveItemsError: String
    val demoLoadError: String

    // ---- Gemini errors ----
    val geminiNoKey: String
    val geminiInvalidKey: String
    val geminiModelUnavailable: String
    val geminiRateLimit: String
    val geminiImageTooLarge: String

    // ---- Recipes ----
    val recipesSubtitle: String
    fun usingIngredients(names: String): String
    val recipesLoading: String
    val recipesEmptyTitle: String
    val recipesEmptyHint: String
    val refreshRecipes: String
    fun minutes(count: Int): String
    fun difficulty(level: RecipeDifficulty): String
    val ingredientsTitle: String
    val stepsTitle: String
    fun stepsProgress(done: Int, total: Int): String
    val allStepsDone: String
    fun photoCredit(name: String): String
    val showRecipe: String
    val recipesLoadError: String
    val recipeAiFailed: String
    val recipeLocalSuffix: String

    // ---- Impact ----
    val impactTitle: String
    val impactSubtitle: String
    val impactEmptyTitle: String
    val impactEmptyHint: String
    val moneyTitle: String
    val foodTitle: String
    val co2Title: String
    val co2Unit: String
    val equivalences: String
    val treesTitle: String
    val treesUnit: String
    val kmTitle: String
    val kmUnit: String
    val tapForDetails: String
    val tapToHide: String
    fun explainMoney(pricePerKg: String, copPerUsd: String): String
    val explainFood: String
    fun explainCo2(kgPerKg: String): String
    fun explainTrees(kgPerYear: String): String
    fun explainKm(kgPerKm: String): String
    val impactDisclaimer: String

    // ---- Notifications ----
    val notificationChannelName: String
    val notificationChannelDescription: String
    val notificationTitle: String
    fun notificationBody(name: String, daysLeft: Int): String
}

fun stringsFor(language: AppLanguage): AppStrings =
    when (language) {
        AppLanguage.ES -> SpanishStrings
        AppLanguage.EN -> EnglishStrings
    }

/** The texts for the current system language (Spanish, or English for any other language). */
fun currentStrings(): AppStrings = stringsFor(currentLanguage())
