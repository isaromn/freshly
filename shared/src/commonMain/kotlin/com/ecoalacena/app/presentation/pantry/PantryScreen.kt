package com.ecoalacena.app.presentation.pantry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import com.ecoalacena.app.presentation.util.formatDecimal
import com.ecoalacena.app.presentation.util.formatQuantity
import com.ecoalacena.app.presentation.util.icon
import ecoalacenakmp.shared.generated.resources.Res
import ecoalacenakmp.shared.generated.resources.logoNevera
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PantryScreen(
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PantryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnScanClick by rememberUpdatedState(onScanClick)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PantryEffect.NavigateToScan -> currentOnScanClick()
                is PantryEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    PantryContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
private fun PantryContent(
    state: PantryState,
    snackbarHostState: SnackbarHostState,
    onIntent: (PantryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AddFoodSpeedDial(
                onScanClick = { onIntent(PantryIntent.ScanClicked) },
                onAddManuallyClick = { onIntent(PantryIntent.AddItemClicked) },
            )
        },
    ) { innerPadding ->
        // One column on phones; as many 340 dp columns as fit on a computer window.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                // Room for the floating "+" button, so it never covers the last card.
                bottom = innerPadding.calculateBottomPadding() + 88.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) { PantryHeader() }

            item(key = "metric", span = { GridItemSpan(maxLineSpan) }) {
                SavedKgCard(
                    savedKg = state.savedKg,
                    expiringSoonCount = state.expiringSoonCount,
                    expiredCount = state.expiredCount,
                )
            }

            item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                CategoryFilters(
                    selectedCategory = state.selectedCategory,
                    totalItems = state.totalItems,
                    onSelect = { category -> onIntent(PantryIntent.SelectCategory(category)) },
                )
            }

            when {
                state.isLoading -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.items.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        hasFilter = state.selectedCategory != null,
                        onScanClick = { onIntent(PantryIntent.ScanClicked) },
                        onAddClick = { onIntent(PantryIntent.AddItemClicked) },
                    )
                }

                else -> items(state.items, key = { it.item.id }) { itemUi ->
                    PantryItemCard(
                        itemUi = itemUi,
                        onEdit = { onIntent(PantryIntent.EditItem(itemUi.item.id)) },
                        onConsumed = { onIntent(PantryIntent.ConsumedClicked(itemUi.item.id)) },
                        onDiscarded = { onIntent(PantryIntent.DiscardClicked(itemUi.item.id)) },
                    )
                }
            }
        }
    }

    PantryDialogs(dialog = state.dialog, onIntent = onIntent)
}

@Composable
private fun PantryDialogs(
    dialog: PantryDialog?,
    onIntent: (PantryIntent) -> Unit,
) {
    val dismiss = { onIntent(PantryIntent.DismissDialog) }

    when (dialog) {
        null -> Unit

        PantryDialog.Add -> AddPantryItemDialog(
            initialItem = null,
            onDismiss = dismiss,
            onSave = { input -> onIntent(PantryIntent.SaveItem(input)) },
        )

        is PantryDialog.Edit -> AddPantryItemDialog(
            initialItem = dialog.item,
            onDismiss = dismiss,
            onSave = { input -> onIntent(PantryIntent.SaveItem(input)) },
        )

        is PantryDialog.ConfirmConsumed -> ConfirmConsumedDialog(
            item = dialog.item,
            impactKg = dialog.impactKg,
            onConfirm = { onIntent(PantryIntent.MarkConsumed(dialog.item.id)) },
            onDismiss = dismiss,
        )

        is PantryDialog.ConfirmDiscarded -> ConfirmDiscardedDialog(
            item = dialog.item,
            onConfirm = { onIntent(PantryIntent.DiscardItem(dialog.item.id)) },
            onDismiss = dismiss,
        )
    }
}

