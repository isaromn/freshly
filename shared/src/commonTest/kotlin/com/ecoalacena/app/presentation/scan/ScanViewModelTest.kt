package com.ecoalacena.app.presentation.scan

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.data.remote.ai.GeminiConfig
import com.ecoalacena.app.data.remote.ai.GeminiVisionService
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.usecase.AnalyzeFoodUseCase
import com.ecoalacena.app.testing.FakePantryRepository
import com.ecoalacena.app.testing.TEST_NOW
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * The demo receipt must work with no network and no API key, so these tests use a blank key
 * and an HTTP engine that fails the test if it is ever called.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

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
    fun demoReceiptIsReadyForReviewInstantly() {
        val viewModel = viewModel()

        viewModel.onIntent(ScanIntent.LoadDemoReceipt)

        val state = viewModel.state.value
        assertEquals(ScanStatus.REVIEW, state.status)
        assertEquals(5, state.drafts.size)
        assertNull(state.errorMessage)
    }

    @Test
    fun demoReceiptHasTheFiveCommonSupermarketProducts() {
        val viewModel = viewModel()

        viewModel.onIntent(ScanIntent.LoadDemoReceipt)

        val drafts = viewModel.state.value.drafts
        assertEquals(
            listOf("Leche entera", "Manzanas", "Pechuga de pollo", "Pan tajado", "Queso campesino"),
            drafts.map { it.name },
        )
        assertEquals(
            listOf(
                Category.DAIRY,
                Category.FRUITS_VEGETABLES,
                Category.MEAT_FISH,
                Category.BAKERY,
                Category.DAIRY,
            ),
            drafts.map { it.category },
        )
    }

    @Test
    fun demoReceiptSuggestsExpirationDatesFromNow() {
        val viewModel = viewModel()

        viewModel.onIntent(ScanIntent.LoadDemoReceipt)

        val chicken = viewModel.state.value.drafts.first { it.name == "Pechuga de pollo" }
        assertEquals(TEST_NOW + 2 * MILLIS_PER_DAY, chicken.expirationDate)
    }

    @Test
    fun confirmingTheDemoReceiptSavesEveryProductAndGoesBack() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(ScanIntent.LoadDemoReceipt)

        viewModel.onIntent(ScanIntent.Confirm)

        assertEquals(5, repository.items.value.size)
        assertEquals(ScanEffect.NavigateBack, viewModel.effects.first())
        assertEquals(ScanStatus.IDLE, viewModel.state.value.status)
    }

    @Test
    fun productsRemovedBeforeConfirmingAreNotSaved() {
        val viewModel = viewModel()
        viewModel.onIntent(ScanIntent.LoadDemoReceipt)
        val removed = viewModel.state.value.drafts.first()

        viewModel.onIntent(ScanIntent.RemoveDraft(removed.id))
        viewModel.onIntent(ScanIntent.Confirm)

        assertEquals(4, repository.items.value.size)
        assertEquals(false, repository.items.value.any { it.name == removed.name })
    }

    @Test
    fun resetDiscardsTheDemoReceipt() {
        val viewModel = viewModel()
        viewModel.onIntent(ScanIntent.LoadDemoReceipt)

        viewModel.onIntent(ScanIntent.Reset)

        assertEquals(ScanStatus.IDLE, viewModel.state.value.status)
        assertEquals(0, viewModel.state.value.drafts.size)
    }

    private fun viewModel(): ScanViewModel {
        val client = HttpClient(MockEngine { error("The demo receipt must not use the network") })
        val visionService = GeminiVisionService(client, GeminiConfig(apiKey = ""))
        val analyzeFood = AnalyzeFoodUseCase(
            visionService,
            nowMillis = { TEST_NOW },
            // The demo names are language dependent: pin Spanish so the test does not depend on the machine.
            language = { AppLanguage.ES },
        )

        return ScanViewModel(
            analyzeFood = analyzeFood,
            repository = repository,
            nowMillis = { TEST_NOW },
        )
    }
}
