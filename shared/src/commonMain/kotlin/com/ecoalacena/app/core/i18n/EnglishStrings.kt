package com.ecoalacena.app.core.i18n

import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.RecipeDifficulty

/** English texts: the default for every system language other than Spanish. */
object EnglishStrings : AppStrings {
    override val language = AppLanguage.EN

    // ---- Navigation ----
    override val tabPantry = "Pantry"
    override val tabScanner = "AI Scanner"
    override val tabRecipes = "Smart Recipes"
    override val tabImpact = "Eco Impact"
    override val tabScannerShort = "Scanner"
    override val tabRecipesShort = "Recipes"
    override val tabImpactShort = "Impact"

    // ---- Common ----
    override val back = "Back"
    override val retry = "Retry"
    override val cancel = "Cancel"
    override val save = "Save"
    override val ok = "OK"
    override val organic = "Organic"
    override val demoMode = "Demo Mode"

    override fun days(count: Int) = if (count == 1) "1 day" else "$count days"

    override fun categoryLabel(category: Category) =
        when (category) {
            Category.FRUITS_VEGETABLES -> "Fruits & vegetables"
            Category.DAIRY -> "Dairy"
            Category.MEAT_FISH -> "Meat & fish"
            Category.BAKERY -> "Bakery"
            Category.PANTRY_STAPLE -> "Pantry staples"
            Category.BEVERAGES -> "Beverages"
            Category.OTHER -> "Other"
        }

    override fun unitShort(unit: String) =
        when (unit.lowercase()) {
            "unit" -> "pcs"
            "pack" -> "pack"
            "l" -> "L"
            else -> unit
        }

    override fun unitName(unit: String) =
        when (unit.lowercase()) {
            "unit" -> "units"
            "pack" -> "packs"
            "l" -> "L"
            else -> unit
        }

    // ---- Pantry ----
    override val pantryTitle = "My pantry"
    override val pantrySubtitle = "Less waste, more flavor"
    override val avoidedWasteTitle = "Avoided waste"
    override val allGood = "Everything looks good in your pantry"

    override fun expiringSummary(expiringSoon: Int, expired: Int) =
        "$expiringSoon expiring soon · $expired expired"

    override fun filterAll(total: Int) = "All ($total)"

    override fun freshLabel(daysLeft: Int) = "Fresh · ${days(daysLeft)}"

    override fun expiringSoonLabel(daysLeft: Int) =
        if (daysLeft <= 0) "Expiring soon · today" else "Expiring soon · ${days(daysLeft)}"

    override fun expiredLabel(daysAgo: Int) =
        if (daysAgo <= 0) "Expired · today" else "Expired · ${days(daysAgo)} ago"

    override val noDateLabel = "No date"
    override val actionConsumed = "Consumed"
    override val actionDiscarded = "Discarded"
    override val emptyPantryTitle = "Your pantry is empty"
    override val emptyPantryHint = "Scan a supermarket receipt or add your products to get started."
    override val scanNow = "Scan now"
    override val noItemsInCategory = "There are no items in this category"
    override val fabScan = "Scan Photo"
    override val fabAddItem = "Add Manually"
    override val fabOpenMenu = "Add food"
    override val fabCloseMenu = "Close menu"

    override fun consumedExpiredMessage(name: String) = "“$name” marked as consumed."

    override fun consumedSavedMessage(name: String, kg: String) =
        "Nice! You avoided wasting $kg kg of “$name”."

    override fun consumedMessage(name: String) = "Nice! You made good use of “$name”."

    override val consumeError = "Couldn't mark the item as consumed."

    override fun discardedMessage(name: String) = "“$name” marked as discarded."

    override val discardError = "Couldn't mark the item as discarded."

    override fun savedMessage(name: String) = "“$name” saved."

    override fun savedWithEstimateMessage(name: String, date: String) =
        "“$name” saved. Estimated expiration: $date."

    override val saveError = "Couldn't save the item."

    // ---- Consumed / discarded confirmation ----
    override fun confirmConsumedTitle(name: String) = "Mark “$name” as consumed?"

    override fun confirmConsumedCounts(kg: String) =
        "You used it before it went bad: it adds about $kg kg to your impact (money and CO₂e saved)."

    override val confirmConsumedNoImpact = "It will be removed from the pantry, but it won't add to " +
        "your impact because it already expired or its weight can't be estimated."

    override fun confirmDiscardedTitle(name: String) = "Mark “$name” as discarded?"

    override val confirmDiscardedBody = "It will be removed from the pantry as waste (it spoiled, " +
        "expired or you threw it out). It won't add to your impact."

    // ---- Add / edit item dialog ----
    override val addItemTitle = "Add Item"
    override val editItemTitle = "Edit item"
    override val fieldName = "Product name"
    override val fieldCategory = "Category"
    override val fieldQuantity = "Quantity"
    override val fieldUnit = "Unit"
    override val fieldExpiration = "Expiration date"
    override val noExpirationDate = "Estimated automatically"
    override val clearDate = "Clear date"
    override val fieldOrganic = "Organic product"
    override val nameRequired = "Enter the product name"
    override val quantityInvalid = "Enter a quantity greater than 0"

    // ---- Scanner ----
    override val scanPickTitle = "Add food with a photo"
    override val scanPickDescription = "Take a photo of a supermarket receipt or your products. " +
        "The AI will recognize the food and estimate when it expires."
    override val takePhoto = "Take photo"
    override val pickFromGallery = "Choose from gallery"
    override val pickImage = "Choose image"
    override val demoLoadButton = "Load sample receipt (Demo Mode)"
    override val demoHint = "Works without internet or an API key."
    override val demoUseButton = "Use sample receipt (Demo Mode)"
    override val analyzing = "Analyzing with Gemini…"
    override val analyzingHint = "This may take a few seconds"
    override val chooseAnother = "Choose another photo"
    override val imagePreview = "Image preview"

