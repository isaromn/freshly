package com.ecoalacena.app.presentation.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.startOfDayUtc
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import com.ecoalacena.app.presentation.util.formatQuantity

/** Size of the small +/- buttons, so the controls fit inside a product card on a phone. */
private val SMALL_BUTTON_SIZE = 36.dp

/**
 * Quantity of a scanned product. It shows as a chip; a click turns it into a text field with
 * -/+ buttons. Every valid value is sent right away with [onQuantityChange]; the check button
 * (or the keyboard's Done key) closes the editor.
 */
@Composable
internal fun EditableQuantity(
    quantity: Double,
    unitLabel: String,
    unitCode: String,
    enabled: Boolean,
    onQuantityChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    var editing by rememberSaveable { mutableStateOf(false) }
    // Reset from the saved quantity each time the editor opens, but not while the user types.
    var text by rememberSaveable(editing) { mutableStateOf(formatQuantity(quantity)) }

    if (!editing) {
        AssistChip(
            onClick = { editing = true },
            enabled = enabled,
            label = { Text("${formatQuantity(quantity)} $unitLabel") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = strings.editQuantity,
                    modifier = Modifier.size(16.dp),
                )
            },
            modifier = modifier,
        )
        return
    }

    // Accept a comma as the decimal separator, which is what many keyboards show in Spanish.
    val parsed = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    val step = quantityStep(unitCode)

    fun applyValue(value: Double) {
        text = formatQuantity(value)
        onQuantityChange(value)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(
            onClick = { applyValue(((parsed ?: quantity) - step).coerceAtLeast(step)) },
            enabled = enabled,
            modifier = Modifier.size(SMALL_BUTTON_SIZE),
        ) {
            Icon(Icons.Filled.Remove, contentDescription = strings.lessQuantity)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { newText ->
                text = newText
                newText.trim().replace(',', '.').toDoubleOrNull()
                    ?.takeIf { it.isFinite() && it > 0.0 }
                    ?.let(onQuantityChange)
            },
            enabled = enabled,
            singleLine = true,
            isError = parsed == null,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (parsed != null) editing = false }),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.width(80.dp),
        )
        Text(text = unitLabel, style = MaterialTheme.typography.bodyMedium)
        IconButton(
            onClick = { applyValue((parsed ?: quantity) + step) },
            enabled = enabled,
            modifier = Modifier.size(SMALL_BUTTON_SIZE),
        ) {
            Icon(Icons.Filled.Add, contentDescription = strings.moreQuantity)
        }
        IconButton(
            onClick = { editing = false },
            enabled = enabled && parsed != null,
            modifier = Modifier.size(SMALL_BUTTON_SIZE),
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = strings.ok,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** How much the -/+ buttons change a quantity: grams and milliliters move in larger steps. */
private fun quantityStep(unitCode: String): Double =
    when (unitCode.lowercase()) {
        "g", "ml" -> 50.0
        "kg", "l" -> 0.5
        else -> 1.0
    }

/**
 * Expiration date of a scanned product: a click on the date opens a calendar to pick the exact
 * day, and -/+ move it one day. Days before today cannot be picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditableExpiration(
    label: String,
    expirationDate: Long?,
    now: Long,
    enabled: Boolean,
    onAdjustDays: (Int) -> Unit,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AssistChip(
            onClick = { showDatePicker = true },
            enabled = enabled,
            label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = strings.changeExpirationDate,
                    modifier = Modifier.size(18.dp),
                )
            },
            modifier = Modifier.weight(1f, fill = false),
        )
        IconButton(
            onClick = { onAdjustDays(-1) },
            enabled = enabled,
            modifier = Modifier.size(SMALL_BUTTON_SIZE),
        ) {
            Icon(Icons.Filled.Remove, contentDescription = strings.oneDayLess)
        }
        IconButton(
            onClick = { onAdjustDays(1) },
            enabled = enabled,
            modifier = Modifier.size(SMALL_BUTTON_SIZE),
        ) {
            Icon(Icons.Filled.Add, contentDescription = strings.oneDayMore)
        }
    }

    if (showDatePicker) {
        val today = startOfDayUtc(now)
        // The picker works in UTC: it needs (and returns) midnight of the chosen day.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = startOfDayUtc(expirationDate ?: now),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= today
            },
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { pickedDay ->
                            // A product expires at the END of the chosen day, so picking "today"
                            // does not mark it as expired right away.
                            onDateSelected(pickedDay + MILLIS_PER_DAY - 1)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(strings.ok)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
