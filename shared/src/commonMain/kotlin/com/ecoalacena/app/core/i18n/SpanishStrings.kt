package com.ecoalacena.app.core.i18n

import com.ecoalacena.app.domain.model.Category
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
        "venció o lo botaste). No suma a tu impacto."

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

    override val reviewHint = "Revisa los datos: toca la cantidad o la fecha de caducidad para cambiarlas."
    override val oneDayLess = "Un día menos"
    override val oneDayMore = "Un día más"
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
    override val recipesSubtitle = "Cocina primero lo que vence primero"

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
    override val notificationChannelName = "Recordatorios de vencimiento"
    override val notificationChannelDescription = "Avisos cuando un alimento de tu alacena está por vencer"
    override val notificationTitle = "Próximo a vencer"

    override fun notificationBody(name: String, daysLeft: Int) =
        when (daysLeft) {
            0 -> "“$name” vence hoy"
            1 -> "“$name” vence mañana"
            else -> "“$name” vence en $daysLeft días"
        }
}
