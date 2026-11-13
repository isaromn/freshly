package com.ecoalacena.app.core.i18n

import com.ecoalacena.app.domain.model.AlertLead
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.DisposalMethod
import com.ecoalacena.app.domain.model.NutritionBasis
import com.ecoalacena.app.domain.model.RecipeDifficulty

object SpanishStrings : AppStrings {
    override val language = AppLanguage.ES

    // ---- Navigation ----
    override val tabPantry = "Alacena"
    override val tabScanner = "Escáner IA"
    override val tabRecipes = "Recetas Inteligentes"
    override val tabImpact = "Impacto Ecológico"
    override val tabScannerShort = "Escáner"
    override val tabRecipesShort = "Recetas"
    override val tabImpactShort = "Impacto"
    override val tabShopping = "Lista de compras"
    override val tabShoppingShort = "Compras"
    override val shoppingSubtitle = "Lo que se acabó o venció en los últimos 7 días"
    override val shoppingAddPlaceholder = "Añadir un producto"
    override val shoppingAdd = "Añadir"
    override val shoppingShare = "Compartir lista"
    override val shoppingEmptyTitle = "Tu lista está vacía"
    override val shoppingEmptyHint =
        "Cuando se te acabe o se venza algo, aparecerá aquí. También puedes añadir productos a mano."
    override val shoppingReasonConsumed = "Se te acabó"
    override val shoppingReasonExpired = "Se venció"
    override val shoppingReasonManual = "Añadido por ti"
    override val removeFromList = "Quitar de la lista"
    override val shoppingShareTitle = "🛒 *Freshly - Lista de compras:*"
    override val copiedToClipboard = "¡Lista copiada al portapapeles!"
    override val shareError = "No se pudo compartir la lista."
    override fun expiredBannerTitle(count: Int) =
        if (count == 1) "1 alimento vencido" else "$count alimentos vencidos"
    override val expiredBannerBody =
        "Ya no son aptos para consumo: se retirarán de tu alacena y contarán como desperdicio."
    override val expiredBannerShort = "No aptos para consumo · toca para ver qué hacer"
    override val removeShort = "Retirar"
    override val whatToDoWithThem = "¿Qué hacer con ellos?"
    override fun disposalTitle(method: DisposalMethod) =
        when (method) {
            DisposalMethod.COMPOST -> "🌱 Compost o abono"
            DisposalMethod.ORGANIC_WASTE -> "🗑️ Caneca de residuos orgánicos"
            DisposalMethod.RECYCLE_CONTAINER -> "♻️ Recicla el envase"
        }
    override fun disposalTip(method: DisposalMethod) =
        when (method) {
            DisposalMethod.COMPOST ->
                "Pícalos y agrégalos a tu compost, o entiérralos cerca de tus plantas como abono natural."
            DisposalMethod.ORGANIC_WASTE ->
                "Ciérralos bien en una bolsa y llévalos a la caneca de orgánicos. No al compost casero: atraen plagas."
            DisposalMethod.RECYCLE_CONTAINER ->
                "Vacía la bebida en el desagüe, enjuaga el envase y llévalo al reciclaje."
        }
    override fun wastedExpiredMessage(count: Int) =
        if (count == 1) "1 alimento vencido retirado de tu alacena." else "$count alimentos vencidos retirados de tu alacena."
    override val moneyWastedTitle = "Dinero perdido por desperdicio"
    override fun explainMoneyWasted(kg: String, pricePerKg: String) =
        "$kg kg de comida botada (vencida o dañada), a un precio medio de COP $pricePerKg por kg."
    override val spoiledTitle = "⚠️ Alimento no apto para consumo"
    override fun oldReceiptTitle(date: String, ageDays: Int) = "Recibo antiguo: es del $date (hace $ageDays días)"
    override fun oldReceiptBody(expiredCount: Int) =
        "Calculamos las fechas de caducidad desde el día de la compra. " + when (expiredCount) {
            0 -> "Revisa que todo siga en buen estado antes de agregarlo."
            1 -> "1 producto ya venció: no se agregará a tu alacena."
            else -> "$expiredCount productos ya vencieron: no se agregarán a tu alacena."
        }
    override fun expiredByReceiptDate(date: String) = "Venció el $date, según la fecha del recibo."
    override fun ecoAlternative(tip: String) = "🌿 Alternativa ecológica: $tip"
    override val spoiledDefaultTip = "Llévalo al compost o a la caneca de residuos orgánicos."
    override val registerAsCompost = "Registrar como desperdiciado (compost)"
    override val discard = "Descartar"
    override fun compostedMessage(name: String) = "\"$name\" registrado como desperdicio. ¡Gracias por compostarlo!"
    override fun caloriesPer(calories: Int, basis: NutritionBasis) =
        "$calories kcal / " + when (basis) {
            NutritionBasis.PER_100_G -> "100 g"
            NutritionBasis.PER_100_ML -> "100 ml"
            NutritionBasis.PER_PIECE -> "u."
            NutritionBasis.PER_PACK -> "paq."
        }
    override val nutritionTitle = "Información nutricional"
    override fun perServing(servings: Int?) =
        if (servings == null) "Por porción" else "Por porción · rinde $servings"
    override val calories = "Calorías"
    override val protein = "Proteínas"
    override val carbs = "Carbohidratos"
    override val fat = "Grasas"
    override val nutritionDisclaimer = "Estimación como guía, no es un consejo médico."
    override val nutritionSectionTitle = "Nutrición y salud"
    override val caloriesSavedTitle = "Calorías rescatadas"
    override val mealsSavedTitle = "Porciones salvadas"
    override val mealsUnit = "platos / porciones"
    override val explainMeals = "Estimado de platos de comida completos aprovechados a tiempo (1 kg de comida ≈ 2,5 porciones)."
    override val proteinSavedTitle = "Proteína aprovechada"
    override val proteinUnit = "g de proteína"
    override val explainProtein = "Gramos de proteína en alimentos consumidos antes de su caducidad."
    override val nutritionSummaryHint = "Energía de la comida que consumiste antes de que se venciera"
    override fun caloriesDaysEquivalent(days: String) = "Unos $days días de energía para un adulto (2.000 kcal al día)"
    override val nutritionSummaryEmpty =
        "Escanea tus productos para estimar sus calorías. Los que añades a mano no las tienen."

