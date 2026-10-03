// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.recurrence

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RecurrencesTest {
    /** The next [n] occurrences after [start], which is the first one. */
    private fun series(rule: String, start: String, n: Int = 4): List<String> {
        val parsed = RecurrenceRules.parse(rule)!!
        var date = LocalDate.parse(start)
        return (1..n).mapNotNull {
            Recurrences.next(parsed, LocalDate.parse(start), date)?.also { date = it }?.toString()
        }
    }

    @Test
    fun `daily, every other day, and filtered by month, day and weekday`() {
        assertEquals(listOf("2026-10-04", "2026-10-05"), series("FREQ=DAILY", "2026-10-03", 2))
        assertEquals(
            listOf("2026-10-05", "2026-10-07"),
            series("FREQ=DAILY;INTERVAL=2", "2026-10-03", 2)
        )
        assertEquals(
            listOf("2026-10-05", "2026-10-07"),
            series("FREQ=DAILY;BYDAY=MO,WE", "2026-10-03", 2)
        )
        assertEquals(
            listOf("2026-11-01", "2026-12-01"),
            series("FREQ=DAILY;BYMONTHDAY=1", "2026-10-03", 2)
        )
        assertEquals(
            listOf("2027-01-01", "2027-01-02"),
            series("FREQ=DAILY;BYMONTH=1", "2026-10-03", 2)
        )
    }

    @Test
    fun `weekly on the start day or on given days, from the right week start`() {
        // Saturday 3 October 2026.
        assertEquals(listOf("2026-10-10", "2026-10-17"), series("FREQ=WEEKLY", "2026-10-03", 2))
        // Thursday: the Tuesday of its own week is before it and does not count.
        assertEquals(
            listOf("2026-10-06", "2026-10-08", "2026-10-13"),
            series("FREQ=WEEKLY;BYDAY=TU,TH", "2026-10-01", 3)
        )
        assertEquals(
            listOf("2026-10-17", "2026-10-31"),
            series("FREQ=WEEKLY;INTERVAL=2", "2026-10-03", 2)
        )
        // A week starting on Sunday puts Sunday 4th in the next week of a biweekly rule.
        assertEquals(
            listOf("2026-10-11"),
            series("FREQ=WEEKLY;INTERVAL=2;BYDAY=SA,SU;WKST=SU", "2026-10-03", 1)
        )
        assertEquals(
            listOf("2026-10-04"),
            series("FREQ=WEEKLY;INTERVAL=2;BYDAY=SA,SU", "2026-10-03", 1)
        )
        assertEquals(listOf("2027-01-02"), series("FREQ=WEEKLY;BYMONTH=1", "2026-10-03", 1))
    }

    @Test
    fun `monthly by day of the month skips months too short`() {
        assertEquals(listOf("2027-03-31", "2027-05-31"), series("FREQ=MONTHLY", "2027-01-31", 2))
        assertEquals(
            listOf("2026-11-30", "2026-12-31"),
            series("FREQ=MONTHLY;BYMONTHDAY=-1", "2026-10-31", 2)
        )
        assertEquals(
            listOf("2026-10-15", "2026-11-01"),
            series("FREQ=MONTHLY;BYMONTHDAY=1,15", "2026-10-01", 2)
        )
        assertEquals(listOf("2027-01-03"), series("FREQ=MONTHLY;BYMONTH=1", "2026-10-03", 1))
    }

    @Test
    fun `monthly by weekday`() {
        assertEquals(
            listOf("2026-11-10", "2026-12-08"),
            series("FREQ=MONTHLY;BYDAY=2TU", "2026-10-13", 2)
        )
        assertEquals(
            listOf("2026-11-27", "2026-12-25"),
            series("FREQ=MONTHLY;BYDAY=-1FR", "2026-10-30", 2)
        )
        // The last Saturday, as Nextcloud and Apple write it.
        assertEquals(
            listOf("2026-10-31", "2026-11-28"),
            series("FREQ=MONTHLY;BYDAY=SA;BYSETPOS=-1", "2026-09-26", 2)
        )
        assertEquals(
            listOf("2026-10-03", "2026-11-07"),
            series("FREQ=MONTHLY;BYDAY=SA;BYSETPOS=1", "2026-09-05", 2)
        )
        // A fifth Monday only in some months.
        assertEquals(
            listOf("2026-11-30", "2027-03-29"),
            series("FREQ=MONTHLY;BYDAY=5MO", "2026-08-31", 2)
        )
        // Friday the 13th.
        assertEquals(
            listOf("2026-03-13", "2026-11-13"),
            series("FREQ=MONTHLY;BYDAY=FR;BYMONTHDAY=13", "2026-02-13", 2)
        )
        assertEquals(
            listOf("2026-10-07"),
            series("FREQ=WEEKLY;BYDAY=MO,WE;BYSETPOS=2,9", "2026-10-03", 1)
        )
    }

    @Test
    fun `yearly keeps the date, 29 February waits for leap years`() {
        assertEquals(listOf("2027-10-03", "2028-10-03"), series("FREQ=YEARLY", "2026-10-03", 2))
        assertEquals(listOf("2032-02-29"), series("FREQ=YEARLY", "2028-02-29", 1))
        assertEquals(
            listOf("2027-01-01", "2027-07-01"),
            series("FREQ=YEARLY;BYMONTH=1,7;BYMONTHDAY=1", "2026-10-03", 2)
        )
        assertEquals(
            listOf("2026-11-26"),
            series("FREQ=YEARLY;BYMONTH=11;BYDAY=4TH", "2025-11-27", 1)
        )
    }

    @Test
    fun `count and until end the series`() {
        assertEquals(listOf("2026-10-04", "2026-10-05"), series("FREQ=DAILY;COUNT=3", "2026-10-03"))
        assertEquals(listOf("2026-10-04"), series("FREQ=DAILY;UNTIL=20261004", "2026-10-03"))
    }

    @Test
    fun `invalid days and rules that never match end too`() {
        assertEquals(emptyList<String>(), series("FREQ=MONTHLY;BYMONTHDAY=-40", "2026-10-03", 1))
        assertNull(
            Recurrences.next(
                RecurrenceRules.parse("FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=30")!!,
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-01")
            )
        )
    }
}
