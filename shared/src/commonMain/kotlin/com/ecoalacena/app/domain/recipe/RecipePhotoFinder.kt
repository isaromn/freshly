package com.ecoalacena.app.domain.recipe

/**
 * A photo of a dish and the name of its author, which must be shown next to it.
 * [creditUrl] is the author's profile page, linked from the credit when there is one.
 */
data class RecipePhoto(
    val url: String,
    val credit: String,
    val creditUrl: String? = null,
)

/**
 * Finds a photo for a recipe from a short search text. Returns `null` when there is no photo
 * (no key configured, no network, no result), so the recipe keeps its local illustration.
 */
fun interface RecipePhotoFinder {
    suspend fun findPhoto(query: String): RecipePhoto?

    companion object {
        /** Never finds a photo: every recipe uses its local illustration. */
        val None = RecipePhotoFinder { null }
    }
}
