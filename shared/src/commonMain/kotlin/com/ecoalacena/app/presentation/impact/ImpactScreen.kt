package com.ecoalacena.app.presentation.impact

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecoalacena.app.core.i18n.AppLanguage
import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.domain.usecase.CalculateImpactUseCase
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import com.ecoalacena.app.presentation.util.formatDecimal
import com.ecoalacena.app.presentation.util.formatGrouped
import com.ecoalacena.app.presentation.util.groupSeparator
import org.koin.compose.viewmodel.koinViewModel
import ecoalacenakmp.shared.generated.resources.Res
import ecoalacenakmp.shared.generated.resources.logoNevera
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.painterResource


private const val COUNTER_DURATION_MILLIS = 1_200

/**
 * Which pair of theme roles tints a metric card. Every tone is a `container` + `onContainer` pair,
 * which Material guarantees to be readable in both light and dark mode.
 */
private enum class MetricTone {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    ERROR,
}

private class ToneColors(
    val container: Color,
    val content: Color,
)

@Composable
private fun MetricTone.colors(): ToneColors {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        MetricTone.PRIMARY -> ToneColors(scheme.primaryContainer, scheme.onPrimaryContainer)
        MetricTone.SECONDARY -> ToneColors(scheme.secondaryContainer, scheme.onSecondaryContainer)
        MetricTone.TERTIARY -> ToneColors(scheme.tertiaryContainer, scheme.onTertiaryContainer)
        MetricTone.ERROR -> ToneColors(scheme.errorContainer, scheme.onErrorContainer)
    }
}

@Composable
fun ImpactScreen(
    modifier: Modifier = Modifier,
    viewModel: ImpactViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ImpactContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
private fun ImpactContent(
    state: ImpactState,
    onIntent: (ImpactIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val metrics = state.metrics

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                ImpactHeader()
            }

            if (!state.isLoading && metrics.savedKg <= 0.0) {
                item(key = "hint", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyImpactHint()
                }
            }

            item(key = "money") {
                MoneyCard(
                    currency = state.currency,
                    copAmount = metrics.moneySavedCop,
                    usdAmount = metrics.moneySavedUsd,
                    onCurrencySelected = { currency -> onIntent(ImpactIntent.SelectCurrency(currency)) },
                )
            }

            item(key = "kg") {
                val animatedKg = rememberAnimatedValue(metrics.savedKg)
                MetricCard(
                    icon = Icons.Filled.Scale,
                    title = strings.foodTitle,
                    value = formatDecimal(animatedKg, 1),
                    unit = "kg",
                    explanation = strings.explainFood,
                    tone = MetricTone.PRIMARY,
                )
            }

            item(key = "co2") {
                val animatedCo2 = rememberAnimatedValue(metrics.co2AvoidedKg)
                MetricCard(
                    icon = Icons.Filled.Cloud,
                    title = strings.co2Title,
                    value = formatDecimal(animatedCo2, 1),
                    unit = strings.co2Unit,
                    explanation = strings.explainCo2(
                        formatDecimal(CalculateImpactUseCase.CO2E_KG_PER_KG_FOOD, 1),
                    ),
                    tone = MetricTone.SECONDARY,
                )
            }

            item(key = "equivalences", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = strings.equivalences,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            item(key = "trees") {
                val animatedTrees = rememberAnimatedValue(metrics.treesEquivalent)
                MetricCard(
                    icon = Icons.Filled.Park,
                    title = strings.treesTitle,
                    value = formatDecimal(animatedTrees, 1),
                    unit = strings.treesUnit,
                    explanation = strings.explainTrees(
                        formatDecimal(CalculateImpactUseCase.CO2_KG_ABSORBED_PER_TREE_YEAR, 0),
                    ),
                    tone = MetricTone.PRIMARY,
                )
            }

            item(key = "km") {
                val animatedKm = rememberAnimatedValue(metrics.carKmEquivalent)
                MetricCard(
                    icon = Icons.Filled.DirectionsCar,
                    title = strings.kmTitle,
                    value = formatDecimal(animatedKm, 0),
                    unit = strings.kmUnit,
                    explanation = strings.explainKm(
                        formatDecimal(CalculateImpactUseCase.CO2_KG_PER_CAR_KM, 2),
                    ),
                    tone = MetricTone.ERROR,
                )
            }

            item(key = "disclaimer", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = strings.impactDisclaimer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ImpactHeader(modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(Res.drawable.logoNevera),
            contentDescription = "Logo Freshly",
            modifier = Modifier.size(72.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.impactTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, // Corregido aquí
            )
            Text(
                text = strings.impactSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
@Composable
private fun EmptyImpactHint(modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = strings.impactEmptyTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = strings.impactEmptyHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun MoneyCard(
    currency: ImpactCurrency,
    copAmount: Double,
    usdAmount: Double,
    onCurrencySelected: (ImpactCurrency) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val separator = strings.groupSeparator()
    val tone = MetricTone.TERTIARY
    val toneColors = tone.colors()

    val animatedAmount = rememberAnimatedValue(
        if (currency == ImpactCurrency.COP) copAmount else usdAmount,
    )
    val valueText = when (currency) {
        ImpactCurrency.COP -> "\$ ${formatGrouped(animatedAmount.toLong(), separator)}"
        ImpactCurrency.USD -> "US\$ ${formatDecimal(animatedAmount, 2)}"
    }

    MetricCard(
        icon = Icons.Filled.Savings,
        title = strings.moneyTitle,
        value = valueText,
        unit = currency.name,
        explanation = strings.explainMoney(
            pricePerKg = formatGrouped(CalculateImpactUseCase.AVERAGE_PRICE_COP_PER_KG.toLong(), separator),
            copPerUsd = formatGrouped(CalculateImpactUseCase.COP_PER_USD.toLong(), separator),
        ),
        tone = tone,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ImpactCurrency.entries.forEach { option ->
                FilterChip(
                    selected = option == currency,
                    onClick = { onCurrencySelected(option) },
                    label = { Text(option.name) },
                    // Inverted pair when selected: always readable on the tinted card.
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = toneColors.content,
                        selectedContainerColor = toneColors.content,
                        selectedLabelColor = toneColors.container,
                    ),
                )
            }
        }
    }
}

/** A tappable card: tapping it reveals how the figure is calculated. */
@Composable
private fun MetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    unit: String,
    explanation: String,
    tone: MetricTone,
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {},
) {
    val strings = LocalAppStrings.current
    val toneColors = tone.colors()
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = toneColors.container,
            contentColor = toneColors.content,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(toneColors.content.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = toneColors.content,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = toneColors.content,
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = toneColors.content,
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                color = toneColors.content.copy(alpha = 0.8f),
            )

            extraContent()

            AnimatedVisibility(visible = expanded) {
                Text(
                    text = explanation,
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = toneColors.content.copy(alpha = 0.9f),
                )
            }

            Text(
                text = if (expanded) strings.tapToHide else strings.tapForDetails,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = toneColors.content,
            )
        }
    }
}

/** Counts from 0 up to [target] when the card first appears, and animates to any later change. */
@Composable
private fun rememberAnimatedValue(target: Double): Double {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(target) {
        animatable.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMillis = COUNTER_DURATION_MILLIS),
        )
    }

    return animatable.value.toDouble()
}
