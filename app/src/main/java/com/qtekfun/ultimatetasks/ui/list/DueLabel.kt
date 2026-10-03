// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.task.DueInfo
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** "Today, 9:30", "Tomorrow", "Mon 6 Oct" (RF-03), in the user's language. */
@Composable
fun dueLabel(due: DueInfo, today: LocalDate): String {
    val day = when (due.date) {
        today -> stringResource(R.string.due_today)
        today.plusDays(1) -> stringResource(R.string.due_tomorrow)
        today.minusDays(1) -> stringResource(R.string.due_yesterday)
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(due.date)
    }
    if (!due.hasTime) return day
    val time = DateTimeFormatter.ofLocalizedTime(
        FormatStyle.SHORT
    ).format(due.instant.atZone(ZoneId.systemDefault()))
    return stringResource(R.string.due_day_time, day, time)
}
