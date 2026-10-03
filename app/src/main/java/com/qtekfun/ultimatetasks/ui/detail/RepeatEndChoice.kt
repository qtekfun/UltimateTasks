// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.detail

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.recurrence.CustomRepeat
import com.qtekfun.ultimatetasks.domain.recurrence.RepeatEnd
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Ends never, after N times, or on a date. */
@Composable
fun EndChoice(repeat: CustomRepeat, onChange: (CustomRepeat) -> Unit) {
    var pickDate by remember { mutableStateOf(false) }
    Text(stringResource(R.string.repeat_end))
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(repeat.end == RepeatEnd.NEVER, onClick = {
            onChange(repeat.copy(end = RepeatEnd.NEVER))
        })
        Text(stringResource(R.string.repeat_end_never))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(repeat.end == RepeatEnd.AFTER_COUNT, onClick = {
            onChange(repeat.copy(end = RepeatEnd.AFTER_COUNT))
        })
        Stepper(repeat.count) { onChange(repeat.copy(end = RepeatEnd.AFTER_COUNT, count = it)) }
        Text(pluralStringResource(R.plurals.unit_times, repeat.count))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(repeat.end == RepeatEnd.ON_DATE, onClick = { pickDate = true })
        Text(stringResource(R.string.repeat_end_on))
        TextButton(onClick = { pickDate = true }) {
            Text(
                repeat.until?.let {
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(it)
                }
                    ?: "…"
            )
        }
    }
    if (pickDate) {
        DueDatePicker(
            date = repeat.until ?: LocalDate.now().plusMonths(1),
            onPick = { onChange(repeat.copy(end = RepeatEnd.ON_DATE, until = it)) },
            onDismiss = { pickDate = false }
        )
    }
}
