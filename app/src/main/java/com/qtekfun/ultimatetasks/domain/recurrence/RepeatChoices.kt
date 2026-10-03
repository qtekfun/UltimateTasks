// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.recurrence

import java.time.DayOfWeek
import java.time.LocalDate

/** The quick choices of the Repeat menu (RF-06), as Apple and Nextcloud Tasks write them. */
enum class RepeatPreset(val rule: String?) {
    NEVER(null),
    DAILY("FREQ=DAILY"),
    WEEKDAYS("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"),
    WEEKLY("FREQ=WEEKLY"),
    BIWEEKLY("FREQ=WEEKLY;INTERVAL=2"),
    MONTHLY("FREQ=MONTHLY"),
    QUARTERLY("FREQ=MONTHLY;INTERVAL=3"),
    HALF_YEARLY("FREQ=MONTHLY;INTERVAL=6"),
    YEARLY("FREQ=YEARLY");

    companion object {
        /** The preset a rule is, written in any order or case; null for custom rules. */
        fun of(rule: String?): RepeatPreset? = if (rule == null) {
            NEVER
        } else {
            val parsed = RecurrenceRules.parse(rule)
            entries.firstOrNull {
                it.rule != null && parsed != null &&
                    RecurrenceRules.parse(it.rule) == parsed
            }
        }
    }
}

/** How a custom monthly rule picks its day: the same day number, or "the 3rd Friday". */
enum class MonthlyMode { DAY_OF_MONTH, WEEKDAY_OF_MONTH }

enum class RepeatEnd { NEVER, ON_DATE, AFTER_COUNT }

/**
 * The custom repetition editor (RF-06): every N days, weeks on some weekdays, months on a day
 * or on the n-th (or last, -1) weekday, or years; ending never, on a date or after N times.
 */
data class CustomRepeat(
    val frequency: Frequency = Frequency.WEEKLY,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val monthlyMode: MonthlyMode = MonthlyMode.DAY_OF_MONTH,
    val ordinal: Int = 1,
    val weekday: DayOfWeek = DayOfWeek.MONDAY,
    val end: RepeatEnd = RepeatEnd.NEVER,
    val until: LocalDate? = null,
    val count: Int = DEFAULT_COUNT
) {
    /** The rule; weekly days default to [anchor]'s weekday, as a plain weekly rule does. */
    fun toRule(anchor: LocalDate): RecurrenceRule = RecurrenceRule(
        frequency = frequency,
        interval = interval.coerceAtLeast(1),
        byDay = when {
            frequency == Frequency.WEEKLY -> weekdays.ifEmpty {
                setOf(anchor.dayOfWeek)
            }.sorted().map { WeekdayNum(it) }

            frequency == Frequency.MONTHLY && monthlyMode == MonthlyMode.WEEKDAY_OF_MONTH -> listOf(
                WeekdayNum(weekday, ordinal)
            )

            else -> emptyList()
        },
        count = count.coerceAtLeast(1).takeIf { end == RepeatEnd.AFTER_COUNT },
        until = until.takeIf { end == RepeatEnd.ON_DATE }
    )

    companion object {
        const val DEFAULT_COUNT = 10
        private const val WEEKS_IN_MONTH = 4
        private const val DAYS_IN_WEEK = 7

        /** The editor for [rule], or one that starts from [anchor] (the task's date). */
        fun from(rule: RecurrenceRule?, anchor: LocalDate): CustomRepeat {
            val nth = (anchor.dayOfMonth - 1) / DAYS_IN_WEEK + 1
            val start = CustomRepeat(
                weekdays = setOf(anchor.dayOfWeek),
                ordinal = if (nth > WEEKS_IN_MONTH) -1 else nth,
                weekday = anchor.dayOfWeek
            )
            if (rule == null) return start
            val single = rule.byDay.singleOrNull()
            // "The 3rd Friday" is one weekday with a position: `3FR`, or `FR` with `BYSETPOS=3`.
            val position = if (single ==
                null
            ) {
                null
            } else {
                single.ordinal ?: rule.bySetPos.singleOrNull()
            }
            return start.copy(
                frequency = rule.frequency,
                interval = rule.interval,
                weekdays = rule.byDay.map { it.day }.toSet().ifEmpty { start.weekdays },
                monthlyMode = if (position ==
                    null
                ) {
                    MonthlyMode.DAY_OF_MONTH
                } else {
                    MonthlyMode.WEEKDAY_OF_MONTH
                },
                ordinal = position ?: start.ordinal,
                weekday = if (single == null) start.weekday else single.day,
                end = when {
                    rule.count != null -> RepeatEnd.AFTER_COUNT
                    rule.until != null -> RepeatEnd.ON_DATE
                    else -> RepeatEnd.NEVER
                },
                until = rule.until,
                count = rule.count ?: DEFAULT_COUNT
            )
        }
    }
}
