package com.ecoalacena.app.domain.model

/** Why an item left the pantry. */
enum class PantryEventKind {
    CONSUMED,

    /** Thrown away: it expired, spoiled or went to compost. This is the food that was wasted. */
    DISCARDED,
}

/** A record of an item (or part of it) that left the pantry; it feeds the shopping list. */
data class PantryEvent(
    val name: String,
    val category: Category,
    val quantity: Double,
    val unit: String,
    val kind: PantryEventKind,
    val happenedAt: Long,
)

/** Estimated kilograms of food thrown away in these events (items whose weight cannot be estimated are skipped). */
fun List<PantryEvent>.wastedKg(): Double =
    filter { it.kind == PantryEventKind.DISCARDED }
        .sumOf { event ->
            PantryItem(
                id = "",
                name = event.name,
                quantity = event.quantity,
                unit = event.unit,
                expirationDate = null,
                isOrganic = false,
                category = event.category,
            ).estimatedWeightKg() ?: 0.0
        }
