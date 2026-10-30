package com.ecoalacena.app.domain.model

/**
 * Spanish and English words that identify a kind of food. Every word is lowercase and has no
 * accents, because product names are normalised with [foodTokens] before they are compared.
 * Plurals are matched automatically (see [hasAny]), so only the singular form is listed.
 */
internal object FoodKeywords {

    // ---- Used to estimate how long a product lasts ----

    val EGGS = setOf("huevo", "egg")

    /** Words that say the product is canned or preserved, whatever its category. */
    val CANNED_FORMS = setOf("enlatado", "enlatada", "lata", "conserva", "canned", "tinned")

    val DRY_STAPLES = setOf(
        "arroz", "rice", "lenteja", "lentil", "frijol", "bean", "garbanzo", "chickpea",
        "pasta", "espagueti", "spaghetti", "macarron", "macaroni", "fideo", "noodle",
        "harina", "flour", "azucar", "sugar", "sal", "salt", "avena", "oat", "oatmeal",
        "cereal", "quinoa",
    )

    val COLD_CUTS = setOf(
        "jamon", "ham", "salchicha", "sausage", "hotdog", "bacon", "tocino", "chorizo",
        "salami", "mortadela", "embutido", "pepperoni", "prosciutto", "fiambre",
    )

    /** Fresh produce that spoils faster than the rest: berries, leafy greens, mushrooms, herbs. */
    val DELICATE_PRODUCE = setOf(
        "fresa", "strawberry", "arandano", "blueberry", "mora", "blackberry", "frambuesa",
        "raspberry", "cereza", "cherry", "lechuga", "lettuce", "espinaca", "spinach", "kale",
        "rucula", "arugula", "champinon", "mushroom", "cilantro", "perejil", "parsley",
        "albahaca", "basil", "esparrago", "asparagus",
    )

    // ---- Used to guess the category of a product from its name ----

    /** Phrases that must win over the single words they contain ("peanut butter" is not dairy). */
    val PANTRY_PHRASES = listOf(
        "peanut butter", "mantequilla de mani", "coconut milk", "leche de coco",
        "almond milk", "leche de almendra", "soy milk", "leche de soya", "oat milk",
        "leche de avena", "tomato sauce", "salsa de tomate",
    )

    val PRODUCE_PHRASES = listOf("green bean", "judia verde")

    /** Words for processed products that belong in the pantry even if they name a fresh ingredient. */
    val PANTRY_FORMS = CANNED_FORMS + setOf(
        "salsa", "sauce", "ketchup", "mayonesa", "mayonnaise", "mermelada", "jam", "jelly",
        "sopa", "soup", "caldo", "broth",
    )

    val BEVERAGES = setOf(
        "jugo", "juice", "gaseosa", "soda", "refresco", "agua", "water", "cerveza", "beer",
        "vino", "wine", "cafe", "coffee", "tea", "bebida", "drink", "beverage", "limonada",
        "lemonade", "kombucha", "cola",
    )

    val MEAT_FISH = COLD_CUTS + setOf(
        "carne", "meat", "res", "beef", "cerdo", "pork", "pollo", "chicken", "pavo", "turkey",
        "pescado", "fish", "salmon", "tilapia", "trucha", "trout", "camaron", "shrimp",
        "langostino", "prawn", "cordero", "lamb", "costilla", "rib", "filete", "fillet",
        "steak", "bistec", "pechuga", "hamburguesa", "burger",
    )

    val DAIRY = EGGS + setOf(
        "leche", "milk", "yogur", "yogurt", "yoghurt", "queso", "cheese", "mantequilla",
        "butter", "crema", "cream", "kefir", "requeson", "cottage", "ricotta", "mozzarella",
        "cheddar", "parmesano", "parmesan", "lacteo", "dairy",
    )

    val BAKERY = setOf(
        "pan", "bread", "baguette", "bagel", "tortilla", "croissant", "galleta", "cookie",
        "biscuit", "cake", "pastel", "pastry", "muffin", "donut", "dona", "bizcocho", "arepa",
        "empanada", "waffle", "brownie", "panqueque", "pancake",
    )

    val FRUITS_VEGETABLES = DELICATE_PRODUCE + setOf(
        "manzana", "apple", "banano", "banana", "platano", "naranja", "orange", "limon", "lemon",
        "lima", "lime", "mandarina", "tangerine", "pera", "pear", "durazno", "peach", "uva",
        "grape", "sandia", "watermelon", "melon", "papaya", "mango", "pina", "pineapple", "kiwi",
        "aguacate", "avocado", "tomate", "tomato", "cebolla", "onion", "ajo", "garlic", "papa",
        "potato", "patata", "zanahoria", "carrot", "brocoli", "broccoli", "coliflor",
        "cauliflower", "pepino", "cucumber", "apio", "celery", "calabacin", "zucchini",
        "berenjena", "eggplant", "repollo", "cabbage", "ahuyama", "pumpkin", "yuca", "fruta",
        "fruit", "verdura", "vegetable", "ensalada", "salad", "habichuela",
    )

    val PANTRY_STAPLES = DRY_STAPLES + setOf(
        "aceite", "oil", "vinagre", "vinegar", "mostaza", "mustard", "miel", "honey",
        "chocolate", "snack", "chip", "nuez", "nut", "almendra", "almond", "mani", "peanut",
        "galletita", "pretzel", "granola",
    )
}

private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")

private fun Char.withoutAccent(): Char =
    when (this) {
        'á' -> 'a'
        'é' -> 'e'
        'í' -> 'i'
        'ó' -> 'o'
        'ú', 'ü' -> 'u'
        'ñ' -> 'n'
        else -> this
    }

/** Lowercase words of a product name without accents: "Arándanos orgánicos" -> [arandanos, organicos]. */
internal fun String.foodTokens(): List<String> =
    lowercase()
        .map { it.withoutAccent() }
        .joinToString(separator = "")
        .split(NON_ALPHANUMERIC)
        .filter { it.isNotEmpty() }

/** True if any word equals one of the [keywords] or its plural ("manzanas", "tomatoes", "blueberries"). */
internal fun List<String>.hasAny(keywords: Set<String>): Boolean =
    any { token -> keywords.any { keyword -> token.isFormOf(keyword) } }

private fun String.isFormOf(keyword: String): Boolean =
    this == keyword ||
        this == keyword + "s" ||
        this == keyword + "es" ||
        (keyword.endsWith("y") && this == keyword.dropLast(1) + "ies")