    override fun foundProducts(count: Int) =
        when (count) {
            0 -> "No items left to add"
            1 -> "Found 1 item"
            else -> "Found $count items"
        }

    override val reviewHint = "Review the details: tap the quantity or the expiration date to change them."
    override val oneDayLess = "One day less"
    override val oneDayMore = "One day more"
    override val editQuantity = "Edit quantity"
    override val lessQuantity = "Decrease quantity"
    override val moreQuantity = "Increase quantity"
    override val changeExpirationDate = "Change expiration date"
    override val removeProduct = "Remove item"
    override val noExpirationLabel = "No expiration date"
    override val expiresToday = "Expires today"

    override fun expiresIn(days: Int) = "Expires in ${days(days)}"

    override fun addToPantry(count: Int) =
        if (count == 1) "Add 1 item to my pantry" else "Add $count items to my pantry"

    override val saving = "Saving…"
    override val discardAndChoose = "Discard and choose another photo"
    override val readImageError = "Couldn't read the selected image."
    override val openPickerError = "Couldn't open the image picker."
    override val openCameraError = "Couldn't open the camera."
    override val noFoodFound = "I couldn't find any food in the image. Try a clearer photo."
    override val analyzeError = "Couldn't analyze the image. Check your connection and try again."
    override val saveItemsError = "Couldn't save the items. Please try again."
    override val demoLoadError = "Couldn't load the sample receipt."

    // ---- Gemini errors ----
    override val geminiNoKey = "The Gemini API key is not configured (GEMINI_API_KEY)."
    override val geminiInvalidKey = "The Gemini API key is invalid or lacks permissions."
    override val geminiModelUnavailable =
        "The configured Gemini model is no longer available. Change the model in GeminiConfig."
    override val geminiRateLimit = "The Gemini usage limit was reached. Try again in a few minutes."
    override val geminiImageTooLarge = "The image is too large. Try a smaller photo."

    // ---- Recipes ----
    override val recipesSubtitle = "Cook what expires first"

    override fun usingIngredients(names: String) = "Using: $names"

    override val recipesLoading = "Cooking up ideas with your ingredients…"
    override val recipesEmptyTitle = "No ingredients yet"
    override val recipesEmptyHint =
        "Scan a receipt or your products and we'll suggest what to cook with what expires first."
    override val refreshRecipes = "Generate new recipes"

    override fun minutes(count: Int) = "$count min"

    override fun difficulty(level: RecipeDifficulty) =
        when (level) {
            RecipeDifficulty.EASY -> "Difficulty: Easy"
            RecipeDifficulty.MEDIUM -> "Difficulty: Medium"
            RecipeDifficulty.HARD -> "Difficulty: Hard"
        }

    override val ingredientsTitle = "Ingredients"
    override val stepsTitle = "Steps"
    override fun stepsProgress(done: Int, total: Int) = "$done of $total steps completed"
    override val allStepsDone = "All done! Enjoy your meal."
    override val showRecipe = "View recipe"
    override val recipesLoadError = "Couldn't load the recipes. Please try again."
    override val recipeAiFailed = "Couldn't generate an AI recipe."
    override val recipeLocalSuffix = "Showing local recipes instead."

    // ---- Impact ----
    override val impactTitle = "Your impact"
    override val impactSubtitle = "What you achieve by eating before it expires"
    override val impactEmptyTitle = "No impact recorded yet"
    override val impactEmptyHint = "Tap “Consumed” on your items in the pantry before they expire " +
        "and you'll see how much you and the planet save."
    override val moneyTitle = "Saved Money"
    override val foodTitle = "Food not wasted"
    override val co2Title = "CO₂e emissions avoided"
    override val co2Unit = "kg of CO₂e"
    override val equivalences = "Equivalences"
    override val treesTitle = "Equivalent trees saved"
    override val treesUnit = "trees working for 1 year"
    override val kmTitle = "Equivalent km not driven"
    override val kmUnit = "km by car"
    override val tapForDetails = "Tap to see how it's calculated"
    override val tapToHide = "Tap to hide"

    override fun explainMoney(pricePerKg: String, copPerUsd: String) =
        "Estimated with an average price of COP $pricePerKg per kg of food and " +
            "1 USD ≈ COP $copPerUsd (October 2026 exchange rate)."

    override val explainFood = "Total weight of the items you marked as consumed before they " +
        "expired. Items counted by unit or pack use an average weight for their category."

    override fun explainCo2(kgPerKg: String) =
        "Every kg of food that isn't wasted avoids roughly $kgPerKg kg of CO₂e " +
            "(production, transport and decomposition)."

    override fun explainTrees(kgPerYear: String) =
        "A mature tree absorbs about $kgPerYear kg of CO₂ per year. This is how many trees it would " +
            "take one year to absorb the CO₂e you avoided."

    override fun explainKm(kgPerKm: String) =
        "An average passenger car emits about $kgPerKm kg of CO₂e per kilometer (EPA reference)."

    override val impactDisclaimer = "SDG 12 · Responsible consumption and production. All figures are " +
        "estimates based on averages; the real impact depends on the type of food."

    // ---- Notifications ----
    override val notificationChannelName = "Expiration reminders"
    override val notificationChannelDescription = "Alerts when a food item in your pantry is about to expire"
    override val notificationTitle = "Expiring soon"

    override fun notificationBody(name: String, daysLeft: Int) =
        when (daysLeft) {
            0 -> "“$name” expires today"
            1 -> "“$name” expires tomorrow"
            else -> "“$name” expires in $daysLeft days"
        }
}
