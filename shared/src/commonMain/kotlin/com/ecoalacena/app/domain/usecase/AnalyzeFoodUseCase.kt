package com.ecoalacena.app.domain.usecase

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.currentEpochMillis
import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.core.i18n.currentLanguage
import com.ecoalacena.app.data.demo.DemoReceipt
import com.ecoalacena.app.data.remote.ai.GeminiAnalysisResponse
import com.ecoalacena.app.data.remote.ai.GeminiAnalysisResult
import com.ecoalacena.app.data.remote.ai.GeminiVisionService
import com.ecoalacena.app.data.remote.ai.geminiJson
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.FoodClassifier
import com.ecoalacena.app.domain.model.PantryItem
import kotlin.math.round
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Analyses a photo (a product or a supermarket receipt, in Spanish or English) with Gemini and
 * turns the answer into [PantryItem]s pre-filled with a category and an expiration date.
 *
 * Nothing the model leaves out stays empty:
 * - if the category is missing or unknown, it is guessed from the product name in either language;
 * - if there is no expiration estimate, [estimateExpiration] provides one.
 *
 * The answer is also normalised to fit the domain model: ALL CAPS receipt names become
 * "Whole Milk", and English or Spanish units ("lb", "oz", "gal", "dozen", "each"...) are
 * converted to the units the app uses: kg, g, l, ml, unit and pack.
 *
 * The items are NOT saved: the UI should let the user review and edit them
 * before calling `PantryRepository.upsert`.
 */
