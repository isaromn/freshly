package com.ecoalacena.app.domain.usecase

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.currentEpochMillis
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.ManualShoppingItem
import com.ecoalacena.app.domain.model.PantryEvent
import com.ecoalacena.app.domain.model.PantryEventKind
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.model.ShoppingItem
import com.ecoalacena.app.domain.model.ShoppingReason
import com.ecoalacena.app.domain.model.shoppingKey
import com.ecoalacena.app.domain.repository.PantryRepository
import com.ecoalacena.app.domain.repository.ShoppingRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * The smart shopping list: what to buy again, built from what happened in the last
 * [WINDOW_DAYS] days, plus anything the user added by hand.
 *
 * A product is suggested when, in that window, it was eaten, thrown away, or it expired while still
 * in the pantry. Repeated lines of the same product are grouped into one, adding up the amounts
 * that share a unit. A suggestion is left out when:
 * - there is still a fresh one in the pantry (no need to buy it yet);
 * - the user already added it by hand;
 * - the user removed it from the list, and nothing new happened to it since.
 */
class GetShoppingListUseCase(
    private val pantryRepository: PantryRepository,
    private val shoppingRepository: ShoppingRepository,
    private val nowMillis: () -> Long = ::currentEpochMillis,
) {

    /** The list, updated whenever the pantry, its history or the manual items change. */
    operator fun invoke(): Flow<List<ShoppingItem>> {
        val since = nowMillis() - WINDOW_DAYS * MILLIS_PER_DAY

        return combine(
            pantryRepository.observeEventsSince(since),
            pantryRepository.observeAll(),
            shoppingRepository.observeManualItems(),
            shoppingRepository.observeDismissals(),
        ) { events, pantry, manual, dismissals ->
            buildList(events, pantry, manual, dismissals, now = nowMillis())
        }
    }

    /** Adds [name] by hand. Blank names and products already on the list by hand are ignored. */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun add(name: String) {
        val cleanName = name.trim().replace(WHITESPACE, " ")
        if (cleanName.isEmpty()) return

        val key = cleanName.shoppingKey()
        if (shoppingRepository.observeManualItems().first().any { it.name.shoppingKey() == key }) return

        shoppingRepository.addManualItem(
            ManualShoppingItem(id = Uuid.random().toString(), name = cleanName, addedAt = nowMillis()),
        )
    }

    /** Takes [item] off the list: a manual item is deleted, a suggestion is dismissed. */
    suspend fun remove(item: ShoppingItem) {
        if (item.isSuggestion) {
            shoppingRepository.dismissSuggestion(item.name.shoppingKey(), nowMillis())
        } else {
            shoppingRepository.removeManualItem(item.id)
        }
    }

    private fun buildList(
        events: List<PantryEvent>,
        pantry: List<PantryItem>,
        manual: List<ManualShoppingItem>,
        dismissals: Map<String, Long>,
        now: Long,
    ): List<ShoppingItem> {
        val windowStart = now - WINDOW_DAYS * MILLIS_PER_DAY

        val fromHistory = events.map { event ->
            UsedUp(
                name = event.name,
                category = event.category,
                quantity = event.quantity,
                unit = event.unit,
                // Thrown-away food went off: it is shown as expired.
                reason = if (event.kind == PantryEventKind.CONSUMED) ShoppingReason.CONSUMED else ShoppingReason.EXPIRED,
                at = event.happenedAt,
            )
        }
        val expiredInPantry = pantry.mapNotNull { item ->
            val expiredAt = item.expirationDate?.takeIf { it in windowStart until now } ?: return@mapNotNull null
            UsedUp(item.name, item.category, item.quantity, item.unit, ShoppingReason.EXPIRED, expiredAt)
        }

        val stillFresh = pantry
            .filter { item -> item.expirationDate == null || item.expirationDate >= now }
            .map { it.name.shoppingKey() }
            .toSet()
        val addedByHand = manual.map { it.name.shoppingKey() }.toSet()

        val suggestions = (fromHistory + expiredInPantry)
            .groupBy { it.name.shoppingKey() }
            .filterKeys { key -> key !in stillFresh && key !in addedByHand }
            .mapNotNull { (key, uses) ->
                val latest = uses.maxBy { it.at }
                val dismissedAt = dismissals[key]
                if (dismissedAt != null && dismissedAt >= latest.at) return@mapNotNull null

                latest.at to ShoppingItem(
                    id = "$SUGGESTION_PREFIX$key",
                    name = latest.name,
                    quantity = uses.filter { it.unit == latest.unit }.sumOf { it.quantity },
                    unit = latest.unit,
                    category = latest.category,
                    reason = latest.reason,
                )
            }
            .sortedByDescending { (at, _) -> at }
            .map { (_, item) -> item }

        val manualItems = manual
            .sortedByDescending { it.addedAt }
            .map { ShoppingItem(it.id, it.name, quantity = null, unit = null, category = Category.OTHER, ShoppingReason.MANUAL) }

        return manualItems + suggestions
    }

    /** Something that was eaten, thrown away or expired, at time [at]. */
    private class UsedUp(
        val name: String,
        val category: Category,
        val quantity: Double,
        val unit: String,
        val reason: ShoppingReason,
        val at: Long,
    )

    companion object {
        /** How far back the list looks for things to buy again. */
        const val WINDOW_DAYS = 7L

        private const val SUGGESTION_PREFIX = "suggestion:"
        private val WHITESPACE = Regex("\\s+")
    }
}