    // ---- Common ----
    override val back = "Volver"
    override val retry = "Reintentar"
    override val cancel = "Cancelar"
    override val save = "Guardar"
    override val ok = "Aceptar"
    override val organic = "Orgánico"
    override val demoMode = "Modo Demo"

    override fun days(count: Int) = if (count == 1) "1 día" else "$count días"

    override fun categoryLabel(category: Category) =
        when (category) {
            Category.FRUITS_VEGETABLES -> "Frutas y verduras"
            Category.DAIRY -> "Lácteos"
            Category.MEAT_FISH -> "Carnes y pescados"
            Category.BAKERY -> "Panadería"
            Category.PANTRY_STAPLE -> "Básicos de alacena"
            Category.BEVERAGES -> "Bebidas"
            Category.OTHER -> "Otro"
        }

    override fun unitShort(unit: String) =
        when (unit.lowercase()) {
            "unit" -> "u."
            "pack" -> "paq."
            "l" -> "L"
            else -> unit
        }

    override fun unitName(unit: String) =
        when (unit.lowercase()) {
            "unit" -> "unidades"
            "pack" -> "paquetes"
            "l" -> "L"
            else -> unit
        }

    // ---- Pantry ----
    override val pantryTitle = "Mi alacena"
    override val pantrySubtitle = "Menos desperdicio, más sabor"
    override val avoidedWasteTitle = "Desperdicio evitado"
    override val allGood = "Todo en orden en tu alacena"

    override fun expiringSummary(expiringSoon: Int, expired: Int) =
        "$expiringSoon por vencer · $expired vencidos"

    override fun filterAll(total: Int) = "Todos ($total)"

    override fun freshLabel(daysLeft: Int) = "Fresco · ${days(daysLeft)}"

    override fun expiringSoonLabel(daysLeft: Int) =
        if (daysLeft <= 0) "Próximo a vencer · hoy" else "Próximo a vencer · ${days(daysLeft)}"

    override fun expiredLabel(daysAgo: Int) =
        if (daysAgo <= 0) "Vencido · hoy" else "Vencido · hace ${days(daysAgo)}"

    override val noDateLabel = "Sin fecha"
    override val actionConsumed = "Consumido"
    override val actionDiscarded = "Desechado"
    override val emptyPantryTitle = "Tu alacena está vacía"
    override val emptyPantryHint = "Escanea un recibo del supermercado o agrega tus productos para empezar."
    override val scanNow = "Escanear ahora"
    override val noItemsInCategory = "No hay productos en esta categoría"
    override val fabScan = "Escanear foto"
    override val fabAddItem = "Agregar manualmente"
    override val fabOpenMenu = "Agregar alimento"
    override val fabCloseMenu = "Cerrar menú"

