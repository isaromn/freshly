package com.ecoalacena.app.presentation.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.core.i18n.currentStrings
import com.ecoalacena.app.domain.usecase.GetShoppingListUseCase
import com.ecoalacena.app.platform.ShareResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(
    private val shoppingList: GetShoppingListUseCase,
    /** `ShareService.shareText` of the platform; a plain function so tests can fake it. */
    private val shareText: (String) -> ShareResult,
    /** Texts for the user's language; read each time a message is built. */
    private val strings: () -> AppStrings = ::currentStrings,
) : ViewModel() {

    private val newItemName = MutableStateFlow("")

    val state: StateFlow<ShoppingState> = combine(shoppingList(), newItemName) { items, typed ->
        ShoppingState(isLoading = false, items = items, newItemName = typed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ShoppingState(),
    )

    private val _effects = Channel<ShoppingEffect>(Channel.BUFFERED)
    val effects: Flow<ShoppingEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: ShoppingIntent) {
        when (intent) {
            is ShoppingIntent.NewItemNameChanged -> newItemName.value = intent.name
            ShoppingIntent.AddItem -> addItem()
            is ShoppingIntent.RemoveItem -> viewModelScope.launch { shoppingList.remove(intent.item) }
            ShoppingIntent.Share -> share()
        }
    }

    private fun addItem() {
        val name = newItemName.value
        if (name.isBlank()) return
        newItemName.value = ""
        viewModelScope.launch { shoppingList.add(name) }
    }

    private fun share() {
        val items = state.value.items
        if (items.isEmpty()) return

        val texts = strings()
        when (shareText(formatShoppingList(items, texts))) {
            ShareResult.SHARED -> Unit
            ShareResult.COPIED_TO_CLIPBOARD -> _effects.trySend(ShoppingEffect.ShowMessage(texts.copiedToClipboard))
            ShareResult.FAILED -> _effects.trySend(ShoppingEffect.ShowMessage(texts.shareError))
        }
    }
}
