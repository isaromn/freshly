package com.ecoalacena.app.domain.model

import com.ecoalacena.app.core.i18n.AppLanguage

/**
 * Shows the name of a well-known food in [language], so a recipe in English says "eggs" even if the
 * product was saved as "huevos" (and the other way around). It only changes what is displayed: the
 * name stored in the pantry is never modified.
 *
 * Only names made of ONE known word are translated, keeping the plural and the capitalization
 * ("Huevos" -> "Eggs", "tomates" -> "tomatoes"). Anything else is returned untouched: names that are
 * already in [language], unknown products ("Panela") and names with several words ("Leche entera").
 */
internal fun String.localizedFoodName(language: AppLanguage): String {
    val tokens = trim().foodTokens()
    if (tokens.size != 1) return this
    val token = tokens.single()

    val identified = FoodNames.TERMS.mapNotNull { term -> term.identify(token)?.let { term to it } }

    // The word already exists in the target language: nothing to translate.
    if (identified.any { (_, found) -> found.language == language }) return this

    val (term, found) = identified.firstOrNull() ?: return this
    val translated = if (found.isPlural) term.plural(language) else term.singular(language)

    return if (trim().first().isUpperCase()) translated.replaceFirstChar { it.uppercaseChar() } else translated
}

private object FoodNames {

    val TERMS = listOf(
        // Dairy and eggs
        FoodTerm("huevo", "egg"),
        FoodTerm("leche", "milk"),
        FoodTerm("yogur", "yogurt"),
        FoodTerm("queso", "cheese"),
        FoodTerm("mantequilla", "butter"),
        FoodTerm("crema", "cream"),

        // Meat and fish
        FoodTerm("pollo", "chicken"),
        FoodTerm("carne", "meat"),
        FoodTerm("res", "beef"),
        FoodTerm("cerdo", "pork"),
        FoodTerm("pescado", "fish", enPlural = "fish"),
        FoodTerm("atún", "tuna"),
        FoodTerm("salchicha", "sausage"),
        FoodTerm("jamón", "ham"),
        FoodTerm("tocino", "bacon"),
        FoodTerm("pavo", "turkey"),
        FoodTerm("camarón", "shrimp", enPlural = "shrimp"),

        // Fruit
        FoodTerm("manzana", "apple"),
        FoodTerm("banano", "banana"),
        FoodTerm("fresa", "strawberry"),
        FoodTerm("naranja", "orange"),
        FoodTerm("pera", "pear"),
        FoodTerm("uva", "grape"),
        FoodTerm("piña", "pineapple"),
        FoodTerm("sandía", "watermelon"),
        FoodTerm("melón", "melon"),
        FoodTerm("mandarina", "tangerine"),
        FoodTerm("durazno", "peach"),
        FoodTerm("mora", "blackberry"),
        FoodTerm("arándano", "blueberry"),
        FoodTerm("frambuesa", "raspberry"),
        FoodTerm("cereza", "cherry"),
        FoodTerm("guayaba", "guava"),
        FoodTerm("limón", "lemon"),
        FoodTerm("lima", "lime"),
        FoodTerm("aguacate", "avocado"),

        // Vegetables
        FoodTerm("zanahoria", "carrot"),
        FoodTerm("papa", "potato", enPlural = "potatoes"),
        FoodTerm("cebolla", "onion"),
        FoodTerm("apio", "celery"),
        FoodTerm("tomate", "tomato", enPlural = "tomatoes"),
        FoodTerm("calabacín", "zucchini"),
        FoodTerm("zapallo", "squash"),
        FoodTerm("brócoli", "broccoli"),
        FoodTerm("coliflor", "cauliflower"),
        FoodTerm("espinaca", "spinach"),
        FoodTerm("arveja", "pea"),
        FoodTerm("ahuyama", "pumpkin"),
        FoodTerm("yuca", "cassava"),
        FoodTerm("lechuga", "lettuce"),
        FoodTerm("pepino", "cucumber"),
        FoodTerm("repollo", "cabbage"),
        FoodTerm("champiñón", "mushroom"),
        FoodTerm("maíz", "corn"),
        FoodTerm("ajo", "garlic"),

        // Pantry
        FoodTerm("arroz", "rice"),
        FoodTerm("lenteja", "lentil"),
        FoodTerm("frijol", "bean"),
        FoodTerm("garbanzo", "chickpea"),
        FoodTerm("espagueti", "spaghetti"),
        FoodTerm("fideo", "noodle"),
        FoodTerm("harina", "flour"),
        FoodTerm("azúcar", "sugar"),
        FoodTerm("avena", "oats", enPlural = "oats"),
        FoodTerm("aceite", "oil"),
        FoodTerm("vinagre", "vinegar"),
        FoodTerm("miel", "honey"),
        FoodTerm("almendra", "almond"),
        FoodTerm("maní", "peanut"),

        // Bakery
        FoodTerm("pan", "bread"),
        FoodTerm("galleta", "cookie"),
        FoodTerm("pastel", "cake"),

        // Beverages
        FoodTerm("jugo", "juice"),
        FoodTerm("agua", "water"),
        FoodTerm("cerveza", "beer"),
        FoodTerm("vino", "wine"),
        FoodTerm("café", "coffee"),
        FoodTerm("gaseosa", "soda"),
    )
}

/** A food with its name in Spanish and in English. Irregular plurals can be given explicitly. */
private class FoodTerm(
    private val es: String,
    private val en: String,
    private val esPlural: String? = null,
    private val enPlural: String? = null,
) {

    fun singular(language: AppLanguage): String =
        when (language) {
            AppLanguage.ES -> es
            AppLanguage.EN -> en
        }

    fun plural(language: AppLanguage): String =
        when (language) {
            AppLanguage.ES -> esPlural ?: spanishPlural(es)
            AppLanguage.EN -> enPlural ?: englishPlural(en)
        }

    /** The language and number of [token] (lowercase, no accents) if it is a form of this food. */
    fun identify(token: String): Found? {
        for (language in AppLanguage.entries) {
            val singular = singular(language).key()
            val plural = plural(language).key()
            when (token) {
                singular -> return Found(language, isPlural = false)
                plural, singular + "s", singular + "es" -> return Found(language, isPlural = true)
            }
        }
        return null
    }

    class Found(val language: AppLanguage, val isPlural: Boolean)

    private fun String.key(): String = foodTokens().joinToString(separator = "")

    private fun spanishPlural(word: String): String =
        when {
            word.endsWith("ón") -> word.dropLast(2) + "ones"
            word.endsWith("z") -> word.dropLast(1) + "ces"
            word.last() in "aeiouáéíóú" -> word + "s"
            else -> word + "es"
        }

    private fun englishPlural(word: String): String =
        when {
            word.endsWith("y") && word.length > 1 && word[word.length - 2] !in "aeiou" ->
                word.dropLast(1) + "ies"

            word.endsWith("s") || word.endsWith("x") || word.endsWith("z") ||
                word.endsWith("ch") || word.endsWith("sh") -> word + "es"

            else -> word + "s"
        }
}