    override fun consumedExpiredMessage(name: String) = "«$name» marcado como consumido."

    override fun consumedSavedMessage(name: String, kg: String) =
        "¡Bien! Evitaste desperdiciar $kg kg de «$name»."

    override fun consumedMessage(name: String) = "¡Bien! Aprovechaste «$name»."

    override val consumeError = "No se pudo marcar como consumido."

    override fun discardedMessage(name: String) = "«$name» marcado como desechado."

    override val discardError = "No se pudo marcar como desechado."

    override fun savedMessage(name: String) = "«$name» guardado."

    override fun savedWithEstimateMessage(name: String, date: String) =
        "«$name» guardado. Vencimiento estimado: $date."

    override val saveError = "No se pudo guardar el producto."

    // ---- Consumed / discarded confirmation ----
    override fun confirmConsumedTitle(name: String) = "¿Marcar «$name» como consumido?"

    override fun confirmConsumedCounts(kg: String) =
        "Lo aprovechaste antes de que se dañara: suma unos $kg kg a tu impacto (dinero y CO₂e ahorrados)."

    override val confirmConsumedNoImpact = "Se quitará de la alacena, pero no suma a tu impacto " +
        "porque ya venció o no se puede estimar su peso."

    override fun confirmDiscardedTitle(name: String) = "¿Marcar «$name» como desechado?"

    override val confirmDiscardedBody = "Se quitará de la alacena como desperdicio (se dañó, " +
        "venció o lo botaste) y contará como comida y dinero desperdiciados."

    // ---- Add / edit item dialog ----
    override val addItemTitle = "Agregar alimento"
    override val editItemTitle = "Editar alimento"
    override val fieldName = "Nombre del producto"
    override val fieldCategory = "Categoría"
    override val fieldQuantity = "Cantidad"
    override val fieldUnit = "Unidad"
    override val fieldExpiration = "Fecha de vencimiento"
    override val noExpirationDate = "Se estimará automáticamente"
    override val clearDate = "Quitar fecha"
    override val fieldOrganic = "Producto orgánico"
    override val nameRequired = "Escribe el nombre del producto"
    override val quantityInvalid = "Ingresa una cantidad mayor que 0"

    // ---- Scanner ----
    override val scanPickTitle = "Añade alimentos con una foto"
    override val scanPickDescription = "Fotografía un recibo del supermercado o tus productos. " +
        "La IA reconocerá los alimentos y estimará cuándo caducan."
    override val takePhoto = "Tomar foto"
    override val pickFromGallery = "Elegir de la galería"
    override val pickImage = "Elegir imagen"
    override val demoLoadButton = "Cargar recibo de prueba (Modo Demo)"
    override val demoHint = "Funciona sin internet ni clave de API."
    override val demoUseButton = "Usar recibo de prueba (Modo Demo)"
    override val analyzing = "Analizando con Gemini…"
    override val analyzingHint = "Esto puede tardar unos segundos"
    override val chooseAnother = "Elegir otra foto"
    override val imagePreview = "Vista previa de la imagen"

    override fun foundProducts(count: Int) =
        when (count) {
            0 -> "No quedan productos por añadir"
            1 -> "Encontré 1 producto"
            else -> "Encontré $count productos"
        }

    override val reviewHint = "Revisa los datos: toca el nombre, la cantidad o la fecha de caducidad para cambiarlos."
    override val oneDayLess = "Un día menos"
    override val oneDayMore = "Un día más"
    override val editName = "Editar nombre"
    override val editQuantity = "Editar cantidad"
    override val lessQuantity = "Disminuir cantidad"
    override val moreQuantity = "Aumentar cantidad"
    override val changeExpirationDate = "Cambiar fecha de caducidad"
    override val removeProduct = "Quitar producto"
    override val noExpirationLabel = "Sin fecha de caducidad"
    override val expiresToday = "Caduca hoy"

    override fun expiresIn(days: Int) = "Caduca en ${days(days)}"

    override fun addToPantry(count: Int) =
        if (count == 1) "Añadir 1 producto a mi alacena" else "Añadir $count productos a mi alacena"

