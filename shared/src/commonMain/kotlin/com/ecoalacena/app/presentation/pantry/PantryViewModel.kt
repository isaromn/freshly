package com.ecoalacena.app.presentation.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.currentEpochMillis
import com.ecoalacena.app.core.formatDate
import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.core.i18n.currentStrings
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.domain.repository.PantryRepository
import com.ecoalacena.app.domain.usecase.EstimateExpirationDateUseCase
import com.ecoalacena.app.presentation.util.formatDecimal
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PantryViewModel(
    private val repository: PantryRepository,
    private val nowMillis: () -> Long = ::currentEpochMillis,
    /** Texts for the user's language; read each time a message is built. */
    private val strings: () -> AppStrings = ::currentStrings,
    /** Creates the id of an item added by hand. */
    private val newItemId: () -> String = { randomItemId() },
    /** Fills in the expiration date when the user does not enter one. */
    private val estimateExpiration: EstimateExpirationDateUseCase = EstimateExpirationDateUseCase(nowMillis),
) : ViewModel() {

    private val selectedCategory = MutableStateFlow<Category?>(null)
    private val activeDialog = MutableStateFlow<PantryDialog?>(null)

    private val _state = MutableStateFlow(PantryState())
    val state: StateFlow<PantryState> = _state.asStateFlow()

    private val _effects = Channel<PantryEffect>(Channel.BUFFERED)
    val effects: Flow<PantryEffect> = _effects.receiveAsFlow()

    init {
        combine(
            repository.observeAll(),
            repository.observeSavedKg(),
            selectedCategory,
            activeDialog,
        ) { items, savedKg, category, dialog ->
            buildState(items, savedKg, category, dialog)
        }
            .onEach { newState -> _state.value = newState }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: PantryIntent) {
        when (intent) {
            is PantryIntent.SelectCategory -> selectedCategory.value = intent.category
            PantryIntent.ScanClicked -> sendEffect(PantryEffect.NavigateToScan)
            is PantryIntent.ConsumedClicked -> askToConfirmConsumed(intent.itemId)
            is PantryIntent.DiscardClicked -> askToConfirmDiscard(intent.itemId)
            is PantryIntent.MarkConsumed -> markConsumed(intent.itemId)
            is PantryIntent.DiscardItem -> discardItem(intent.itemId)
            PantryIntent.AddItemClicked -> activeDialog.value = PantryDialog.Add
            is PantryIntent.EditItem -> editItem(intent.itemId)
            PantryIntent.DismissDialog -> activeDialog.value = null
            is PantryIntent.SaveItem -> saveItem(intent.input)
        }
    }

    private fun buildState(
        items: List<PantryItem>,
        savedKg: Double,
        category: Category?,
        dialog: PantryDialog?,
    ): PantryState {
        val now = nowMillis()
        val allItems = items.map { it.toUi(now) }
        val visibleItems = allItems
            .filter { category == null || it.item.category == category }
            .sortedWith(compareBy<PantryItemUi, Long?>(nullsLast<Long>()) { it.item.expirationDate })

        return PantryState(
            isLoading = false,
            items = visibleItems,
            selectedCategory = category,
            totalItems = allItems.size,
            expiringSoonCount = allItems.count { it.freshness == FreshnessStatus.EXPIRING_SOON },
            expiredCount = allItems.count { it.freshness == FreshnessStatus.EXPIRED },
            savedKg = savedKg,
            dialog = dialog,
        )
    }

    private fun findItem(itemId: String): PantryItemUi? =
        _state.value.items.firstOrNull { it.item.id == itemId }

    private fun editItem(itemId: String) {
        val ui = findItem(itemId) ?: return
        activeDialog.value = PantryDialog.Edit(ui.item)
    }

    /**
     * Opens the "consumed" confirmation. The weight that will count towards the impact is computed
     * here with the same rule the repository uses: only items that have not expired are counted.
     */
    private fun askToConfirmConsumed(itemId: String) {
        val ui = findItem(itemId) ?: return
        val impactKg = ui.item.estimatedWeightKg()
            ?.takeIf { weightKg -> weightKg > 0.0 && ui.freshness != FreshnessStatus.EXPIRED }
        activeDialog.value = PantryDialog.ConfirmConsumed(ui.item, impactKg)
    }

    private fun askToConfirmDiscard(itemId: String) {
        val ui = findItem(itemId) ?: return
        activeDialog.value = PantryDialog.ConfirmDiscarded(ui.item)
    }

    /**
     * Validates the form and writes the item to the database. `upsert` is an
     * `INSERT OR REPLACE`, so a new id inserts and an existing id updates the same row.
     * If no expiration date was entered, an estimate based on the category and name is saved.
     * The dialog stays open if the data is invalid or the write fails, so nothing typed is lost.
     */
    private fun saveItem(input: PantryItemInput) {
        val name = input.name.trim()
        val texts = strings()

        if (name.isEmpty()) {
            sendEffect(PantryEffect.ShowMessage(texts.nameRequired))
            return
        }
        if (!input.quantity.isFinite() || input.quantity <= 0.0) {
            sendEffect(PantryEffect.ShowMessage(texts.quantityInvalid))
            return
        }

        val enteredDate = input.expirationDate
        val expirationDate = enteredDate ?: estimateExpiration(name, input.category, nowMillis())

        val item = PantryItem(
            id = input.id ?: newItemId(),
            name = name,
            quantity = input.quantity,
            unit = input.unit,
            expirationDate = expirationDate,
            isOrganic = input.isOrganic,
            category = input.category,
        )

        launchSafely(error = { saveError }) {
            repository.upsert(item)
            activeDialog.value = null

            val savedTexts = strings()
            val message = if (enteredDate == null) {
                savedTexts.savedWithEstimateMessage(item.name, formatDate(expirationDate, savedTexts.language))
            } else {
                savedTexts.savedMessage(item.name)
            }
            sendEffect(PantryEffect.ShowMessage(message))
        }
    }

    /**
     * The item was eaten. The repository removes it and, when it was still fresh, stores its
     * estimated weight; the impact screen turns that total into money, CO2e and equivalences.
     */
    private fun markConsumed(itemId: String) {
        val ui = findItem(itemId) ?: return
        activeDialog.value = null

        launchSafely(error = { consumeError }) {
            repository.markConsumed(ui.item, nowMillis())

            val texts = strings()
            val weightKg = ui.item.estimatedWeightKg()
            val message = when {
                ui.freshness == FreshnessStatus.EXPIRED -> texts.consumedExpiredMessage(ui.item.name)
                weightKg != null -> texts.consumedSavedMessage(ui.item.name, formatDecimal(weightKg, 2))
                else -> texts.consumedMessage(ui.item.name)
            }
            sendEffect(PantryEffect.ShowMessage(message))
        }
    }

    /** The item was thrown away: it is removed and nothing is added to the impact. */
    private fun discardItem(itemId: String) {
        val ui = findItem(itemId) ?: return
        activeDialog.value = null

        launchSafely(error = { discardError }) {
            repository.deleteById(ui.item.id)
            sendEffect(PantryEffect.ShowMessage(strings().discardedMessage(ui.item.name)))
        }
    }

    private fun launchSafely(error: AppStrings.() -> String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                sendEffect(PantryEffect.ShowMessage(strings().error()))
            }
        }
    }

    private fun sendEffect(effect: PantryEffect) {
        _effects.trySend(effect)
    }

    private fun PantryItem.toUi(now: Long): PantryItemUi {
        val expiration = expirationDate
            ?: return PantryItemUi(this, FreshnessStatus.NO_DATE, daysLeft = null)

        val difference = expiration - now
        val daysLeft = if (difference >= 0) {
            (difference / MILLIS_PER_DAY).toInt()
        } else {
            -((-difference) / MILLIS_PER_DAY).toInt()
        }
        val freshness = when {
            difference < 0 -> FreshnessStatus.EXPIRED
            daysLeft <= EXPIRING_SOON_DAYS -> FreshnessStatus.EXPIRING_SOON
            else -> FreshnessStatus.FRESH
        }
        return PantryItemUi(this, freshness, daysLeft)
    }

    private companion object {
        /** Items that expire within this many days are shown as "expiring soon". */
        const val EXPIRING_SOON_DAYS = 3
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun randomItemId(): String = Uuid.random().toString()
