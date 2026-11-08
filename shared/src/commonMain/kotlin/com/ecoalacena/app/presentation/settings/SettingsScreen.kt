package com.ecoalacena.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecoalacena.app.domain.model.AlertLead
import com.ecoalacena.app.domain.model.NotificationSettings
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import com.ecoalacena.app.presentation.platform.rememberNotificationPermissionRequest
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val requestPermission = rememberNotificationPermissionRequest { granted ->
        viewModel.onIntent(SettingsIntent.PermissionResult(granted))
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.RequestPermission -> requestPermission()
                is SettingsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    SettingsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // One column on phones; on computer windows the alert options and the household
        // card sit side by side and use the whole width, like the pantry grid.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val twoColumns = maxWidth >= TWO_COLUMN_MIN_WIDTH

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 24.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsHeader(onBack = onBack)

                if (twoColumns) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AlertsCard(state.settings, onIntent, Modifier.weight(1f))
                        HouseholdColumn(state.settings, onIntent, Modifier.weight(1f))
                    }
                } else {
                    AlertsCard(state.settings, onIntent)
                    HouseholdColumn(state.settings, onIntent)
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    val strings = LocalAppStrings.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
        }
        Spacer(Modifier.width(4.dp))
        Column {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = strings.settingsSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The main switch, and when it is on, how early and at what time alerts arrive. */
@Composable
private fun AlertsCard(
    settings: NotificationSettings,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    SettingsCard(modifier) {
        SwitchRow(
            icon = Icons.Filled.NotificationsActive,
            title = strings.notificationsEnabled,
            hint = strings.notificationsEnabledHint,
            checked = settings.enabled,
            onCheckedChange = { onIntent(SettingsIntent.NotificationsToggled(it)) },
        )
        if (settings.enabled) {
            HorizontalDivider()
            ChoiceGroup(
                title = strings.alertLeadTitle,
                options = AlertLead.entries.reversed(),
                selected = settings.lead,
                label = strings::alertLeadLabel,
                onSelect = { onIntent(SettingsIntent.LeadSelected(it)) },
            )
            ChoiceGroup(
                title = strings.alertHourTitle,
                options = NotificationSettings.ALERT_HOURS,
                selected = settings.alertHour,
                label = { hour -> "${hour.toString().padStart(2, '0')}:00" },
                onSelect = { onIntent(SettingsIntent.AlertHourSelected(it)) },
            )
        }
    }
}

/** Household updates, the test alert and what the alerts can and cannot do. */
@Composable
private fun HouseholdColumn(
    settings: NotificationSettings,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (settings.enabled) {
            SettingsCard {
                SwitchRow(
                    icon = Icons.Filled.Groups,
                    title = strings.householdUpdates,
                    hint = strings.householdUpdatesHint,
                    checked = settings.householdUpdates,
                    onCheckedChange = { onIntent(SettingsIntent.HouseholdUpdatesToggled(it)) },
                )
            }

            FilledTonalButton(
                onClick = { onIntent(SettingsIntent.SendTestAlert) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.sendTestAlert)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = strings.notificationsDeviceNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option)) },
                )
            }
        }
    }
}

/** Windows at least this wide show the settings in two columns. */
private val TWO_COLUMN_MIN_WIDTH = 760.dp
