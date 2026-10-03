// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.domain.recurrence.Frequency
import com.qtekfun.ultimatetasks.domain.recurrence.RecurrenceRules
import com.qtekfun.ultimatetasks.domain.task.DueEdits
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Date and Time switches, early reminder and repetition (RF-05, RF-06, RF-10). */
@Composable
fun DateSection(task: TaskEntity, editable: Boolean, viewModel: TaskDetailViewModel) {
    val zone = ZoneId.systemDefault()
    val date = DueEdits.date(task)
    val time = DueEdits.time(task)
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    DetailCard {
        SwitchRow(
            label = stringResource(R.string.field_date),
            value = date?.let { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(it) },
            checked = date != null,
            enabled = editable,
            onChecked = { on ->
                if (on) pickDate = true else viewModel.edit { DueEdits.withDate(it, null, zone) }
            }
        )
        HorizontalDivider(Modifier.padding(start = 16.dp))
        SwitchRow(
            label = stringResource(R.string.field_time),
            value = time?.let { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(it) },
            checked = time != null,
            enabled = editable,
            onChecked = { on ->
                if (on) pickTime = true else viewModel.edit { DueEdits.withTime(it, null, zone) }
            }
        )
        HorizontalDivider(Modifier.padding(start = 16.dp))
        ReminderRow(
            task.reminderBefore,
            enabled = editable && date != null
        ) { seconds -> viewModel.edit { it.copy(reminderBefore = seconds) } }
        HorizontalDivider(Modifier.padding(start = 16.dp))
        ValueRow(stringResource(R.string.field_repeat), recurrenceLabel(task.recurrence))
    }
    if (pickDate) {
        DueDatePicker(
            date,
            onPick = { picked -> viewModel.edit { DueEdits.withDate(it, picked, zone) } },
            onDismiss = { pickDate = false }
        )
    }
    if (pickTime) {
        DueTimePicker(
            time,
            onPick = { picked -> viewModel.edit { DueEdits.withTime(it, picked, zone) } },
            onDismiss = { pickTime = false }
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    value: String?,
    checked: Boolean,
    enabled: Boolean,
    /** Also called with true when the value is tapped, to change it. */
    onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.weight(
                1f
            ).clickable(
                enabled = enabled && value != null,
                role = Role.Button
            ) { onChecked(true) }
        ) {
            Text(label, Modifier.weight(1f))
            value?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

@Composable
fun ValueRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** None, or 5 min to 1 week before the due date (RF-10). */
@Composable
private fun ReminderRow(seconds: Long?, enabled: Boolean, onPick: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().heightIn(
            min = 56.dp
        ).clickable(enabled = enabled, role = Role.DropdownList) {
            open =
                true
        }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.field_early_reminder), Modifier.weight(1f))
        Text(reminderLabel(seconds), color = MaterialTheme.colorScheme.onSurfaceVariant)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (listOf<Long?>(null) + EARLY_REMINDERS).forEach { option ->
                DropdownMenuItem(
                    text = { Text(reminderLabel(option)) },
                    onClick = {
                        open = false
                        onPick(option)
                    }
                )
            }
        }
    }
}

private const val MINUTE = 60L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR
private const val WEEK = 7 * DAY
private const val FIVE_MINUTES = 5 * MINUTE
private const val QUARTER_HOUR = 15 * MINUTE
private const val HALF_HOUR = 30 * MINUTE
private val EARLY_REMINDERS = listOf(FIVE_MINUTES, QUARTER_HOUR, HALF_HOUR, HOUR, DAY, WEEK)

@Composable
private fun reminderLabel(seconds: Long?): String {
    val (plural, unit) = when {
        seconds == null -> return stringResource(R.string.none)
        seconds % WEEK == 0L -> R.plurals.weeks_before to WEEK
        seconds % DAY == 0L -> R.plurals.days_before to DAY
        seconds % HOUR == 0L -> R.plurals.hours_before to HOUR
        else -> R.plurals.minutes_before to MINUTE
    }
    val count = (seconds / unit).toInt()
    return pluralStringResource(plural, count, count)
}

/** A short description until the repetition editor arrives (T18). */
@Composable
private fun recurrenceLabel(recurrence: String?): String {
    val rule = recurrence?.let(RecurrenceRules::parse)
    val simple = rule?.takeIf { it.interval == 1 && it.byDay.isEmpty() && it.byMonthDay.isEmpty() }
    return stringResource(
        when {
            recurrence == null -> R.string.repeat_never
            simple == null -> R.string.repeat_custom
            simple.frequency == Frequency.DAILY -> R.string.repeat_daily
            simple.frequency == Frequency.WEEKLY -> R.string.repeat_weekly
            simple.frequency == Frequency.MONTHLY -> R.string.repeat_monthly
            else -> R.string.repeat_yearly
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDatePicker(date: LocalDate?, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // The picker works in UTC milliseconds at midnight.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (date ?: LocalDate.now()).atStartOfDay().toInstant(
            ZoneOffset.UTC
        ).toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    ) { DatePicker(state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueTimePicker(time: LocalTime?, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val start = time ?: LocalTime.of(DEFAULT_HOUR, 0)
    val state = rememberTimePickerState(start.hour, start.minute)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state) },
        confirmButton = {
            TextButton(onClick = {
                onPick(LocalTime.of(state.hour, state.minute))
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** A new time starts at 9:00, as in Apple Reminders. */
private const val DEFAULT_HOUR = 9