    override val saving = "Guardando…"
    override val discardAndChoose = "Descartar y elegir otra foto"
    override val readImageError = "No se pudo leer la imagen seleccionada."
    override val openPickerError = "No se pudo abrir el selector de imágenes."
    override val openCameraError = "No se pudo abrir la cámara."
    override val noFoodFound = "No encontré alimentos en la imagen. Prueba con una foto más clara."
    override val analyzeError = "No se pudo analizar la imagen. Revisa tu conexión e inténtalo de nuevo."
    override val saveItemsError = "No se pudieron guardar los productos. Inténtalo de nuevo."
    override val demoLoadError = "No se pudo cargar el recibo de prueba."

    // ---- Gemini errors ----
    override val geminiNoKey = "Falta configurar la clave de Gemini (GEMINI_API_KEY)."
    override val geminiInvalidKey = "La clave de Gemini no es válida o no tiene permisos."
    override val geminiModelUnavailable =
        "El modelo de Gemini configurado ya no está disponible. Cambia el modelo en GeminiConfig."
    override val geminiRateLimit = "Se alcanzó el límite de uso de Gemini. Inténtalo de nuevo en unos minutos."
    override val geminiImageTooLarge = "La imagen es demasiado grande. Prueba con una foto más pequeña."
    override val geminiNoConnection = "No hay conexión a internet. Revisa tu red e inténtalo de nuevo."

    // ---- Recipes ----
    override val recipesSubtitle = "Ideas con lo que hay en tu alacena"
    override val modeExpiringSoon = "⚠️ Aprovechar por vencer"
    override val modeAvailablePantry = "🧺 Cocinar con lo que hay"
    override val modeExpiringSoonHint = "Recetas de desperdicio cero con lo que está por vencer."
    override val modeAvailablePantryHint = "Recetas que combinan lo máximo posible de lo que tienes."

    override fun usingIngredients(names: String) = "Aprovechando: $names"

    override val recipesLoading = "Cocinando ideas con tus ingredientes…"
    override val recipesEmptyTitle = "Aún no hay ingredientes"
    override val recipesEmptyHint =
        "Escanea un recibo o tus productos y te sugeriremos qué cocinar con lo que vence primero."
    override val refreshRecipes = "Generar nuevas recetas"

    override fun minutes(count: Int) = "$count min"

    override fun difficulty(level: RecipeDifficulty) =
        when (level) {
            RecipeDifficulty.EASY -> "Dificultad: Fácil"
            RecipeDifficulty.MEDIUM -> "Dificultad: Media"
            RecipeDifficulty.HARD -> "Dificultad: Difícil"
        }

    override val ingredientsTitle = "Ingredientes"
    override val stepsTitle = "Preparación"
    override fun stepsProgress(done: Int, total: Int) = "$done de $total pasos completados"
    override val allStepsDone = "¡Listo! Buen provecho."
    override fun photoCredit(name: String) = "Foto de $name en Unsplash"
    override val finishRecipe = "Finalizar receta"
    override val completionTitle = "¡Buen provecho! 🍽️"
    override val completionQuestion =
        "¿Qué usaste de tu alacena? Desmarca lo que no gastaste o ajusta la cantidad."
    override val completionNoPantryItems = "Esta receta no usa productos que sigan en tu alacena."
    override fun inPantry(amount: String) = "En tu alacena: $amount"
    override val discountFromPantry = "Descontar de la alacena"
    override val notNow = "Ahora no"
    override val completionDoneTitle = "¡Alacena actualizada!"
    override val completionFoodSaved = "Comida salvada del desperdicio"
    override val completionNothingCounted =
        "Los productos vencidos o sin peso estimado no suman a tu impacto."
    override val completionDone = "Listo"
    override val showRecipe = "Ver receta"
    override val recipesLoadError = "No se pudieron cargar las recetas. Inténtalo de nuevo."
    override val recipeAiFailed = "No se pudo generar una receta con IA."
    override val recipeLocalSuffix = "Te mostramos recetas locales."

    // ---- Impact ----
    override val impactTitle = "Tu impacto"
    override val impactSubtitle = "Lo que logras al comer antes de que venza"
    override val impactEmptyTitle = "Aún no hay impacto registrado"
    override val impactEmptyHint = "Pulsa «Consumido» en tus productos de la alacena antes de que " +
        "venzan y aquí verás cuánto ahorras tú y el planeta."
    override val moneyTitle = "Dinero ahorrado"
    override val foodTitle = "Comida no desperdiciada"
    override val co2Title = "Emisiones de CO₂e evitadas"
    override val co2Unit = "kg de CO₂e"
    override val equivalences = "Equivalencias"
    override val treesTitle = "Árboles equivalentes salvados"
    override val treesUnit = "árboles trabajando durante 1 año"
    override val kmTitle = "Km equivalentes no conducidos"
    override val kmUnit = "km en auto"
    override val tapForDetails = "Toca para ver cómo se calcula"
    override val tapToHide = "Toca para ocultar"

