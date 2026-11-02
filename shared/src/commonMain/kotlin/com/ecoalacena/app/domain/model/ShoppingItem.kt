package com.ecoalacena.app.domain.model

/** Why a product is on the shopping list. */
enum class ShoppingReason {
    /** It was eaten recently. */
    CONSUMED,

    /** It expired recently (thrown away, or still in the pantry past its date). */
    EXPIRED,

    /** The user added it by hand. */
    MANUAL,
}

/**
 * One line of the shopping list. Suggestions carry the amount that was used up; products added by
 * hand have no amount.
 */
data class ShoppingItem(
    /** Stable key: the id of a manual item, or `suggestion:<normalised name>`. */
    val id: String,
    val name: String,
    val quantity: Double?,
    val unit: String?,
    val category: Category,
    val reason: ShoppingReason,
) {
    val isSuggestion: Boolean
        get() = reason != ShoppingReason.MANUAL
}

/** A product the user typed into the shopping list. */
data class ManualShoppingItem(
    val id: String,
    val name: String,
    val addedAt: Long,
)

/** Same product, whatever the case, accents or extra spaces ("Leche  entera" = "leche entera"). */
fun String.shoppingKey(): String = trim().replace(Regex("\\s+"), " ").lowercase()
