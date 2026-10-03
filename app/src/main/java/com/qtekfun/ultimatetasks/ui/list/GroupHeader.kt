// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.task.GroupKey
import com.qtekfun.ultimatetasks.ui.theme.ListColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** "Overdue", "Today", "Tue, 6 Oct" or a list name in its color (T16). */
@Composable
fun GroupHeader(key: GroupKey, state: TaskListState) {
    val (text, color) = when (key) {
        GroupKey.None -> "" to Color.Unspecified

        GroupKey.Overdue -> stringResource(R.string.group_overdue) to
            MaterialTheme.colorScheme.error

        is GroupKey.Day -> dayTitle(key.date) to Color.Unspecified

        is GroupKey.InList -> state.lists[key.href]?.let { it.name to ListColors.of(it.color) }
            ?: ("" to Color.Unspecified)
    }
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.fillMaxWidth().padding(
            start = 16.dp,
            top = 16.dp,
            bottom = 4.dp
        ).semantics {
            heading()
        }
    )
}

@Composable
private fun dayTitle(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.due_today)
        today.plusDays(1) -> stringResource(R.string.due_tomorrow)
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(date)
    }
}
