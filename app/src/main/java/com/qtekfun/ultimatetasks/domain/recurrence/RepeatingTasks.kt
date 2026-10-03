// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.recurrence

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The dates and rule of a repeating task once its current occurrence is done. */
data class NextOccurrence(val due: String?, val start: String?, val recurrence: String)

/**
 * Completing a repeating task moves it to its next occurrence (SPEC §9: the same task, like
 * Nextcloud Tasks and tasks.org): due and start move by the same days, the time of day stays,
 * and COUNT counts down. Dates are the app's ISO local text (`2026-10-05` or `…T09:30`).
 */
object RepeatingTasks {
    private const val DATE_LENGTH = 10

    /** Whether the app can work out the next occurrence; other rules are only shown. */
    fun understood(recurrence: String?): Boolean =
        recurrence == null || RecurrenceRules.parse(recurrence) != null

    /**
     * The next occurrence after the current one: after the due date, else the start, else
     * [today]. Null when the series ended or the rule is not understood.
     */
    fun next(due: String?, start: String?, recurrence: String, today: LocalDate): NextOccurrence? {
        val rule = RecurrenceRules.parse(recurrence) ?: return null
        val anchorText = due ?: start
        val anchor = if (anchorText ==
            null
        ) {
            today
        } else {
            LocalDate.parse(anchorText.take(DATE_LENGTH))
        }
        return Recurrences.next(rule, anchor, anchor)?.let { next ->
            val days = ChronoUnit.DAYS.between(anchor, next)
            NextOccurrence(
                due = due?.let { shift(it, days) },
                start = start?.let { shift(it, days) },
                recurrence = if (rule.count ==
                    null
                ) {
                    recurrence
                } else {
                    RecurrenceRules.format(rule.copy(count = rule.count - 1))
                }
            )
        }
    }

    private fun shift(date: String, days: Long): String =
        LocalDate.parse(date.take(DATE_LENGTH)).plusDays(days).toString() + date.drop(DATE_LENGTH)
}
