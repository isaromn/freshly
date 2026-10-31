package com.ecoalacena.app.presentation.pantry

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.formatDate
import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.core.i18n.EnglishStrings
import com.ecoalacena.app.core.i18n.SpanishStrings
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.repository.PantryRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class PantryViewModelTest {

    @BeforeTest
    fun setUp() {
        // viewModelScope runs on Dispatchers.Main, which does not exist in plain unit tests.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val fresh = item("fresh", expiresInDays = 10, category = Category.DAIRY)
    private val soon = item("soon", expiresInDays = 2, category = Category.FRUITS_VEGETABLES)
    private val expired = item("expired", expiresInDays = -2, category = Category.MEAT_FISH)
    private val noDate = item("noDate", expiresInDays = null, category = Category.PANTRY_STAPLE)

    @Test
    fun classifiesFreshnessAndCountsUrgentItems() {
        val viewModel = viewModelWith(fresh, soon, expired, noDate)

        val state = viewModel.state.value
        val statuses = state.items.associate { it.item.id to it.freshness }

        assertEquals(FreshnessStatus.FRESH, statuses["fresh"])
        assertEquals(FreshnessStatus.EXPIRING_SOON, statuses["soon"])
        assertEquals(FreshnessStatus.EXPIRED, statuses["expired"])
        assertEquals(FreshnessStatus.NO_DATE, statuses["noDate"])
        assertEquals(4, state.totalItems)
        assertEquals(1, state.expiringSoonCount)
        assertEquals(1, state.expiredCount)
    }

    @Test
    fun sortsMostUrgentFirstAndItemsWithoutDateLast() {
        val viewModel = viewModelWith(fresh, noDate, soon, expired)

        val ids = viewModel.state.value.items.map { it.item.id }

        assertEquals(listOf("expired", "soon", "fresh", "noDate"), ids)
    }

    @Test
    fun computesDaysLeftForFutureAndPastDates() {
        val viewModel = viewModelWith(soon, expired)

        val daysLeft = viewModel.state.value.items.associate { it.item.id to it.daysLeft }

        assertEquals(2, daysLeft["soon"])
        assertEquals(-2, daysLeft["expired"])
    }

    @Test
    fun categoryFilterNarrowsVisibleItemsButNotTheTotal() {
        val viewModel = viewModelWith(fresh, soon, expired, noDate)

        viewModel.onIntent(PantryIntent.SelectCategory(Category.DAIRY))

        val state = viewModel.state.value
        assertEquals(Category.DAIRY, state.selectedCategory)
        assertEquals(listOf("fresh"), state.items.map { it.item.id })
        assertEquals(4, state.totalItems)

        viewModel.onIntent(PantryIntent.SelectCategory(null))

        assertEquals(4, viewModel.state.value.items.size)
    }

    @Test
    fun consumingAFreshItemAddsItsWeightToSavedKg() = runTest {
        val repository = FakePantryRepository(listOf(soon))
        val viewModel = viewModel(repository)

        viewModel.onIntent(PantryIntent.MarkConsumed("soon"))

        assertEquals(1.0, viewModel.state.value.savedKg)
        assertTrue(viewModel.state.value.items.isEmpty())
        val effect = assertIs<PantryEffect.ShowMessage>(viewModel.effects.first())
        assertTrue("Evitaste" in effect.message)
    }

    @Test
    fun consumingAnExpiredItemDoesNotCountAsSaved() {
        val repository = FakePantryRepository(listOf(expired))
        val viewModel = viewModel(repository)

        viewModel.onIntent(PantryIntent.MarkConsumed("expired"))

        assertEquals(0.0, viewModel.state.value.savedKg)
        assertTrue(viewModel.state.value.items.isEmpty())
    }

    @Test
    fun consumedClickOpensAConfirmationThatCountsAFreshItemTowardsTheImpact() {
        val viewModel = viewModelWith(soon)

        viewModel.onIntent(PantryIntent.ConsumedClicked("soon"))

        // Nothing is removed until the user confirms.
        assertEquals(PantryDialog.ConfirmConsumed(soon, impactKg = 1.0), viewModel.state.value.dialog)
        assertEquals(listOf("soon"), viewModel.state.value.items.map { it.item.id })
        assertEquals(0.0, viewModel.state.value.savedKg)
    }

    @Test
    fun consumedConfirmationForAnExpiredItemDoesNotCountTowardsTheImpact() {
        val viewModel = viewModelWith(expired)

        viewModel.onIntent(PantryIntent.ConsumedClicked("expired"))

        assertEquals(PantryDialog.ConfirmConsumed(expired, impactKg = null), viewModel.state.value.dialog)
    }

    @Test
    fun confirmingConsumedClosesTheDialogAndAddsTheWeightToTheImpact() = runTest {
        val viewModel = viewModelWith(fresh)
        viewModel.onIntent(PantryIntent.ConsumedClicked("fresh"))

        viewModel.onIntent(PantryIntent.MarkConsumed("fresh"))

        assertNull(viewModel.state.value.dialog)
        assertTrue(viewModel.state.value.items.isEmpty())
        assertEquals(1.0, viewModel.state.value.savedKg)
        assertIs<PantryEffect.ShowMessage>(viewModel.effects.first())
    }

    @Test
    fun itemsCountedByUnitAlsoAddToTheImpact() {
        val apples = PantryItem(
            id = "apples",
            name = "Manzanas",
            quantity = 4.0,
            unit = "unit",
            expirationDate = NOW + 5 * MILLIS_PER_DAY,
            isOrganic = false,
            category = Category.FRUITS_VEGETABLES,
        )
        val viewModel = viewModelWith(apples)

        viewModel.onIntent(PantryIntent.MarkConsumed("apples"))

        // 4 fruit units x 0.15 kg each
        assertEquals(0.6, viewModel.state.value.savedKg, 1e-9)
    }

    @Test
    fun discardClickOpensAConfirmationWithoutRemovingAnything() {
        val viewModel = viewModelWith(fresh, soon)

        viewModel.onIntent(PantryIntent.DiscardClicked("fresh"))

        assertEquals(PantryDialog.ConfirmDiscarded(fresh), viewModel.state.value.dialog)
        assertEquals(2, viewModel.state.value.items.size)
    }

    @Test
    fun discardingRemovesTheItemAndNeverCountsTowardsTheImpact() = runTest {
        val viewModel = viewModelWith(fresh, soon)
        viewModel.onIntent(PantryIntent.DiscardClicked("fresh"))

        viewModel.onIntent(PantryIntent.DiscardItem("fresh"))

        assertEquals(listOf("soon"), viewModel.state.value.items.map { it.item.id })
        assertEquals(0.0, viewModel.state.value.savedKg)
        assertNull(viewModel.state.value.dialog)
        val effect = assertIs<PantryEffect.ShowMessage>(viewModel.effects.first())
        assertEquals(SpanishStrings.discardedMessage("fresh"), effect.message)
    }

    @Test
    fun scanClickEmitsNavigationEffect() = runTest {
        val viewModel = viewModelWith()

        viewModel.onIntent(PantryIntent.ScanClicked)

        assertEquals(PantryEffect.NavigateToScan, viewModel.effects.first())
    }

    // ---- Manual entry form ----

    @Test
    fun addItemOpensTheFormAndDismissClosesIt() {
        val viewModel = viewModelWith()
        assertNull(viewModel.state.value.dialog)

        viewModel.onIntent(PantryIntent.AddItemClicked)
        assertEquals(PantryDialog.Add, viewModel.state.value.dialog)

        viewModel.onIntent(PantryIntent.DismissDialog)
        assertNull(viewModel.state.value.dialog)
    }

    @Test
    fun savingANewItemInsertsItClosesTheFormAndConfirms() = runTest {
        val viewModel = viewModelWith()
        viewModel.onIntent(PantryIntent.AddItemClicked)

        viewModel.onIntent(
            PantryIntent.SaveItem(
                PantryItemInput(
                    id = null,
                    name = "  Arroz  ",
                    category = Category.PANTRY_STAPLE,
                    quantity = 2.5,
                    unit = "kg",
                    expirationDate = NOW + 30 * MILLIS_PER_DAY,
                    isOrganic = true,
                ),
            ),
        )

        val saved = viewModel.state.value.items.single().item
        assertEquals("new-id", saved.id)
        assertEquals("Arroz", saved.name)
        assertEquals(Category.PANTRY_STAPLE, saved.category)
        assertEquals(2.5, saved.quantity)
        assertEquals("kg", saved.unit)
        assertEquals(NOW + 30 * MILLIS_PER_DAY, saved.expirationDate)
        assertTrue(saved.isOrganic)
        assertNull(viewModel.state.value.dialog)
        val effect = assertIs<PantryEffect.ShowMessage>(viewModel.effects.first())
        assertEquals(SpanishStrings.savedMessage("Arroz"), effect.message)
    }

    @Test
    fun editingAnItemKeepsItsIdAndReplacesItsData() {
        val viewModel = viewModelWith(soon)

        viewModel.onIntent(PantryIntent.EditItem("soon"))
        assertEquals(PantryDialog.Edit(soon), viewModel.state.value.dialog)

        viewModel.onIntent(
            PantryIntent.SaveItem(
                PantryItemInput(
                    id = "soon",
                    name = "Manzanas verdes",
                    category = Category.FRUITS_VEGETABLES,
                    quantity = 3.0,
                    unit = "unit",
                    expirationDate = null,
                    isOrganic = false,
                ),
            ),
        )

        val items = viewModel.state.value.items
        assertEquals(1, items.size)
        assertEquals("soon", items.single().item.id)
        assertEquals("Manzanas verdes", items.single().item.name)
        assertEquals(3.0, items.single().item.quantity)
        // No date was entered, so the estimate for fruit and vegetables (7 days) is saved.
        assertEquals(NOW + 7 * MILLIS_PER_DAY, items.single().item.expirationDate)
        assertEquals(FreshnessStatus.FRESH, items.single().freshness)
        assertNull(viewModel.state.value.dialog)
    }

    @Test
    fun invalidFormDataIsNotSavedAndKeepsTheFormOpen() = runTest {
        val viewModel = viewModelWith()
        viewModel.onIntent(PantryIntent.AddItemClicked)

        viewModel.onIntent(PantryIntent.SaveItem(input(name = "   ")))
        assertEquals(SpanishStrings.nameRequired, (viewModel.effects.first() as PantryEffect.ShowMessage).message)

        viewModel.onIntent(PantryIntent.SaveItem(input(name = "Leche", quantity = 0.0)))
        assertEquals(SpanishStrings.quantityInvalid, (viewModel.effects.first() as PantryEffect.ShowMessage).message)

        assertTrue(viewModel.state.value.items.isEmpty())
        assertEquals(PantryDialog.Add, viewModel.state.value.dialog)
    }

    @Test
    fun anItemSavedWithoutADateGetsAnEstimateBasedOnItsCategory() = runTest {
        val viewModel = viewModelWith()

        viewModel.onIntent(PantryIntent.SaveItem(input(name = "Leche", category = Category.DAIRY)))

        val expected = NOW + 14 * MILLIS_PER_DAY
        assertEquals(expected, viewModel.state.value.items.single().item.expirationDate)
        val effect = assertIs<PantryEffect.ShowMessage>(viewModel.effects.first())
        assertEquals(
            SpanishStrings.savedWithEstimateMessage("Leche", formatDate(expected, AppLanguage.ES)),
            effect.message,
        )
    }

    @Test
    fun theNameRefinesTheEstimateWhenTheCategoryIsTheGenericOne() = runTest {
        val viewModel = viewModelWith()

        // Saved as "Other", but the name says these are eggs, which last 30 days.
        viewModel.onIntent(PantryIntent.SaveItem(input(name = "Huevos")))

        assertEquals(NOW + 30 * MILLIS_PER_DAY, viewModel.state.value.items.single().item.expirationDate)
    }

    @Test
    fun aDateTypedByTheUserIsNeverReplacedByAnEstimate() = runTest {
        val viewModel = viewModelWith()
        val typed = NOW + 3 * MILLIS_PER_DAY

        viewModel.onIntent(
            PantryIntent.SaveItem(input(name = "Leche", category = Category.DAIRY, expirationDate = typed)),
        )

        assertEquals(typed, viewModel.state.value.items.single().item.expirationDate)
        val effect = assertIs<PantryEffect.ShowMessage>(viewModel.effects.first())
        assertEquals(SpanishStrings.savedMessage("Leche"), effect.message)
    }

    @Test
    fun messagesFollowTheLanguageOfTheStrings() = runTest {
        val viewModel = PantryViewModel(
            repository = FakePantryRepository(),
            nowMillis = { NOW },
            strings = { EnglishStrings },
            newItemId = { "new-id" },
        )

        viewModel.onIntent(PantryIntent.SaveItem(input(name = "Rice", expirationDate = NOW + MILLIS_PER_DAY)))

        val effect = assertIs<PantryEffect.ShowMessage>(viewModel.effects.first())
        assertEquals(EnglishStrings.savedMessage("Rice"), effect.message)
    }

    private fun viewModel(repository: PantryRepository) =
        PantryViewModel(
            repository = repository,
            nowMillis = { NOW },
            strings = { SpanishStrings },
            newItemId = { "new-id" },
        )

    private fun viewModelWith(vararg items: PantryItem) = viewModel(FakePantryRepository(items.toList()))

    private fun input(
        name: String,
        quantity: Double = 1.0,
        category: Category = Category.OTHER,
        expirationDate: Long? = null,
    ) = PantryItemInput(
        id = null,
        name = name,
        category = category,
        quantity = quantity,
        unit = "unit",
        expirationDate = expirationDate,
        isOrganic = false,
    )

    private fun item(
        id: String,
        expiresInDays: Int?,
        category: Category,
    ) = PantryItem(
        id = id,
        name = id,
        quantity = 1.0,
        unit = "kg",
        expirationDate = expiresInDays?.let { NOW + it * MILLIS_PER_DAY },
        isOrganic = false,
        category = category,
    )

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}

private class FakePantryRepository(initialItems: List<PantryItem> = emptyList()) : PantryRepository {

    private val items = MutableStateFlow(initialItems)
    private val savedKg = MutableStateFlow(0.0)

    override fun observeAll(): Flow<List<PantryItem>> = items

    override fun observeById(id: String): Flow<PantryItem?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override fun observeExpiringSoon(thresholdEpochMillis: Long): Flow<List<PantryItem>> =
        items.map { list ->
            list.filter { item ->
                item.expirationDate?.let { it <= thresholdEpochMillis } ?: false
            }
        }

    override fun observeSavedKg(): Flow<Double> = savedKg

    override suspend fun upsert(item: PantryItem) {
        items.value = items.value.filterNot { it.id == item.id } + item
    }

    override suspend fun deleteById(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }

    override suspend fun markConsumed(item: PantryItem, consumedAtMillis: Long, quantity: Double): Double {
        items.value = items.value.filterNot { it.id == item.id }

        val weightKg = item.estimatedWeightKg()
        val wasExpired = item.expirationDate?.let { it < consumedAtMillis } ?: false
        if (weightKg == null || wasExpired) return 0.0
        savedKg.value += weightKg
        return weightKg
    }
}
