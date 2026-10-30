package com.ecoalacena.app.presentation.impact

import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.usecase.CalculateImpactUseCase
import com.ecoalacena.app.testing.FakePantryRepository
import com.ecoalacena.app.testing.TEST_NOW
import com.ecoalacena.app.testing.testItem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ImpactViewModelTest {

    private val repository = FakePantryRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsWithZeroImpactInPesos() {
        val viewModel = ImpactViewModel(repository, CalculateImpactUseCase())

        val state = viewModel.state.value

        assertFalse(state.isLoading)
        assertEquals(0.0, state.metrics.savedKg)
        assertEquals(ImpactCurrency.COP, state.currency)
    }

    @Test
    fun metricsFollowTheSavedKilograms() {
        val viewModel = ImpactViewModel(repository, CalculateImpactUseCase())

        repository.savedKg.value = 10.0

        val metrics = viewModel.state.value.metrics
        assertEquals(10.0, metrics.savedKg)
        assertEquals(10.0 * CalculateImpactUseCase.AVERAGE_PRICE_COP_PER_KG, metrics.moneySavedCop)
        assertEquals(10.0 * CalculateImpactUseCase.CO2E_KG_PER_KG_FOOD, metrics.co2AvoidedKg)
    }

    @Test
    fun selectingACurrencyKeepsTheMetrics() {
        val viewModel = ImpactViewModel(repository, CalculateImpactUseCase())
        repository.savedKg.value = 2.0

        viewModel.onIntent(ImpactIntent.SelectCurrency(ImpactCurrency.USD))

        assertEquals(ImpactCurrency.USD, viewModel.state.value.currency)
        assertEquals(2.0, viewModel.state.value.metrics.savedKg)
    }

    @Test
    fun consumingAnItemCountedInUnitsUpdatesEveryImpactMetric() = runTest {
        // 6 dairy units x 0.25 kg = 1.5 kg
        val eggs = testItem("eggs", quantity = 6.0, unit = "unit", expiresInDays = 3, category = Category.DAIRY)
        val pantry = FakePantryRepository(listOf(eggs))
        val viewModel = ImpactViewModel(pantry, CalculateImpactUseCase())

        pantry.markConsumed(eggs, TEST_NOW)

        val metrics = viewModel.state.value.metrics
        val co2Kg = 1.5 * CalculateImpactUseCase.CO2E_KG_PER_KG_FOOD
        assertEquals(1.5, metrics.savedKg, TOLERANCE)
        assertEquals(1.5 * CalculateImpactUseCase.AVERAGE_PRICE_COP_PER_KG, metrics.moneySavedCop, TOLERANCE)
        assertEquals(co2Kg, metrics.co2AvoidedKg, TOLERANCE)
        assertEquals(co2Kg / CalculateImpactUseCase.CO2_KG_ABSORBED_PER_TREE_YEAR, metrics.treesEquivalent, TOLERANCE)
        assertEquals(co2Kg / CalculateImpactUseCase.CO2_KG_PER_CAR_KM, metrics.carKmEquivalent, TOLERANCE)
    }

    @Test
    fun consumingSeveralItemsAccumulatesTheImpact() = runTest {
        val milk = testItem("milk", quantity = 2.0, unit = "l", expiresInDays = 4)
        val bread = testItem("bread", quantity = 500.0, unit = "g", expiresInDays = 1)
        val pantry = FakePantryRepository(listOf(milk, bread))
        val viewModel = ImpactViewModel(pantry, CalculateImpactUseCase())

        pantry.markConsumed(milk, TEST_NOW)
        pantry.markConsumed(bread, TEST_NOW)

        assertEquals(2.5, viewModel.state.value.metrics.savedKg, TOLERANCE)
    }

    @Test
    fun anExpiredItemLeavesTheImpactUntouched() = runTest {
        val spoiled = testItem("spoiled", quantity = 3.0, unit = "kg", expiresInDays = -2)
        val pantry = FakePantryRepository(listOf(spoiled))
        val viewModel = ImpactViewModel(pantry, CalculateImpactUseCase())

        pantry.markConsumed(spoiled, TEST_NOW)

        assertEquals(0.0, viewModel.state.value.metrics.savedKg)
        assertEquals(0.0, viewModel.state.value.metrics.moneySavedCop)
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
