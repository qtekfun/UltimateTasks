// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.recurrence.Frequency
import com.qtekfun.ultimatetasks.domain.recurrence.RecurrenceRule
import com.qtekfun.ultimatetasks.domain.recurrence.RecurrenceRules
import com.qtekfun.ultimatetasks.domain.recurrence.RepeatPreset
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun presetLabel(preset: RepeatPreset): String = stringResource(
    when (preset) {
        RepeatPreset.NEVER -> R.string.repeat_never
        RepeatPreset.DAILY -> R.string.repeat_daily
        RepeatPreset.WEEKDAYS -> R.string.repeat_weekdays
        RepeatPreset.WEEKLY -> R.string.repeat_weekly
        RepeatPreset.BIWEEKLY -> R.string.repeat_biweekly
        RepeatPreset.MONTHLY -> R.string.repeat_monthly
        RepeatPreset.QUARTERLY -> R.string.repeat_quarterly
        RepeatPreset.HALF_YEARLY -> R.string.repeat_half_yearly
        RepeatPreset.YEARLY -> R.string.repeat_yearly
    }
)

/** "Weekly", or "Every 2 months, on the 3rd Friday, 5 times" for custom rules (RF-06). */
@Composable
fun repeatLabel(recurrence: String?): String {
    val preset = RepeatPreset.of(recurrence)
    val rule = recurrence?.let(RecurrenceRules::parse)
    return when {
        preset != null -> presetLabel(preset)
        rule == null -> stringResource(R.string.repeat_custom)
        else -> listOfNotNull(every(rule), on(rule), end(rule)).joinToString(", ")
    }
}

@Composable
private fun every(rule: RecurrenceRule): String = pluralStringResource(
    when (rule.frequency) {
        Frequency.DAILY -> R.plurals.every_days
        Frequency.WEEKLY -> R.plurals.every_weeks
        Frequency.MONTHLY -> R.plurals.every_months
        Frequency.YEARLY -> R.plurals.every_years
    },
    rule.interval,
    rule.interval
)

@Composable
private fun on(rule: RecurrenceRule): String? {
    val single = rule.byDay.singleOrNull()
    val ordinal = single?.ordinal ?: rule.bySetPos.singleOrNull()
    return when {
        rule.frequency == Frequency.MONTHLY && single != null && ordinal != null ->
            stringResource(
                R.string.repeat_on_nth,
                ordinalLabel(ordinal),
                dayName(single.day, TextStyle.FULL)
            )

        rule.frequency == Frequency.WEEKLY && rule.byDay.isNotEmpty() ->
            rule.byDay.joinToString(" ") { dayName(it.day, TextStyle.SHORT) }

        else -> null
    }
}

@Composable
private fun end(rule: RecurrenceRule): String? = when {
    rule.count != null -> pluralStringResource(R.plurals.repeat_times, rule.count, rule.count)

    rule.until != null -> stringResource(
        R.string.repeat_until,
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(rule.until)
    )

    else -> null
}

/** First, second, third, fourth; any other position is the last one (-1). */
private val ORDINAL_NAMES = listOf(
    R.string.ordinal_first,
    R.string.ordinal_second,
    R.string.ordinal_third,
    R.string.ordinal_fourth
)

/** The positions offered by the custom editor: 1st to 4th, and the last. */
val ORDINAL_CHOICES: List<Int> = ORDINAL_NAMES.indices.map { it + 1 } + -1

@Composable
fun ordinalLabel(ordinal: Int): String =
    stringResource(ORDINAL_NAMES.getOrNull(ordinal - 1) ?: R.string.ordinal_last)

fun dayName(day: DayOfWeek, style: TextStyle): String =
    day.getDisplayName(style, Locale.getDefault())
