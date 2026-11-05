package com.ecoalacena.app.domain.model

/** How the recipe suggestions choose their ingredients. */
enum class RecipeMode {
    /** Zero-waste: cook the few items closest to expiring, they must all be used. */
    EXPIRING_SOON,

    /** Cook with what there is: combine as many of the available items as possible. */
    AVAILABLE_PANTRY,
}
