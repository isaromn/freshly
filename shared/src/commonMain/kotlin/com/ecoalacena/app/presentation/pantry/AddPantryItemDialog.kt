package com.ecoalacena.app.presentation.pantry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.currentEpochMillis
import com.ecoalacena.app.core.formatDate
import com.ecoalacena.app.core.startOfDayUtc
import com.ecoalacena.app.domain.model.Category
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import com.ecoalacena.app.presentation.util.formatQuantity

/** Unit codes stored in the database; the text shown for each one comes from `AppStrings.unitName`. */
private val UNIT_CODES = listOf("kg", "g", "l", "ml", "unit", "pack")

private const val DEFAULT_UNIT = "unit"

/**
 * Modal form to add a food item by hand, or to edit [initialItem] when it is not `null`.
 *
 * It only collects and validates the data: [onSave] receives a [PantryItemInput] and the
 * caller (the ViewModel) writes it to the database. All texts follow the system language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPantryItemDialog(
    initialItem: PantryItem?,
    onDismiss: () -> Unit,
    onSave: (PantryItemInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    // `rememberSaveable` keeps what was typed through screen rotations. Only simple types are
    // stored (the category is kept by its name) so this also works outside Android.
    var name by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.name.orEmpty()) }
    var categoryName by rememberSaveable(initialItem?.id) {
        mutableStateOf((initialItem?.category ?: Category.OTHER).name)
    }
    var quantityText by rememberSaveable(initialItem?.id) {
        mutableStateOf(initialItem?.let { formatQuantity(it.quantity) } ?: "1")
    }
    var unit by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.unit ?: DEFAULT_UNIT) }
    var expirationDate by rememberSaveable(initialItem?.id) {
        mutableStateOf<Long?>(initialItem?.expirationDate)
    }
    var isOrganic by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.isOrganic ?: false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    // Errors are only shown after the first attempt to save, not while the user is still typing.
    var submitted by rememberSaveable { mutableStateOf(false) }

    val category = Category.entries.firstOrNull { it.name == categoryName } ?: Category.OTHER
    // An item that came from the AI may use a unit that is not in the list: keep it selectable.
    val unitOptions = if (unit in UNIT_CODES) UNIT_CODES else UNIT_CODES + unit

    // Accept a comma as the decimal separator, which is what many keyboards show in Spanish.
    val quantity = quantityText.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    val quantityIsValid = quantity != null
    val nameIsValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(if (initialItem == null) strings.addItemTitle else strings.editItemTitle)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.fieldName) },
                    singleLine = true,
                    isError = submitted && !nameIsValid,
                    supportingText = if (submitted && !nameIsValid) {
                        { Text(strings.nameRequired) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                DropdownField(
                    label = strings.fieldCategory,
                    selectedText = strings.categoryLabel(category),
                    options = Category.entries,
                    optionText = { strings.categoryLabel(it) },
                    onSelected = { categoryName = it.name },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text(strings.fieldQuantity) },
                        singleLine = true,
                        isError = submitted && !quantityIsValid,
                        supportingText = if (submitted && !quantityIsValid) {
                            { Text(strings.quantityInvalid) }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    DropdownField(
                        label = strings.fieldUnit,
                        selectedText = strings.unitName(unit),
                        options = unitOptions,
                        optionText = { strings.unitName(it) },
                        onSelected = { unit = it },
                        modifier = Modifier.weight(1f),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.fieldExpiration,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = expirationDate?.let { formatDate(it, strings.language) }
                                    ?: strings.noExpirationDate,
                            )
                        }
                        if (expirationDate != null) {
                            IconButton(onClick = { expirationDate = null }) {
                                Icon(Icons.Filled.Close, contentDescription = strings.clearDate)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = isOrganic,
                            role = Role.Switch,
                            onValueChange = { isOrganic = it },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = strings.fieldOrganic,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    // The whole row is the touch target, so the switch itself does not handle clicks.
                    Switch(checked = isOrganic, onCheckedChange = null)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    submitted = true
                    if (nameIsValid && quantity != null) {
                        onSave(
                            PantryItemInput(
                                id = initialItem?.id,
                                name = name.trim(),
                                category = category,
                                quantity = quantity,
                                unit = unit,
                                expirationDate = expirationDate,
                                isOrganic = isOrganic,
                            ),
                        )
                    }
                },
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )

    if (showDatePicker) {
        // The picker works in UTC: it needs (and returns) midnight of the chosen day.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = startOfDayUtc(expirationDate ?: currentEpochMillis()),
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { pickedDay ->
                            // An item expires at the END of the chosen day, so picking "today"
                            // does not mark it as expired right away.
                            expirationDate = pickedDay + MILLIS_PER_DAY - 1
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

/**
 * A read-only text field that opens a menu with [options]. The menu is a plain
 * [DropdownMenu] anchored to the field; a transparent layer on top makes the whole field tappable.
 */
@Composable
private fun <T> DropdownField(
    label: String,
    selectedText: String,
    options: List<T>,
    optionText: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClickLabel = label, role = Role.DropdownList) { expanded = true },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionText(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
