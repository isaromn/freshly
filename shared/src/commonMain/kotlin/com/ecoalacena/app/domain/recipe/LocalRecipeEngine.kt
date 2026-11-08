package com.ecoalacena.app.domain.recipe

import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.core.i18n.currentLanguage
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.model.Recipe
import com.ecoalacena.app.domain.model.RecipeCategory
import com.ecoalacena.app.domain.model.RecipeDifficulty
import com.ecoalacena.app.domain.model.RecipeSource
import com.ecoalacena.app.domain.model.amountText
import com.ecoalacena.app.domain.model.localizedFoodName

/**
 * Offline recipe engine: matches pantry items against a small catalogue of templates,
 * so the app can still suggest something to cook without a network connection or AI.
 *
 * Items are matched by whole words (plurals included), so "Manzanas" matches "manzana"
 * but "Panela" does not match "pan". The keywords are in Spanish and English, so an item is
 * found whatever language its name is in, and the recipe text is written in the output language.
 */
class LocalRecipeEngine(
    /** Language of the device, read on every call, so the recipes match the UI. */
    private val language: () -> AppLanguage = ::currentLanguage,
) {

    /**
     * Returns up to [limit] recipes, best match first. When no template fits, a generic
     * "use what you have" recipe is returned. [ingredients] should be ordered by urgency.
     *
     * @param outputLanguage language of the recipe text; the device language by default.
     */
    fun suggest(
        ingredients: List<PantryItem>,
        limit: Int = DEFAULT_LIMIT,
        outputLanguage: AppLanguage = language(),
    ): List<Recipe> {
        if (ingredients.isEmpty() || limit <= 0) return emptyList()

        val tokenized = ingredients.map { item -> item to item.name.tokens() }

        val matches = TEMPLATES
            .mapNotNull { template -> template.matchAgainst(tokenized) }
            .sortedByDescending { match -> match.items.size }
            .take(limit)

        return if (matches.isEmpty()) {
            listOf(genericRecipe(ingredients, outputLanguage))
        } else {
            matches.map { match -> match.toRecipe(outputLanguage) }
        }
    }

    private fun genericRecipe(ingredients: List<PantryItem>, outputLanguage: AppLanguage): Recipe {
        val text = when (outputLanguage) {
            AppLanguage.ES -> GENERIC_ES
            AppLanguage.EN -> GENERIC_EN
        }
        return Recipe(
            id = "local-generic",
            title = text.title,
            ingredients = ingredients.take(GENERIC_MAX_INGREDIENTS).map { it.asIngredientLine(outputLanguage) } +
                text.extraIngredients,
            steps = text.steps,
            prepTimeMinutes = 20,
            difficulty = RecipeDifficulty.EASY,
            source = RecipeSource.OFFLINE,
            category = RecipeCategory.OTHER,
            imageQuery = "vegetable stir fry",
            pantryItemIds = ingredients.take(GENERIC_MAX_INGREDIENTS).map { it.id },
        ).withEstimatedNutrition(ingredients.take(GENERIC_MAX_INGREDIENTS), oilTablespoons = 2)
    }

    /** The words of one recipe in one language. */
    private class RecipeText(
        val title: String,
        val extraIngredients: List<String>,
        val steps: List<String>,
    )

    private class TemplateMatch(
        val template: RecipeTemplate,
        val items: List<PantryItem>,
    ) {
        fun toRecipe(outputLanguage: AppLanguage): Recipe {
            val text = template.text(outputLanguage)
            return Recipe(
                id = "local-${template.id}",
                title = text.title,
                ingredients = items.map { it.asIngredientLine(outputLanguage) } + text.extraIngredients,
                steps = text.steps,
                prepTimeMinutes = template.prepTimeMinutes,
                difficulty = template.difficulty,
                source = RecipeSource.OFFLINE,
                category = template.category,
                imageQuery = template.imageQuery,
                pantryItemIds = items.map { it.id },
            ).withEstimatedNutrition(items, template.oilTablespoons)
        }
    }

    private class RecipeTemplate(
        val id: String,
        /** Words (Spanish and English) that make a pantry item useful for this recipe. */
        val keywords: Set<String>,
        /**
         * Groups of words that must all be present: for each group, at least one pantry item must
         * match one of its words (e.g. "rice" and "milk" for rice pudding).
         */
        val required: List<Set<String>> = emptyList(),
        /** Tablespoons of cooking oil (or butter) the recipe adds, for the calorie estimate. */
        val oilTablespoons: Int = 0,
        val es: RecipeText,
        val en: RecipeText,
        val prepTimeMinutes: Int,
        val difficulty: RecipeDifficulty,
        val category: RecipeCategory,
        /** English words to search a photo of the dish when there is internet. */
        val imageQuery: String,
    ) {
        fun text(outputLanguage: AppLanguage): RecipeText =
            when (outputLanguage) {
                AppLanguage.ES -> es
                AppLanguage.EN -> en
            }

        fun matchAgainst(tokenized: List<Pair<PantryItem, Set<String>>>): TemplateMatch? {
            val matched = tokenized
                .filter { (_, tokens) -> tokens.any { it.matchesAny(keywords) } }
                .map { (item, _) -> item }
            if (matched.isEmpty()) return null

            val hasEveryRequired = required.all { group ->
                tokenized.any { (_, tokens) -> tokens.any { it.matchesAny(group) } }
            }
            if (!hasEveryRequired) return null
            return TemplateMatch(this, matched)
        }
    }

    private companion object {
        const val DEFAULT_LIMIT = 2
        const val GENERIC_MAX_INGREDIENTS = 6

        val FRUITS = setOf(
            // Spanish
            "manzana", "banano", "fresa", "naranja", "pera", "mango", "papaya", "uva", "pina",
            "melon", "sandia", "mandarina", "kiwi", "fruta", "durazno", "mora", "guayaba", "lulo",
            // English
            "apple", "banana", "strawberry", "orange", "pear", "grape", "pineapple", "watermelon",
            "tangerine", "fruit", "peach", "blackberry", "blueberry", "raspberry", "cherry", "guava",
        )

        val VEGETABLES = setOf(
            // Spanish
            "zanahoria", "papa", "cebolla", "apio", "tomate", "verdura", "calabacin", "zapallo",
            "brocoli", "coliflor", "espinaca", "arveja", "habichuela", "ahuyama", "yuca", "pimenton",
            "lechuga", "pepino", "repollo", "champinon", "maiz",
            // English
            "carrot", "potato", "onion", "celery", "tomato", "vegetable", "zucchini", "squash",
            "broccoli", "cauliflower", "spinach", "pea", "pumpkin", "cassava", "lettuce", "cucumber",
            "cabbage", "mushroom", "corn",
        )

        val PROTEINS = setOf(
            // Spanish
            "pollo", "carne", "res", "cerdo", "pescado", "atun", "salchicha", "pavo", "salmon",
            // English
            "chicken", "meat", "beef", "pork", "fish", "tuna", "sausage", "turkey",
        )

        val GENERIC_ES = RecipeText(
            title = "Salteado de aprovechamiento",
            extraIngredients = listOf("2 cucharadas de aceite", "Sal y pimienta al gusto"),
            steps = listOf(
                "Lava, pela y corta los ingredientes en trozos pequeños, empezando por los que vencen primero.",
                "Calienta el aceite en una sartén grande a fuego medio-alto.",
                "Saltea primero los ingredientes más duros durante 5 minutos y luego agrega los más tiernos.",
                "Cocina 5 minutos más moviendo con frecuencia y sazona con sal y pimienta.",
                "Sirve caliente, solo o acompañado de arroz, pan o tortillas.",
            ),
        )

        val GENERIC_EN = RecipeText(
            title = "Use-it-up stir-fry",
            extraIngredients = listOf("2 tablespoons of oil", "Salt and pepper to taste"),
            steps = listOf(
                "Wash, peel and chop the ingredients into small pieces, starting with the ones that expire first.",
                "Heat the oil in a large pan over medium-high heat.",
                "Sauté the harder ingredients first for 5 minutes, then add the softer ones.",
                "Cook 5 more minutes, stirring often, and season with salt and pepper.",
                "Serve hot, on its own or with rice, bread or tortillas.",
            ),
        )

        val TEMPLATES = listOf(
            RecipeTemplate(
                id = "vegetable-soup",
                oilTablespoons = 1,
                category = RecipeCategory.SOUP,
                imageQuery = "vegetable soup",
                keywords = VEGETABLES,
                es = RecipeText(
                    title = "Sopa casera de verduras",
                    extraIngredients = listOf(
                        "1 litro de agua o caldo",
                        "1 cucharada de aceite",
                        "Sal y pimienta al gusto",
                    ),
                    steps = listOf(
                        "Lava y pica en trozos pequeños las verduras que más pronto vencen.",
                        "Calienta el aceite en una olla y sofríe las verduras durante 3 minutos.",
                        "Agrega el agua o el caldo y deja que hierva.",
                        "Cocina a fuego medio unos 20 minutos, hasta que estén blandas.",
                        "Ajusta de sal y pimienta y sirve caliente.",
                    ),
                ),
                en = RecipeText(
                    title = "Homemade vegetable soup",
                    extraIngredients = listOf(
                        "1 liter of water or broth",
                        "1 tablespoon of oil",
                        "Salt and pepper to taste",
                    ),
                    steps = listOf(
                        "Wash and chop the vegetables that expire first into small pieces.",
                        "Heat the oil in a pot and sauté the vegetables for 3 minutes.",
                        "Add the water or broth and bring to a boil.",
                        "Cook over medium heat for about 20 minutes, until soft.",
                        "Adjust the salt and pepper and serve hot.",
                    ),
                ),
                prepTimeMinutes = 30,
                difficulty = RecipeDifficulty.EASY,
            ),
            RecipeTemplate(
                id = "fruit-salad",
                category = RecipeCategory.SALAD,
                imageQuery = "fruit salad",
                keywords = FRUITS,
                es = RecipeText(
                    title = "Ensalada de frutas",
                    extraIngredients = listOf(
                        "Jugo de 1 limón o naranja",
                        "1 cucharadita de miel (opcional)",
                        "Yogur natural (opcional)",
                    ),
                    steps = listOf(
                        "Lava bien las frutas y córtalas en cubos del mismo tamaño.",
                        "Mézclalas en un recipiente con el jugo de limón o naranja para que no se oxiden.",
                        "Agrega la miel y el yogur si lo deseas.",
                        "Deja reposar 10 minutos en la nevera y sirve fría.",
                    ),
                ),
                en = RecipeText(
                    title = "Fruit salad",
                    extraIngredients = listOf(
                        "Juice of 1 lemon or orange",
                        "1 teaspoon of honey (optional)",
                        "Plain yogurt (optional)",
                    ),
                    steps = listOf(
                        "Wash the fruit well and cut it into even cubes.",
                        "Toss it in a bowl with the lemon or orange juice so it doesn't brown.",
                        "Add the honey and yogurt if you like.",
                        "Let it rest in the fridge for 10 minutes and serve cold.",
                    ),
                ),
                prepTimeMinutes = 10,
                difficulty = RecipeDifficulty.EASY,
            ),
            RecipeTemplate(
                id = "egg-scramble",
                oilTablespoons = 1,
                category = RecipeCategory.EGGS,
                imageQuery = "scrambled eggs vegetables",
                keywords = VEGETABLES + setOf("huevo", "queso", "jamon", "egg", "cheese", "ham"),
                required = listOf(setOf("huevo", "egg")),
                es = RecipeText(
                    title = "Revuelto de huevo con vegetales",
                    extraIngredients = listOf(
                        "1 cucharada de aceite o mantequilla",
                        "Sal y pimienta al gusto",
                    ),
                    steps = listOf(
                        "Pica los vegetales en trozos pequeños.",
                        "Sofríelos en una sartén con el aceite durante 3 minutos.",
                        "Bate los huevos con una pizca de sal y viértelos sobre los vegetales.",
                        "Revuelve a fuego bajo hasta que cuajen y sirve de inmediato.",
                    ),
                ),
                en = RecipeText(
                    title = "Scrambled eggs with vegetables",
                    extraIngredients = listOf(
                        "1 tablespoon of oil or butter",
                        "Salt and pepper to taste",
                    ),
                    steps = listOf(
                        "Chop the vegetables into small pieces.",
                        "Sauté them in a pan with the oil for 3 minutes.",
                        "Beat the eggs with a pinch of salt and pour them over the vegetables.",
                        "Stir over low heat until set and serve right away.",
                    ),
                ),
                prepTimeMinutes = 15,
                difficulty = RecipeDifficulty.EASY,
            ),
            RecipeTemplate(
                id = "creamy-smoothie",
                category = RecipeCategory.DRINK,
                imageQuery = "fruit smoothie",
                keywords = FRUITS + setOf("leche", "yogur", "yogurt", "avena", "milk", "yoghurt", "oat", "oatmeal"),
                required = listOf(setOf("leche", "yogur", "yogurt", "milk", "yoghurt")),
                es = RecipeText(
                    title = "Batido cremoso",
                    extraIngredients = listOf(
                        "Hielo (opcional)",
                        "Azúcar o miel al gusto (opcional)",
                    ),
                    steps = listOf(
                        "Pela y trocea las frutas.",
                        "Colócalas en la licuadora junto con la leche o el yogur y el hielo.",
                        "Licúa durante 1 minuto hasta obtener una mezcla suave.",
                        "Endulza si hace falta y sirve de inmediato.",
                    ),
                ),
                en = RecipeText(
                    title = "Creamy smoothie",
                    extraIngredients = listOf(
                        "Ice (optional)",
                        "Sugar or honey to taste (optional)",
                    ),
                    steps = listOf(
                        "Peel and chop the fruit.",
                        "Put it in the blender with the milk or yogurt and the ice.",
                        "Blend for 1 minute until smooth.",
                        "Sweeten if needed and serve right away.",
                    ),
                ),
                prepTimeMinutes = 5,
                difficulty = RecipeDifficulty.EASY,
            ),
            RecipeTemplate(
                id = "fried-rice",
                oilTablespoons = 2,
                category = RecipeCategory.RICE,
                imageQuery = "fried rice",
                keywords = VEGETABLES + PROTEINS + setOf("arroz", "huevo", "rice", "egg"),
                required = listOf(setOf("arroz", "rice")),
                es = RecipeText(
                    title = "Arroz salteado de aprovechamiento",
                    extraIngredients = listOf(
                        "2 cucharadas de aceite",
                        "Salsa de soya (opcional)",
                        "Sal y pimienta al gusto",
                    ),
                    steps = listOf(
                        "Cocina el arroz en agua con sal si todavía no está cocido.",
                        "Pica los vegetales y la proteína en trozos pequeños.",
                        "Saltéalos en una sartén grande con el aceite durante 5 minutos.",
                        "Agrega el arroz, mezcla bien y cocina 3 minutos más.",
                        "Sazona con salsa de soya, sal y pimienta, y sirve caliente.",
                    ),
                ),
                en = RecipeText(
                    title = "Use-it-up fried rice",
                    extraIngredients = listOf(
                        "2 tablespoons of oil",
                        "Soy sauce (optional)",
                        "Salt and pepper to taste",
                    ),
                    steps = listOf(
                        "Cook the rice in salted water if it isn't cooked yet.",
                        "Chop the vegetables and the protein into small pieces.",
                        "Stir-fry them in a large pan with the oil for 5 minutes.",
                        "Add the rice, mix well and cook for 3 more minutes.",
                        "Season with soy sauce, salt and pepper, and serve hot.",
                    ),
                ),
                prepTimeMinutes = 30,
                difficulty = RecipeDifficulty.MEDIUM,
            ),
            RecipeTemplate(
                id = "tomato-pasta",
                oilTablespoons = 2,
                category = RecipeCategory.PASTA,
                imageQuery = "pasta tomato sauce",
                keywords = VEGETABLES + setOf(
                    "pasta", "espagueti", "spaghetti", "queso", "ajo", "albahaca", "cheese", "garlic", "basil",
                ),
                required = listOf(setOf("pasta", "espagueti", "spaghetti")),
                es = RecipeText(
                    title = "Pasta con salsa de tomate casera",
                    extraIngredients = listOf(
                        "2 cucharadas de aceite",
                        "Sal y pimienta al gusto",
                    ),
                    steps = listOf(
                        "Cocina la pasta en abundante agua con sal hasta que esté al dente.",
                        "Mientras tanto, pica los vegetales y sofríelos en el aceite durante 8 minutos.",
                        "Si usas tomate, cocínalo hasta que se deshaga y forme una salsa.",
                        "Mezcla la pasta escurrida con la salsa y sirve con queso si tienes.",
                    ),
                ),
                en = RecipeText(
                    title = "Pasta with homemade tomato sauce",
                    extraIngredients = listOf(
                        "2 tablespoons of oil",
                        "Salt and pepper to taste",
                    ),
                    steps = listOf(
                        "Cook the pasta in plenty of salted water until al dente.",
                        "Meanwhile, chop the vegetables and sauté them in the oil for 8 minutes.",
                        "If you use tomato, cook it until it breaks down into a sauce.",
                        "Mix the drained pasta with the sauce and serve with cheese if you have some.",
                    ),
                ),
                prepTimeMinutes = 25,
                difficulty = RecipeDifficulty.EASY,
            ),
            RecipeTemplate(
                id = "toasted-sandwich",
                oilTablespoons = 1,
                category = RecipeCategory.SANDWICH,
                imageQuery = "toasted sandwich",
                keywords = setOf(
                    "pan", "queso", "jamon", "tomate", "aguacate", "huevo", "lechuga", "pollo",
                    "bread", "cheese", "ham", "tomato", "avocado", "egg", "lettuce", "chicken",
                ),
                required = listOf(setOf("pan", "bread")),
                es = RecipeText(
                    title = "Sándwich tostado",
                    extraIngredients = listOf("Mantequilla (opcional)"),
                    steps = listOf(
                        "Corta el pan y unta un poco de mantequilla por fuera.",
                        "Arma el sándwich con los ingredientes que tengas.",
                        "Tuesta en una sartén a fuego medio 3 minutos por lado, hasta que dore.",
                        "Corta por la mitad y sirve caliente.",
                    ),
                ),
                en = RecipeText(
                    title = "Toasted sandwich",
                    extraIngredients = listOf("Butter (optional)"),
                    steps = listOf(
                        "Cut the bread and spread a little butter on the outside.",
                        "Build the sandwich with the ingredients you have.",
                        "Toast in a pan over medium heat for 3 minutes per side, until golden.",
                        "Cut in half and serve hot.",
                    ),
                ),
                prepTimeMinutes = 10,
                difficulty = RecipeDifficulty.EASY,
            ),
            RecipeTemplate(
                id = "home-stew",
                oilTablespoons = 2,
                category = RecipeCategory.SOUP,
                imageQuery = "vegetable stew",
                keywords = VEGETABLES + PROTEINS,
                required = listOf(PROTEINS),
                es = RecipeText(
                    title = "Guiso casero con verduras",
                    extraIngredients = listOf(
                        "2 cucharadas de aceite",
                        "1 taza de agua o caldo",
                        "Sal, pimienta y comino al gusto",
                    ),
                    steps = listOf(
                        "Corta la proteína y las verduras en trozos medianos.",
                        "Dora la proteína en una olla con el aceite durante 5 minutos.",
                        "Agrega las verduras, el agua o el caldo y los condimentos.",
                        "Tapa y cocina a fuego bajo unos 30 minutos, hasta que todo esté tierno.",
                        "Rectifica la sazón y sirve caliente.",
                    ),
                ),
                en = RecipeText(
                    title = "Homemade stew with vegetables",
                    extraIngredients = listOf(
                        "2 tablespoons of oil",
                        "1 cup of water or broth",
                        "Salt, pepper and cumin to taste",
                    ),
                    steps = listOf(
                        "Cut the protein and the vegetables into medium pieces.",
                        "Brown the protein in a pot with the oil for 5 minutes.",
                        "Add the vegetables, the water or broth and the seasonings.",
                        "Cover and cook over low heat for about 30 minutes, until everything is tender.",
                        "Adjust the seasoning and serve hot.",
                    ),
                ),
                prepTimeMinutes = 45,
                difficulty = RecipeDifficulty.MEDIUM,
            ),
            RecipeTemplate(
                id = "potato-omelette",
                oilTablespoons = 3,
                category = RecipeCategory.EGGS,
                imageQuery = "spanish potato omelette",
                keywords = setOf("huevo", "papa", "cebolla", "pimenton", "egg", "potato", "onion", "pepper"),
                required = listOf(setOf("huevo", "egg"), setOf("papa", "potato")),
                es = RecipeText(
                    title = "Tortilla de papa",
                    extraIngredients = listOf("3 cucharadas de aceite", "Sal al gusto"),
                    steps = listOf(
                        "Pela las papas y córtalas en rodajas finas; pica la cebolla si tienes.",
                        "Fríelas a fuego medio en el aceite unos 15 minutos, hasta que estén tiernas.",
                        "Bate los huevos con sal y mézclalos con las papas escurridas.",
                        "Cuaja la mezcla en la sartén a fuego bajo 5 minutos por lado, volteándola con un plato.",
                    ),
                ),
                en = RecipeText(
                    title = "Potato omelette",
                    extraIngredients = listOf("3 tablespoons of oil", "Salt to taste"),
                    steps = listOf(
                        "Peel the potatoes and slice them thinly; chop the onion if you have one.",
                        "Fry them over medium heat in the oil for about 15 minutes, until tender.",
                        "Beat the eggs with salt and mix them with the drained potatoes.",
                        "Set the mixture in the pan over low heat, 5 minutes per side, flipping it with a plate.",
                    ),
                ),
                prepTimeMinutes = 30,
                difficulty = RecipeDifficulty.MEDIUM,
            ),
            RecipeTemplate(
                id = "french-toast",
                oilTablespoons = 1,
                category = RecipeCategory.DESSERT,
                imageQuery = "french toast",
                keywords = setOf(
                    "pan", "huevo", "leche", "banano", "fresa", "canela", "miel",
                    "bread", "egg", "milk", "banana", "strawberry", "cinnamon", "honey",
                ),
                required = listOf(setOf("pan", "bread"), setOf("huevo", "egg", "leche", "milk")),
                es = RecipeText(
                    title = "Tostadas francesas",
                    extraIngredients = listOf("1 cucharada de mantequilla", "Azúcar y canela al gusto"),
                    steps = listOf(
                        "Bate el huevo con la leche, una pizca de azúcar y canela.",
                        "Remoja las rebanadas de pan en la mezcla unos segundos por lado.",
                        "Dóralas en una sartén con mantequilla a fuego medio, 2 a 3 minutos por lado.",
                        "Sirve con fruta picada o un poco de miel por encima.",
                    ),
                ),
                en = RecipeText(
                    title = "French toast",
                    extraIngredients = listOf("1 tablespoon of butter", "Sugar and cinnamon to taste"),
                    steps = listOf(
                        "Beat the egg with the milk, a pinch of sugar and cinnamon.",
                        "Soak the bread slices in the mixture for a few seconds on each side.",
                        "Brown them in a pan with butter over medium heat, 2 to 3 minutes per side.",
                        "Serve with chopped fruit or a little honey on top.",
                    ),
                ),
                prepTimeMinutes = 15,
                difficulty = RecipeDifficulty.EASY,
            ),
            RecipeTemplate(
                id = "rice-pudding",
                category = RecipeCategory.DESSERT,
                imageQuery = "rice pudding",
                keywords = setOf("arroz", "leche", "canela", "rice", "milk", "cinnamon"),
                required = listOf(setOf("arroz", "rice"), setOf("leche", "milk")),
                es = RecipeText(
                    title = "Arroz con leche",
                    extraIngredients = listOf("4 cucharadas de azúcar", "1 rama de canela (opcional)"),
                    steps = listOf(
                        "Cocina el arroz en agua 10 minutos y escúrrelo.",
                        "Agrega la leche, el azúcar y la canela, y cocina a fuego bajo.",
                        "Revuelve con frecuencia durante 20 minutos, hasta que espese.",
                        "Sirve tibio o frío, con canela en polvo por encima.",
                    ),
                ),
                en = RecipeText(
                    title = "Rice pudding",
                    extraIngredients = listOf("4 tablespoons of sugar", "1 cinnamon stick (optional)"),
                    steps = listOf(
                        "Cook the rice in water for 10 minutes and drain it.",
                        "Add the milk, the sugar and the cinnamon, and cook over low heat.",
                        "Stir often for 20 minutes, until it thickens.",
                        "Serve warm or cold, with ground cinnamon on top.",
                    ),
                ),
                prepTimeMinutes = 35,
                difficulty = RecipeDifficulty.EASY,
            ),
            RecipeTemplate(
                id = "baked-fruit",
                category = RecipeCategory.DESSERT,
                imageQuery = "baked apples cinnamon",
                keywords = setOf(
                    "manzana", "pera", "banano", "durazno", "pina", "mango",
                    "apple", "pear", "banana", "peach", "pineapple",
                ),
                required = listOf(
                    setOf("manzana", "pera", "banano", "durazno", "pina", "mango", "apple", "pear", "banana", "peach", "pineapple"),
                ),
                es = RecipeText(
                    title = "Fruta asada con canela",
                    extraIngredients = listOf("1 cucharada de azúcar o miel", "Canela al gusto"),
                    steps = listOf(
                        "Lava la fruta y córtala en mitades o trozos grandes, sin semillas.",
                        "Colócala en una fuente, espolvorea azúcar y canela.",
                        "Hornea a 180 °C unos 20 minutos, o cocínala tapada en una sartén 10 minutos.",
                        "Sirve tibia, sola o con yogur.",
                    ),
                ),
                en = RecipeText(
                    title = "Baked fruit with cinnamon",
                    extraIngredients = listOf("1 tablespoon of sugar or honey", "Cinnamon to taste"),
                    steps = listOf(
                        "Wash the fruit and cut it into halves or large pieces, without seeds.",
                        "Place it in a dish and sprinkle with sugar and cinnamon.",
                        "Bake at 180 °C for about 20 minutes, or cook it covered in a pan for 10 minutes.",
                        "Serve warm, on its own or with yogurt.",
                    ),
                ),
                prepTimeMinutes = 25,
                difficulty = RecipeDifficulty.EASY,
            ),
            RecipeTemplate(
                id = "fresh-salad",
                oilTablespoons = 2,
                category = RecipeCategory.SALAD,
                imageQuery = "fresh garden salad",
                keywords = setOf(
                    "lechuga", "tomate", "pepino", "zanahoria", "aguacate", "cebolla", "repollo", "espinaca",
                    "maiz", "queso", "atun", "pollo", "huevo",
                    "lettuce", "tomato", "cucumber", "carrot", "avocado", "onion", "cabbage", "spinach",
                    "corn", "cheese", "tuna", "chicken", "egg",
                ),
                required = listOf(
                    setOf("lechuga", "tomate", "pepino", "repollo", "espinaca", "lettuce", "tomato", "cucumber", "cabbage", "spinach"),
                ),
                es = RecipeText(
                    title = "Ensalada fresca de la nevera",
                    extraIngredients = listOf("2 cucharadas de aceite", "Jugo de limón o vinagre", "Sal y pimienta al gusto"),
                    steps = listOf(
                        "Lava y seca bien las hojas y los vegetales.",
                        "Corta todo en trozos del mismo tamaño y mézclalo en un bol.",
                        "Agrega la proteína que tengas (atún, pollo, huevo o queso).",
                        "Aliña con aceite, limón, sal y pimienta justo antes de servir.",
                    ),
                ),
                en = RecipeText(
                    title = "Fresh fridge salad",
                    extraIngredients = listOf("2 tablespoons of oil", "Lemon juice or vinegar", "Salt and pepper to taste"),
                    steps = listOf(
                        "Wash and dry the leaves and vegetables well.",
                        "Cut everything into pieces of the same size and mix in a bowl.",
                        "Add whatever protein you have (tuna, chicken, egg or cheese).",
                        "Dress with oil, lemon, salt and pepper just before serving.",
                    ),
                ),
                prepTimeMinutes = 10,
                difficulty = RecipeDifficulty.EASY,
            ),
            RecipeTemplate(
                id = "grilled-protein",
                oilTablespoons = 1,
                category = RecipeCategory.MEAT,
                imageQuery = "grilled chicken vegetables",
                keywords = PROTEINS + VEGETABLES,
                required = listOf(PROTEINS),
                es = RecipeText(
                    title = "Proteína a la plancha con verduras",
                    extraIngredients = listOf("1 cucharada de aceite", "Ajo, sal y pimienta al gusto"),
                    steps = listOf(
                        "Seca la proteína y sazónala con sal, pimienta y ajo.",
                        "Cocínala en una sartén caliente con el aceite, 4 a 6 minutos por lado.",
                        "En la misma sartén saltea las verduras cortadas 5 minutos.",
                        "Deja reposar la proteína 3 minutos, córtala y sírvela con las verduras.",
                    ),
                ),
                en = RecipeText(
                    title = "Grilled protein with vegetables",
                    extraIngredients = listOf("1 tablespoon of oil", "Garlic, salt and pepper to taste"),
                    steps = listOf(
                        "Pat the protein dry and season it with salt, pepper and garlic.",
                        "Cook it in a hot pan with the oil, 4 to 6 minutes per side.",
                        "In the same pan, sauté the chopped vegetables for 5 minutes.",
                        "Let the protein rest for 3 minutes, slice it and serve with the vegetables.",
                    ),
                ),
                prepTimeMinutes = 25,
                difficulty = RecipeDifficulty.EASY,
            ),
            RecipeTemplate(
                id = "quesadillas",
                oilTablespoons = 1,
                category = RecipeCategory.SANDWICH,
                imageQuery = "cheese quesadilla",
                keywords = setOf(
                    "tortilla", "arepa", "queso", "pollo", "jamon", "champinon", "cebolla", "pimenton", "maiz", "frijol",
                    "cheese", "chicken", "ham", "mushroom", "onion", "pepper", "corn", "bean",
                ),
                required = listOf(setOf("tortilla", "arepa"), setOf("queso", "cheese")),
                es = RecipeText(
                    title = "Quesadillas rápidas",
                    extraIngredients = listOf("1 cucharada de aceite o mantequilla"),
                    steps = listOf(
                        "Pica el relleno que tengas (pollo, jamón, champiñones, cebolla).",
                        "Pon queso y relleno sobre media tortilla y dóblala.",
                        "Dórala en una sartén con un poco de aceite, 2 minutos por lado, hasta que el queso se derrita.",
                        "Corta en triángulos y sirve caliente.",
                    ),
                ),
                en = RecipeText(
                    title = "Quick quesadillas",
                    extraIngredients = listOf("1 tablespoon of oil or butter"),
                    steps = listOf(
                        "Chop whatever filling you have (chicken, ham, mushrooms, onion).",
                        "Put cheese and filling on half a tortilla and fold it.",
                        "Brown it in a pan with a little oil, 2 minutes per side, until the cheese melts.",
                        "Cut into wedges and serve hot.",
                    ),
                ),
                prepTimeMinutes = 15,
                difficulty = RecipeDifficulty.EASY,
            ),
            RecipeTemplate(
                id = "yogurt-parfait",
                category = RecipeCategory.DESSERT,
                imageQuery = "yogurt parfait fruit",
                keywords = FRUITS + setOf("yogur", "avena", "granola", "miel", "yogurt", "yoghurt", "oat", "honey"),
                required = listOf(setOf("yogur", "yogurt", "yoghurt")),
                es = RecipeText(
                    title = "Vaso de yogur con fruta",
                    extraIngredients = listOf("Avena o granola (opcional)", "Miel al gusto"),
                    steps = listOf(
                        "Pica la fruta en trozos pequeños.",
                        "En un vaso alterna capas de yogur, fruta y avena o granola.",
                        "Termina con un hilo de miel y sirve frío.",
                    ),
                ),
                en = RecipeText(
                    title = "Yogurt and fruit parfait",
                    extraIngredients = listOf("Oats or granola (optional)", "Honey to taste"),
                    steps = listOf(
                        "Chop the fruit into small pieces.",
                        "In a glass, layer yogurt, fruit and oats or granola.",
                        "Finish with a drizzle of honey and serve cold.",
                    ),
                ),
                prepTimeMinutes = 5,
                difficulty = RecipeDifficulty.EASY,
            ),
            RecipeTemplate(
                id = "legume-stew",
                oilTablespoons = 2,
                category = RecipeCategory.SOUP,
                imageQuery = "bean stew",
                keywords = VEGETABLES + setOf(
                    "frijol", "lenteja", "garbanzo", "arveja", "platano", "bean", "lentil", "chickpea", "plantain",
                ),
                required = listOf(setOf("frijol", "lenteja", "garbanzo", "bean", "lentil", "chickpea")),
                es = RecipeText(
                    title = "Guiso de legumbres",
                    extraIngredients = listOf("2 cucharadas de aceite", "1 litro de agua", "Comino, sal y pimienta al gusto"),
                    steps = listOf(
                        "Si las legumbres son secas, déjalas en remojo desde la noche anterior.",
                        "Sofríe en el aceite la cebolla, el tomate y las verduras picadas 5 minutos.",
                        "Agrega las legumbres, el agua y los condimentos.",
                        "Cocina a fuego medio hasta que estén tiernas (20 minutos si son de lata, 1 hora si son secas).",
                        "Sirve con arroz, arepa o pan.",
                    ),
                ),
                en = RecipeText(
                    title = "Legume stew",
                    extraIngredients = listOf("2 tablespoons of oil", "1 liter of water", "Cumin, salt and pepper to taste"),
                    steps = listOf(
                        "If the legumes are dry, soak them overnight.",
                        "Sauté the onion, the tomato and the chopped vegetables in the oil for 5 minutes.",
                        "Add the legumes, the water and the seasonings.",
                        "Cook over medium heat until tender (20 minutes if canned, 1 hour if dry).",
                        "Serve with rice, arepa or bread.",
                    ),
                ),
                prepTimeMinutes = 40,
                difficulty = RecipeDifficulty.MEDIUM,
            ),
        )
    }
}

/** The ingredient as shown in the recipe: well-known foods are named in [language] ("huevos" -> "eggs"). */
private fun PantryItem.asIngredientLine(language: AppLanguage): String =
    "${name.localizedFoodName(language)} (${amountText(language)})"

/** Lowercase words of [this] without accents, e.g. "Pimentón rojo" -> {pimenton, rojo}. */
private fun String.tokens(): Set<String> =
    lowercase()
        .replace('á', 'a')
        .replace('é', 'e')
        .replace('í', 'i')
        .replace('ó', 'o')
        .replace('ú', 'u')
        .replace('ü', 'u')
        .replace('ñ', 'n')
        .split(Regex("[^a-z0-9]+"))
        .filter { it.isNotEmpty() }
        .toSet()

/** True if this word equals a keyword or its plural ("manzanas", "tomatoes", "blueberries"). */
private fun String.matchesAny(keywords: Set<String>): Boolean =
    keywords.any { keyword ->
        this == keyword ||
            this == keyword + "s" ||
            this == keyword + "es" ||
            (keyword.endsWith("y") && this == keyword.dropLast(1) + "ies")
    }
