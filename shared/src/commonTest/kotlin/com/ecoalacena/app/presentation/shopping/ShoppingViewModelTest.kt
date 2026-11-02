package com.ecoalacena.app.presentation.shopping

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.i18n.EnglishStrings
import com.ecoalacena.app.core.i18n.SpanishStrings
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.ShoppingItem
import com.ecoalacena.app.domain.model.ShoppingReason
import com.ecoalacena.app.domain.usecase.GetShoppingListUseCase
import com.ecoalacena.app.platform.ShareResult
import com.ecoalacena.app.testing.FakePantryRepository
import com.ecoalacena.app.testing.FakeShoppingRepository
import com.ecoalacena.app.testing.TEST_NOW
import com.ecoalacena.app.testing.testItem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingViewModelTest {

    private val pantry = FakePantryRepository()
    private val shared = mutableListOf<String>()
    private var shareResult = ShareResult.SHARED

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun typingAndAddingPutsTheProductOnTheListAndClearsTheField() {
        val viewModel = viewModel()

        viewModel.onIntent(ShoppingIntent.NewItemNameChanged("Café"))
        viewModel.onIntent(ShoppingIntent.AddItem)

        assertEquals(listOf("Café"), viewModel.state.value.items.map { it.name })
        assertEquals("", viewModel.state.value.newItemName)
    }

    @Test
    fun sharingSendsTheFormattedList() = runTest {
        pantry.markConsumed(
            testItem("milk", "Leche", expiresInDays = 5, quantity = 2.0, unit = "l", category = Category.DAIRY),
            TEST_NOW - MILLIS_PER_DAY,
        )
        val viewModel = viewModel()

        viewModel.onIntent(ShoppingIntent.Share)

        assertEquals(listOf("🛒 *Freshly - Lista de compras:*\n- Leche (2 L)"), shared)
    }

    @Test
    fun copyingOnDesktopShowsAMessage() = runTest {
        shareResult = ShareResult.COPIED_TO_CLIPBOARD
        val viewModel = viewModel()
        viewModel.onIntent(ShoppingIntent.NewItemNameChanged("Café"))
        viewModel.onIntent(ShoppingIntent.AddItem)

        viewModel.onIntent(ShoppingIntent.Share)

        assertEquals(ShoppingEffect.ShowMessage(SpanishStrings.copiedToClipboard), viewModel.effects.first())
    }

    @Test
    fun anEmptyListIsNotShared() {
        val viewModel = viewModel()

        viewModel.onIntent(ShoppingIntent.Share)

        assertTrue(shared.isEmpty())
    }

    private fun viewModel(): ShoppingViewModel {
        val useCase = GetShoppingListUseCase(pantry, FakeShoppingRepository(), nowMillis = { TEST_NOW })
        return ShoppingViewModel(
            shoppingList = useCase,
            shareText = { text ->
                shared += text
                shareResult
            },
            strings = { SpanishStrings },
        )
    }
}

class ShoppingListFormatterTest {

    @Test
    fun formatsTheListWithBulletsAndAmounts() {
        val items = listOf(
            ShoppingItem("1", "Milk", 2.0, "l", Category.DAIRY, ShoppingReason.CONSUMED),
            ShoppingItem("2", "Tomatoes", 1.0, "kg", Category.FRUITS_VEGETABLES, ShoppingReason.EXPIRED),
            ShoppingItem("3", "Whole Wheat Bread", null, null, Category.OTHER, ShoppingReason.MANUAL),
        )

        assertEquals(
            "🛒 *Freshly - Shopping List:*\n- Milk (2 L)\n- Tomatoes (1 kg)\n- Whole Wheat Bread",
            formatShoppingList(items, EnglishStrings),
        )
    }

    @Test
    fun anEmptyListIsJustTheTitle() {
        assertNull(formatShoppingList(emptyList(), EnglishStrings).lineSequence().drop(1).firstOrNull())
    }
}