    override fun explainMoney(pricePerKg: String, copPerUsd: String) =
        "Estimado con un precio promedio de \$ $pricePerKg COP por kg de alimento y " +
            "1 USD ≈ \$ $copPerUsd COP (TRM de octubre de 2026)."

    override val explainFood = "Suma del peso de los productos que marcaste como consumidos antes de " +
        "vencer. Los productos contados por unidad o paquete usan un peso promedio de su categoría."

    override fun explainCo2(kgPerKg: String) =
        "Cada kg de comida que no se desperdicia evita aproximadamente $kgPerKg kg de CO₂e " +
            "(producción, transporte y descomposición)."

    override fun explainTrees(kgPerYear: String) =
        "Un árbol establecido absorbe unos $kgPerYear kg de CO₂ al año. Es el número de árboles que " +
            "necesitarían un año para absorber el CO₂e que evitaste."

    override fun explainKm(kgPerKm: String) =
        "Un auto de pasajeros promedio emite unos $kgPerKm kg de CO₂e por kilómetro (referencia EPA)."

    override val impactDisclaimer = "ODS 12 · Producción y consumo responsables. Todas las cifras son " +
        "estimaciones basadas en promedios; el impacto real depende del tipo de alimento."

    // ---- Notifications ----
    override val notificationChannelName = "Alertas de la alacena"
    override val notificationChannelDescription =
        "Avisos cuando un alimento de tu alacena está por vencer o se acaba"

    override fun expirationAlertTitle(name: String, daysLeft: Int) =
        when (daysLeft) {
            0 -> "⚠️ ¡$name vence hoy!"
            1 -> "⚠️ ¡$name vence mañana!"
            else -> "⚠️ ¡$name vence en $daysLeft días!"
        }

    override val expirationAlertBody = "Revisa las recetas para aprovecharlo antes de que se dañe."
    override fun usedUpAlertTitle(name: String) = "🧺 Se acabó: $name"
    override val usedUpAlertBody =
        "Ya aparece en las sugerencias de tu lista de compras. Compártela con tu hogar para no comprarlo dos veces."
    override val testAlertTitle = "🥬 Las alertas de Freshly están activas"
    override val testAlertBody = "Así te avisaremos cuando un alimento esté por vencer."

    // ---- Settings ----
    override val settingsTitle = "Ajustes"
    override val settingsSubtitle = "Notificaciones y alertas"
    override val openSettings = "Abrir ajustes"
    override val notificationsEnabled = "Notificaciones"
    override val notificationsEnabledHint = "Avisos de alimentos que están por vencer"
    override val alertLeadTitle = "Cuándo avisarme"

    override fun alertLeadLabel(lead: AlertLead) =
        when (lead) {
            AlertLead.SAME_DAY -> "El mismo día"
            AlertLead.ONE_DAY -> "1 día antes"
            AlertLead.TWO_DAYS -> "2 días antes"
        }

    override val alertHourTitle = "Hora del aviso"
    override val householdUpdates = "Avisos para el hogar"
    override val householdUpdatesHint =
        "Avisa cuando algo se acaba, para contárselo a tu hogar y no comprarlo dos veces"
    override val sendTestAlert = "Enviar alerta de prueba"
    override val testAlertSent = "Alerta de prueba enviada"
    override val notificationsDenied =
        "Las notificaciones están bloqueadas. Permítelas para Freshly en los ajustes del sistema."
    override val notificationsDeviceNote =
        "Las alertas se guardan en este dispositivo. En el computador, al cerrar la ventana Freshly sigue en la " +
            "barra de menú para que las alertas lleguen; si sales de la app, se detienen hasta que la abras. " +
            "Los avisos del hogar no se sincronizan entre dispositivos: comparte tu lista desde Compras."

    // ---- Desktop menu bar / tray ----
    override val trayOpen = "Abrir Freshly"
    override val trayQuit = "Salir de Freshly"
    override val trayStillRunningTitle = "Freshly sigue activo"
    override val trayStillRunningBody =
        "Queda en la barra de menú para que lleguen tus alertas de vencimiento. Puedes salir desde ahí."
}
