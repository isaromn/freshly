package com.ecoalacena.app.domain.usecase

import com.ecoalacena.app.core.currentEpochMillis
import com.ecoalacena.app.domain.model.ImpactMetrics
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.repository.PantryRepository

/** How much of a pantry item a finished recipe used. */
data class IngredientUse(
    val item: PantryItem,
    val quantity: Double,
)

/**
 * A recipe was cooked: takes the ingredients it used out of the pantry and works out the impact
 * of this meal alone (food saved, money and CO2e), for the celebration screen.
 *
 * Each ingredient is discounted with `PantryRepository.markConsumed`, so the "waste avoided"
 * total grows and the impact dashboard, which observes it, updates on its own.
 */
class ConsumeRecipeIngredientsUseCase(
    private val repository: PantryRepository,
    private val calculateImpact: CalculateImpactUseCase = CalculateImpactUseCase(),
    private val nowMillis: () -> Long = ::currentEpochMillis,
) {

    /** @return the impact of the discounted ingredients (only fresh food with a known weight counts). */
    suspend operator fun invoke(uses: List<IngredientUse>): ImpactMetrics {
        val now = nowMillis()
        val savedKg = uses
            .filter { it.quantity > 0.0 }
            .sumOf { use -> repository.markConsumed(use.item, now, use.quantity) }
        return calculateImpact(savedKg)
    }
}
