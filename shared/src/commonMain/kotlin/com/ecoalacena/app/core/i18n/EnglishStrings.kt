package com.ecoalacena.app.core.i18n

import com.ecoalacena.app.domain.model.AlertLead
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.DisposalMethod
import com.ecoalacena.app.domain.model.NutritionBasis
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
    override val tabShopping = "Shopping List"
    override val tabShoppingShort = "Shopping"
    override val shoppingSubtitle = "What ran out or expired in the last 7 days"
    override val shoppingAddPlaceholder = "Add a product"
    override val shoppingAdd = "Add"
    override val shoppingShare = "Share list"
    override val shoppingEmptyTitle = "Your list is empty"
    override val shoppingEmptyHint =
        "When you use something up or it expires, it shows up here. You can also add products by hand."
    override val shoppingReasonConsumed = "You used it up"
    override val shoppingReasonExpired = "It expired"
    override val shoppingReasonManual = "Added by you"
    override val removeFromList = "Remove from the list"
    override val shoppingShareTitle = "🛒 *Freshly - Shopping List:*"
    override val copiedToClipboard = "Copied to clipboard!"
    override val shareError = "Couldn't share the list."
    override fun expiredBannerTitle(count: Int) =
        if (count == 1) "1 item expired" else "$count items expired"
    override val expiredBannerBody =
        "They are no longer safe to eat: they will be taken out of your pantry and counted as wasted food."
    override val expiredBannerShort = "Not safe to eat · tap to see what to do"
    override val removeShort = "Remove"
    override val whatToDoWithThem = "What to do with them"
    override fun disposalTitle(method: DisposalMethod) =
        when (method) {
            DisposalMethod.COMPOST -> "🌱 Compost or fertiliser"
            DisposalMethod.ORGANIC_WASTE -> "🗑️ Organic waste bin"
            DisposalMethod.RECYCLE_CONTAINER -> "♻️ Recycle the container"
        }
    override fun disposalTip(method: DisposalMethod) =
        when (method) {
            DisposalMethod.COMPOST ->
                "Chop them and add them to your compost, or bury them near your plants as natural fertiliser."
            DisposalMethod.ORGANIC_WASTE ->
                "Close them well in a bag and put them in the organic waste bin. Not in home compost: they attract pests."
            DisposalMethod.RECYCLE_CONTAINER ->
                "Pour the drink down the drain, rinse the container and put it in the recycling."
        }
    override fun wastedExpiredMessage(count: Int) =
        if (count == 1) "1 expired item taken out of your pantry." else "$count expired items taken out of your pantry."
    override val moneyWastedTitle = "Money lost to waste"
    override fun explainMoneyWasted(kg: String, pricePerKg: String, pricePerKgUsd: String) =
        "$kg kg of food thrown away (expired or spoiled), at an average price of COP $pricePerKg " +
            "(≈ US$ $pricePerKgUsd) per kg."
    override val spoiledTitle = "⚠️ Not fit to eat"
    override fun oldReceiptTitle(date: String, ageDays: Int) = "Old receipt: from $date ($ageDays days ago)"
    override fun oldReceiptBody(expiredCount: Int) =
        "We counted the expiration dates from the day of the purchase. " + when (expiredCount) {
            0 -> "Check that everything still looks good before adding it."
            1 -> "1 product has already expired: it won't be added to your pantry."
            else -> "$expiredCount products have already expired: they won't be added to your pantry."
        }
    override fun expiredByReceiptDate(date: String) = "Expired on $date, counting from the receipt date."
    override fun ecoAlternative(tip: String) = "🌿 Eco alternative: $tip"
    override val spoiledDefaultTip = "Put it in the compost or the organic waste bin."
    override val registerAsCompost = "Record as wasted (compost)"
    override val discard = "Discard"
    override fun compostedMessage(name: String) = "\"$name\" recorded as wasted food. Thanks for composting it!"
 
    override fun caloriesPer(calories: Int, basis: NutritionBasis) =
        "$calories kcal / " + when (basis) {
            NutritionBasis.PER_100_G -> "100 g"
            NutritionBasis.PER_100_ML -> "100 ml"
            NutritionBasis.PER_PIECE -> "pc"
            NutritionBasis.PER_PACK -> "pack"
        }
    override val nutritionTitle = "Nutrition facts"
    override fun perServing(servings: Int?) =
        if (servings == null) "Per serving" else "Per serving · serves $servings"
    override val calories = "Calories"
    override val protein = "Protein"
    override val carbs = "Carbs"
    override val fat = "Fat"
    override val nutritionDisclaimer = "Estimate for guidance, not medical advice."
    override val nutritionSectionTitle = "Nutrition & Health"
    override val caloriesSavedTitle = "Calories saved"
    override val mealsSavedTitle = "Servings saved"
    override val mealsUnit = "plates / servings"
    override val explainMeals = "Estimate of full plates of food used in time (1 kg of food ≈ 2.5 servings)."
    override val proteinSavedTitle = "Protein used"
    override val proteinUnit = "g of protein"
    override val explainProtein = "Grams of protein in food eaten before it expired."
    override val nutritionSummaryHint = "Energy of the food you ate before it expired"
    override fun caloriesDaysEquivalent(days: String) = "About $days days of energy for an adult (2,000 kcal a day)"
    override val nutritionSummaryEmpty =
        "Scan your products so we can estimate their calories. Items you type by hand have none."

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
        "expired or you threw it out) and will count as food and money wasted."

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

    override val reviewHint = "Review the details: tap the name, the quantity or the expiration date to change them."
    override val oneDayLess = "One day less"
    override val oneDayMore = "One day more"
    override val editName = "Edit name"
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
    override val geminiNoConnection = "No internet connection. Check your network and try again."

    // ---- Recipes ----
    override val recipesSubtitle = "Ideas from what is in your pantry"
    override val modeExpiringSoon = "⚠️ Use what expires"
    override val modeAvailablePantry = "🧺 Cook with what I have"
    override val modeExpiringSoonHint = "Zero-waste recipes with the items closest to expiring."
    override val modeAvailablePantryHint = "Recipes that combine as much as possible of what you have."

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
    override fun photoCredit(name: String) = "Photo by $name on Unsplash"
    override val finishRecipe = "Finish recipe"
    override val completionTitle = "Enjoy your meal! 🍽️"
    override val completionQuestion =
        "What did you use from your pantry? Untick what you didn't use or adjust the amount."
    override val completionNoPantryItems = "This recipe doesn't use anything that is still in your pantry."
    override fun inPantry(amount: String) = "In your pantry: $amount"
    override val discountFromPantry = "Take out of my pantry"
    override val notNow = "Not now"
    override val completionDoneTitle = "Pantry updated!"
    override val completionFoodSaved = "Food saved from waste"
    override val completionNothingCounted =
        "Expired food and items without an estimated weight don't add to your impact."
    override val completionDone = "Done"
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
    override val notificationChannelName = "Pantry Alerts"
    override val notificationChannelDescription =
        "Alerts when food in your pantry is about to expire or runs out"

    override fun expirationAlertTitle(name: String, daysLeft: Int) =
        when (daysLeft) {
            0 -> "⚠️ $name expires today!"
            1 -> "⚠️ $name expires tomorrow!"
            else -> "⚠️ $name expires in $daysLeft days!"
        }

    override val expirationAlertBody = "Check recipes to use it before it spoils."
    override fun usedUpAlertTitle(name: String) = "🧺 $name ran out"
    override val usedUpAlertBody =
        "It's now suggested on your shopping list. Share the list with your household so nobody buys it twice."
    override val testAlertTitle = "🥬 Freshly alerts are on"
    override val testAlertBody = "This is how you'll hear about food that is about to expire."

    // ---- Settings ----
    override val settingsTitle = "Settings"
    override val settingsSubtitle = "Notifications and alerts"
    override val openSettings = "Open settings"
    override val notificationsEnabled = "Notifications"
    override val notificationsEnabledHint = "Alerts about food that is about to expire"
    override val alertLeadTitle = "When to alert me"

    override fun alertLeadLabel(lead: AlertLead) =
        when (lead) {
            AlertLead.SAME_DAY -> "Same day"
            AlertLead.ONE_DAY -> "1 day before"
            AlertLead.TWO_DAYS -> "2 days before"
        }

    override val alertHourTitle = "Alert time"
    override val householdUpdates = "Household updates"
    override val householdUpdatesHint =
        "Alert when something runs out, so you can tell your household and avoid buying it twice"
    override val sendTestAlert = "Send a test alert"
    override val testAlertSent = "Test alert sent"
    override val notificationsDenied =
        "Notifications are blocked. Allow them for Freshly in the system settings."
    override val notificationsDeviceNote =
        "Alerts are stored on this device. On a computer, closing the window keeps Freshly in the menu bar " +
            "so alerts still arrive; quitting it stops them until you open it again. " +
            "Household updates do not sync between devices: share your list from the Shopping tab."

    // ---- Desktop menu bar / tray ----
    override val trayOpen = "Open Freshly"
    override val trayQuit = "Quit Freshly"
    override val trayStillRunningTitle = "Freshly is still running"
    override val trayStillRunningBody =
        "It stays in the menu bar so your expiration alerts can arrive. Quit it from there."
}