@Composable
private fun PantryHeader(modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.logoNevera),
            contentDescription = "Logo EcoAlacena",
            modifier = Modifier.size(56.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.pantryTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = strings.pantrySubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SavedKgCard(
    savedKg: Double,
    expiringSoonCount: Int,
    expiredCount: Int,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val summary = if (expiringSoonCount + expiredCount == 0) {
        strings.allGood
    } else {
        strings.expiringSummary(expiringSoonCount, expiredCount)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Eco,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = strings.avoidedWasteTitle,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = "${formatDecimal(savedKg, 1)} kg",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CategoryFilters(
    selectedCategory: Category?,
    totalItems: Int,
    onSelect: (Category?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelect(null) },
                label = { Text(strings.filterAll(totalItems)) },
            )
        }
        items(Category.entries, key = { it.name }) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onSelect(category) },
                label = { Text(strings.categoryLabel(category)) },
                leadingIcon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}

@Composable
private fun PantryItemCard(
    itemUi: PantryItemUi,
    onEdit: () -> Unit,
    onConsumed: () -> Unit,
    onDiscarded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val item = itemUi.item

    // Tapping the card opens the form to edit the item.
    Card(
        onClick = onEdit,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.category.icon,
                        contentDescription = strings.categoryLabel(item.category),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (item.isOrganic) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Eco,
                                contentDescription = strings.organic,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Text(
                        text = "${formatQuantity(item.quantity)} ${strings.unitShort(item.unit)} · " +
                            strings.categoryLabel(item.category),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(6.dp))
                    FreshnessBadge(freshness = itemUi.freshness, daysLeft = itemUi.daysLeft)
                }
            }

            ItemActions(onConsumed = onConsumed, onDiscarded = onDiscarded)
        }
    }
}

/**
 * The two ways an item leaves the pantry, each with its own icon AND a text label:
 * "Consumed" (eaten, counts towards the impact) and "Discarded" (thrown away, does not count).
 * Each one asks for confirmation and explains what will happen.
 */
@Composable
private fun ItemActions(
    onConsumed: () -> Unit,
    onDiscarded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalButton(
            onClick = onConsumed,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = strings.actionConsumed, maxLines = 1)
        }

        OutlinedButton(
            onClick = onDiscarded,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = strings.actionDiscarded, maxLines = 1)
        }
    }
}

@Composable
private fun FreshnessBadge(
    freshness: FreshnessStatus,
    daysLeft: Int?,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val color = freshness.color()

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.16f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = freshnessLabel(strings, freshness, daysLeft),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyState(
    hasFilter: Boolean,
    onScanClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Inventory2,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )
        Text(
            text = if (hasFilter) strings.noItemsInCategory else strings.emptyPantryTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (!hasFilter) {
            Text(
                text = strings.emptyPantryHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onScanClick) {
                Text(strings.scanNow)
            }
            OutlinedButton(onClick = onAddClick) {
                Text(strings.fabAddItem)
            }
        }
    }
}

/**
 * Traffic light taken from the theme roles, so it adapts to light and dark mode:
 * primary = fine, tertiary = warning, error = problem, outline = neutral.
 */
@Composable
private fun FreshnessStatus.color(): Color =
    when (this) {
        FreshnessStatus.FRESH -> MaterialTheme.colorScheme.primary
        FreshnessStatus.EXPIRING_SOON -> MaterialTheme.colorScheme.tertiary
        FreshnessStatus.EXPIRED -> MaterialTheme.colorScheme.error
        FreshnessStatus.NO_DATE -> MaterialTheme.colorScheme.outline
    }

/** "Fresh · 5 days", "Expiring soon · 2 days", "Expired · 1 day ago" or "No date", in the user's language. */
private fun freshnessLabel(strings: AppStrings, freshness: FreshnessStatus, daysLeft: Int?): String {
    val days = daysLeft ?: 0
    return when (freshness) {
        FreshnessStatus.FRESH -> strings.freshLabel(days)
        FreshnessStatus.EXPIRING_SOON -> strings.expiringSoonLabel(days)
        FreshnessStatus.EXPIRED -> strings.expiredLabel(-days)
        FreshnessStatus.NO_DATE -> strings.noDateLabel
    }
}
