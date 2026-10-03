// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.recurrence

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Occurrences of a [RecurrenceRule] (RF-06). Works on days: a task's time of day stays as it
 * is. Dates that do not exist (31 February) are skipped, as RFC 5545 says.
 */
object Recurrences {
    /** Enough for daily rules over a few centuries; protects against rules that never match. */
    private const val MAX_PERIODS = 100_000
    private const val DAYS_IN_WEEK = 7L

    /**
     * The first occurrence strictly after [after], for a series that starts on [start] (which
     * counts as the first occurrence for COUNT), or null when the series has ended.
     */
    fun next(rule: RecurrenceRule, start: LocalDate, after: LocalDate): LocalDate? =
        (0 until MAX_PERIODS).asSequence()
            .flatMap { occurrences(rule, start, it) }
            .filter { !it.isBefore(start) }
            .takeWhile { rule.until == null || !it.isAfter(rule.until) }
            .take(rule.count ?: Int.MAX_VALUE)
            .firstOrNull { it.isAfter(after) }

    /** The occurrences in the [period]-th interval after the start, in order. */
    internal fun occurrences(rule: RecurrenceRule, start: LocalDate, period: Int): List<LocalDate> {
        val step = period.toLong() * rule.interval
        val candidates = when (rule.frequency) {
            Frequency.DAILY -> listOf(start.plusDays(step)).filter { matches(rule, it) }
            Frequency.WEEKLY -> week(rule, start, step)
            Frequency.MONTHLY -> month(rule, YearMonth.from(start).plusMonths(step), start)
            Frequency.YEARLY -> year(rule, start.year + step.toInt(), start)
        }
        return setPositions(rule, candidates.distinct().sorted())
    }

    private fun matches(rule: RecurrenceRule, date: LocalDate): Boolean =
        (rule.byMonth.isEmpty() || date.monthValue in rule.byMonth) &&
            (
                rule.byMonthDay.isEmpty() ||
                    rule.byMonthDay.any { monthDay(YearMonth.from(date), it) == date }
                ) &&
            (rule.byDay.isEmpty() || rule.byDay.any { it.day == date.dayOfWeek })

    private fun week(rule: RecurrenceRule, start: LocalDate, step: Long): List<LocalDate> {
        val first = start.with(TemporalAdjusters.previousOrSame(rule.weekStart)).plusWeeks(step)
        val days = rule.byDay.map { it.day }.ifEmpty { listOf(start.dayOfWeek) }
        return (0 until DAYS_IN_WEEK).map { first.plusDays(it) }
            .filter {
                it.dayOfWeek in days &&
                    (rule.byMonth.isEmpty() || it.monthValue in rule.byMonth)
            }
    }

    private fun month(rule: RecurrenceRule, month: YearMonth, start: LocalDate): List<LocalDate> {
        if (rule.byMonth.isNotEmpty() && month.monthValue !in rule.byMonth) return emptyList()
        val byDays = rule.byDay.flatMap { weekdays(month, it) }
        val byMonthDays = rule.byMonthDay.mapNotNull { monthDay(month, it) }
        return when {
            rule.byDay.isNotEmpty() && rule.byMonthDay.isNotEmpty() -> byDays.filter {
                it in
                    byMonthDays
            }

            rule.byDay.isNotEmpty() -> byDays

            rule.byMonthDay.isNotEmpty() -> byMonthDays

            else -> listOfNotNull(monthDay(month, start.dayOfMonth))
        }
    }

    /** A yearly rule picks its months (BYMONTH, or the start's), then days as a monthly one. */
    private fun year(rule: RecurrenceRule, year: Int, start: LocalDate): List<LocalDate> {
        val months = rule.byMonth.ifEmpty { listOf(start.monthValue) }
        return months.flatMap {
            month(rule.copy(byMonth = emptyList()), YearMonth.of(year, it), start)
        }
    }

    /** Every [weekday] of [month], or only the n-th (negative counts from the end). */
    private fun weekdays(month: YearMonth, weekday: WeekdayNum): List<LocalDate> {
        val all = (1..month.lengthOfMonth()).map { month.atDay(it) }.filter {
            it.dayOfWeek ==
                weekday.day
        }
        val ordinal = weekday.ordinal ?: return all
        return listOfNotNull(all.getOrNull(if (ordinal > 0) ordinal - 1 else all.size + ordinal))
    }

    /** Day [day] of [month]; negative counts from the end; null when the month is too short. */
    private fun monthDay(month: YearMonth, day: Int): LocalDate? {
        val index = if (day > 0) day else month.lengthOfMonth() + day + 1
        return if (index in 1..month.lengthOfMonth()) month.atDay(index) else null
    }

    /** BYSETPOS keeps the n-th candidates of each interval (`-1` is the last one). */
    private fun setPositions(rule: RecurrenceRule, candidates: List<LocalDate>): List<LocalDate> {
        if (rule.bySetPos.isEmpty()) return candidates
        return rule.bySetPos.mapNotNull { pos ->
            candidates.getOrNull(if (pos > 0) pos - 1 else candidates.size + pos)
        }.distinct().sorted()
    }
}