class AnalyzeFoodUseCase(
    private val visionService: GeminiVisionService,
    private val nowMillis: () -> Long = ::currentEpochMillis,
    private val estimateExpiration: EstimateExpirationDateUseCase = EstimateExpirationDateUseCase(nowMillis),
    /** Language of the demo receipt's product names. */
    private val language: () -> AppLanguage = ::currentLanguage,
) {

    suspend operator fun invoke(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
    ): Result<List<PantryItem>> =
        visionService.analyzeImage(imageBytes, mimeType).map { it.toPantryItems() }

    suspend fun fromBase64(
        base64Image: String,
        mimeType: String = "image/jpeg",
    ): Result<List<PantryItem>> =
        visionService.analyzeImageBase64(base64Image, mimeType).map { it.toPantryItems() }

    /**
     * Demo mode: turns a preloaded supermarket receipt into pantry items without network access
     * or an API key, so the app can be shown with no internet. It uses the same mapping as a
     * real scan.
     */
    fun demoReceipt(): Result<List<PantryItem>> =
        try {
            val response = geminiJson.decodeFromString<GeminiAnalysisResponse>(DemoReceipt.json(language()))
            Result.success(response.items.toPantryItems())
        } catch (error: Exception) {
            Result.failure(error)
        }

    private fun List<GeminiAnalysisResult>.toPantryItems(): List<PantryItem> {
        val now = nowMillis()
        return mapNotNull { result -> result.toPantryItem(now) }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun GeminiAnalysisResult.toPantryItem(now: Long): PantryItem? {
        val cleanName = name.toDisplayName()
        if (cleanName.isEmpty()) return null

        val itemCategory = resolveCategory(cleanName)
        val expirationDate = estimatedDaysUntilExpiration
            ?.let { days -> now + days.coerceIn(0, MAX_SHELF_LIFE_DAYS) * MILLIS_PER_DAY }
            ?: estimateExpiration(cleanName, itemCategory, now)
        val measure = normalizeMeasure(quantity, unit)

        return PantryItem(
            id = Uuid.random().toString(),
            name = cleanName,
            quantity = measure.quantity,
            unit = measure.unit,
            expirationDate = expirationDate,
            isOrganic = isOrganic,
            category = itemCategory,
        )
    }

    /** The model's category when it is usable; otherwise the one the name points to ("Blueberries"). */
    private fun GeminiAnalysisResult.resolveCategory(cleanName: String): Category =
        Category.fromRaw(category).takeUnless { it == Category.OTHER }
            ?: FoodClassifier.categoryOf(cleanName)
            ?: Category.OTHER

    /** A quantity with one of the units the app understands. */
    private data class Measure(val quantity: Double, val unit: String)

    /**
     * Converts what the model returned ("2.5" + "lb", "1" + "dozen", "3" + "pcs") into the units
     * of `PantryItem` (kg, g, l, ml, unit, pack). Unknown units are counted as `unit`, so the
     * item still gets an estimated weight for the impact dashboard.
     */
    private fun normalizeMeasure(rawQuantity: Double, rawUnit: String): Measure {
        val amount = rawQuantity.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
        val key = rawUnit.trim().lowercase().trimEnd('.').replace(WHITESPACE, " ")

        return when (key) {
            in KILOGRAM_UNITS -> Measure(amount, "kg")
            in GRAM_UNITS -> Measure(amount, "g")
            in LITER_UNITS -> Measure(amount, "l")
            in MILLILITER_UNITS -> Measure(amount, "ml")
            in POUND_UNITS -> Measure(amount * KG_PER_POUND, "kg")
            in OUNCE_UNITS -> Measure(amount * GRAMS_PER_OUNCE, "g")
            in GALLON_UNITS -> Measure(amount * LITERS_PER_GALLON, "l")
            in FLUID_OUNCE_UNITS -> Measure(amount * ML_PER_FLUID_OUNCE, "ml")
            in DOZEN_UNITS -> Measure(amount * 12, "unit")
            in PACK_UNITS -> Measure(amount, "pack")
            else -> Measure(amount, DEFAULT_UNIT)
        }.let { Measure(it.quantity.roundTo(2), it.unit) }
    }

    private fun Double.roundTo(decimals: Int): Double {
        var factor = 1.0
        repeat(decimals) { factor *= 10 }
        return round(this * factor) / factor
    }

    /** Collapses spaces and turns ALL CAPS receipt names into "Title Case"; mixed case is kept. */
    private fun String.toDisplayName(): String {
        val cleaned = trim().replace(WHITESPACE, " ").trim(' ', '.', ',', '-', '*')
        if (cleaned.any { it.isLowerCase() }) return cleaned
        return cleaned.lowercase().split(' ').joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercaseChar() }
        }
    }

    private companion object {
        const val MAX_SHELF_LIFE_DAYS = 3650
        const val DEFAULT_UNIT = "unit"

        const val KG_PER_POUND = 0.45359237
        const val GRAMS_PER_OUNCE = 28.349523
        const val LITERS_PER_GALLON = 3.785412
        const val ML_PER_FLUID_OUNCE = 29.5735

        val WHITESPACE = Regex("\\s+")

        val KILOGRAM_UNITS = setOf("kg", "kgs", "kilo", "kilos", "kilogram", "kilograms", "kilogramo", "kilogramos")
        val GRAM_UNITS = setOf("g", "gr", "grs", "gram", "grams", "gramo", "gramos")
        val LITER_UNITS = setOf("l", "lt", "lts", "liter", "liters", "litre", "litres", "litro", "litros")
        val MILLILITER_UNITS = setOf("ml", "cc", "milliliter", "milliliters", "mililitro", "mililitros")
        val POUND_UNITS = setOf("lb", "lbs", "pound", "pounds", "libra", "libras")
        val OUNCE_UNITS = setOf("oz", "ounce", "ounces", "onza", "onzas")
        val GALLON_UNITS = setOf("gal", "gallon", "gallons", "galon", "galones")
        val FLUID_OUNCE_UNITS = setOf("fl oz", "floz", "fluid ounce", "fluid ounces")
        val DOZEN_UNITS = setOf("dozen", "doz", "dz", "docena", "docenas")
        val PACK_UNITS = setOf(
            "pack", "packs", "pk", "pkg", "package", "packages", "paquete", "paquetes",
            "bag", "bags", "bolsa", "bolsas", "box", "boxes", "caja", "cajas",
            "carton", "cartons", "tray", "trays", "bandeja", "bandejas",
        )
    }
}
