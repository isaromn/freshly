package com.ecoalacena.app.domain.model

/** Why an item left the pantry. */
enum class PantryEventKind {
    CONSUMED,
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
