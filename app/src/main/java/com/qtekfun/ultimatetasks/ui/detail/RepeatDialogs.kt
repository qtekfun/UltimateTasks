// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.recurrence.CustomRepeat
import com.qtekfun.ultimatetasks.domain.recurrence.Frequency
import com.qtekfun.ultimatetasks.domain.recurrence.MonthlyMode
import com.qtekfun.ultimatetasks.domain.recurrence.RepeatEnd
import com.qtekfun.ultimatetasks.domain.recurrence.RepeatPreset
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

/** Quick choices, plus Custom… (RF-06). [onPick] gets the RRULE, or null for Never. */
@Composable
fun RepeatDialog(
    current: String?,
    onPick: (String?) -> Unit,
    onCustom: () -> Unit,
    onDismiss: () -> Unit
) {
    val selected = RepeatPreset.of(current)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.field_repeat)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                RepeatPreset.entries.forEach { preset ->
                    RadioRow(presetLabel(preset), selected == preset) { onPick(preset.rule) }
                }
                RadioRow(
                    stringResource(R.string.repeat_custom_ellipsis),
                    selected == null,
                    onCustom
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(
            min = 48.dp
        ).selectable(selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.weight(1f))
    }
}

/** Every N days, weeks (on some days), months (on a day, or the 3rd Friday) or years. */
@Composable
fun CustomRepeatDialog(
    start: CustomRepeat,
    anchor: LocalDate,
    onSave: (CustomRepeat) -> Unit,
    onDismiss: () -> Unit
) {
    var repeat by remember { mutableStateOf(start) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.repeat_custom)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IntervalRow(repeat) { repeat = it }
                when (repeat.frequency) {
                    Frequency.WEEKLY -> WeekdayChips(repeat) { repeat = it }
                    Frequency.MONTHLY -> MonthlyChoice(repeat, anchor) { repeat = it }
                    else -> Unit
                }
                HorizontalDivider()
                EndChoice(repeat) { repeat = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(repeat) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun IntervalRow(repeat: CustomRepeat, onChange: (CustomRepeat) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.repeat_every), Modifier.weight(1f))
        Stepper(repeat.interval) { onChange(repeat.copy(interval = it)) }
        Choice(
            label = frequencyName(repeat.frequency, repeat.interval),
            options = Frequency.entries,
            name = { frequencyName(it, repeat.interval) }
        ) { onChange(repeat.copy(frequency = it)) }
    }
}

@Composable
private fun frequencyName(frequency: Frequency, count: Int): String = pluralStringResource(
    when (frequency) {
        Frequency.DAILY -> R.plurals.unit_days
        Frequency.WEEKLY -> R.plurals.unit_weeks
        Frequency.MONTHLY -> R.plurals.unit_months
        Frequency.YEARLY -> R.plurals.unit_years
    },
    count
)

/** − N + buttons. */
@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onChange(value - 1) }, enabled = value > 1) { Text("−") }
        Text("$value", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { onChange(value + 1) }) { Text("+") }
    }
}

/** A text button that opens a menu of [options]. */
@Composable
fun <T> Choice(
    label: String,
    options: List<T>,
    name: @Composable (T) -> String,
    onPick: (T) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) {
        Text(label)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(name(option)) },
                    onClick = {
                        open = false
                        onPick(option)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayChips(repeat: CustomRepeat, onChange: (CustomRepeat) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DayOfWeek.entries.forEach { day ->
            val on = day in repeat.weekdays
            FilterChip(
                selected = on,
                onClick = {
                    onChange(
                        repeat.copy(
                            weekdays = if (on) {
                                repeat.weekdays - day
                            } else {
                                repeat.weekdays +
                                    day
                            }
                        )
                    )
                },
                label = { Text(dayName(day, TextStyle.SHORT)) }
            )
        }
    }
}

/** "On day 15" or "On the [third] [Friday]" (the request that started this editor). */
@Composable
private fun MonthlyChoice(
    repeat: CustomRepeat,
    anchor: LocalDate,
    onChange: (CustomRepeat) -> Unit
) {
    RadioRow(
        stringResource(R.string.repeat_on_day, anchor.dayOfMonth),
        repeat.monthlyMode == MonthlyMode.DAY_OF_MONTH
    ) {
        onChange(repeat.copy(monthlyMode = MonthlyMode.DAY_OF_MONTH))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = repeat.monthlyMode == MonthlyMode.WEEKDAY_OF_MONTH,
            onClick = { onChange(repeat.copy(monthlyMode = MonthlyMode.WEEKDAY_OF_MONTH)) }
        )
        Text(stringResource(R.string.repeat_on_the))
        Choice(ordinalLabel(repeat.ordinal), ORDINAL_CHOICES, { ordinalLabel(it) }) {
            onChange(repeat.copy(monthlyMode = MonthlyMode.WEEKDAY_OF_MONTH, ordinal = it))
        }
        Choice(dayName(repeat.weekday, TextStyle.FULL), DayOfWeek.entries, {
            dayName(it, TextStyle.FULL)
        }) {
            onChange(repeat.copy(monthlyMode = MonthlyMode.WEEKDAY_OF_MONTH, weekday = it))
        }
    }
}
