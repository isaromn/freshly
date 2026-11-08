package com.ecoalacena.app.data.notification

import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.repository.PantryRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

/**
 * A [PantryRepository] that keeps the notifications in step with every change, whichever screen
 * makes it: the AI scanner, demo mode, manual entry, editing, recipes or the pantry buttons.
 * A notification problem never stops the food from being saved.
 */
class AlertingPantryRepository(
    private val delegate: PantryRepository,
    private val alerts: PantryAlerts,
) : PantryRepository by delegate {

    override suspend fun upsert(item: PantryItem) {
        val isNew = delegate.observeById(item.id).first() == null
        delegate.upsert(item)
        notify { alerts.onItemSaved(item, isNew) }
    }

    override suspend fun deleteById(id: String) {
        delegate.deleteById(id)
        notify { alerts.onItemRemoved(id) }
    }

    override suspend fun discard(item: PantryItem, discardedAtMillis: Long) {
        delegate.discard(item, discardedAtMillis)
        notify { alerts.onItemRemoved(item.id) }
    }

    override suspend fun markConsumed(item: PantryItem, consumedAtMillis: Long, quantity: Double): Double {
        val savedKg = delegate.markConsumed(item, consumedAtMillis, quantity)
        val left = item.quantity - quantity.coerceIn(0.0, item.quantity)
        if (left <= USED_UP_EPSILON) notify { alerts.onItemUsedUp(item) }
        return savedKg
    }

    private inline fun notify(action: () -> Unit) {
        try {
            action()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            // The pantry change is already saved; a missing alert must not undo it.
        }
    }

    private companion object {
        /** Same tolerance as the repository uses to remove an item with nothing left. */
        const val USED_UP_EPSILON = 1e-6
    }
}
