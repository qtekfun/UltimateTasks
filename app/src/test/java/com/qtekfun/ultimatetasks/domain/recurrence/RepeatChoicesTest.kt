// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.recurrence

import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RepeatChoicesTest {
    // Friday 16 October 2026: the third Friday of the month.
    private val friday = LocalDate.parse("2026-10-16")

    private fun rule(text: String) = RecurrenceRules.parse(text)!!

    private fun format(repeat: CustomRepeat, anchor: LocalDate = friday) =
        RecurrenceRules.format(repeat.toRule(anchor))

    @Test
    fun `presets are recognised in any order or case`() {
        assertEquals(RepeatPreset.NEVER, RepeatPreset.of(null))
        assertEquals(RepeatPreset.WEEKDAYS, RepeatPreset.of("freq=weekly;byday=MO,TU,WE,TH,FR"))
        assertEquals(RepeatPreset.QUARTERLY, RepeatPreset.of("INTERVAL=3;FREQ=MONTHLY"))
        assertNull(RepeatPreset.of("FREQ=MONTHLY;BYDAY=3FR"))
        assertNull(RepeatPreset.of("FREQ=HOURLY"))
    }

    @Test
    fun `the editor starts from the task date`() {
        val start = CustomRepeat.from(null, friday)
        assertEquals(setOf(FRIDAY), start.weekdays)
        assertEquals(3 to FRIDAY, start.ordinal to start.weekday)
        // Day 30 is in the fifth week: "the last" one.
        assertEquals(-1, CustomRepeat.from(null, LocalDate.parse("2026-10-30")).ordinal)
    }

    @Test
    fun `the third Friday of every month`() {
        val repeat =
            CustomRepeat(
                frequency = Frequency.MONTHLY,
                monthlyMode = MonthlyMode.WEEKDAY_OF_MONTH,
                ordinal = 3,
                weekday = FRIDAY
            )
        assertEquals("FREQ=MONTHLY;BYDAY=3FR", format(repeat))
        assertEquals(
            repeat.copy(weekdays = setOf(FRIDAY)),
            CustomRepeat.from(rule("FREQ=MONTHLY;BYDAY=3FR"), friday)
        )
    }

    @Test
    fun `the last Saturday written with BYSETPOS reads as such`() {
        val read = CustomRepeat.from(rule("FREQ=MONTHLY;BYDAY=SA;BYSETPOS=-1"), friday)
        assertEquals(
            Triple(MonthlyMode.WEEKDAY_OF_MONTH, -1, SATURDAY),
            Triple(read.monthlyMode, read.ordinal, read.weekday)
        )
        assertEquals("FREQ=MONTHLY;BYDAY=-1SA", format(read))
    }

    @Test
    fun `monthly on the same day, weekly on some days, daily and yearly`() {
        assertEquals(
            "FREQ=MONTHLY;INTERVAL=2",
            format(CustomRepeat(frequency = Frequency.MONTHLY, interval = 2))
        )
        assertEquals(
            MonthlyMode.DAY_OF_MONTH,
            CustomRepeat.from(rule("FREQ=MONTHLY"), friday).monthlyMode
        )
        assertEquals(
            "FREQ=WEEKLY;BYDAY=MO,WE",
            format(CustomRepeat(weekdays = setOf(WEDNESDAY, MONDAY)))
        )
        assertEquals("FREQ=WEEKLY;BYDAY=FR", format(CustomRepeat()))
        assertEquals(
            setOf(MONDAY, WEDNESDAY),
            CustomRepeat.from(rule("FREQ=WEEKLY;BYDAY=MO,WE"), friday).weekdays
        )
        assertEquals(
            "FREQ=DAILY;INTERVAL=3",
            format(
                CustomRepeat(frequency = Frequency.DAILY, interval = 3, weekdays = setOf(MONDAY))
            )
        )
        assertEquals(
            "FREQ=YEARLY",
            format(CustomRepeat(frequency = Frequency.YEARLY, interval = 0))
        )
    }

    @Test
    fun `endings`() {
        val until = LocalDate.parse("2027-06-30")
        assertEquals(
            "FREQ=WEEKLY;BYDAY=FR;COUNT=5",
            format(CustomRepeat(end = RepeatEnd.AFTER_COUNT, count = 5))
        )
        assertEquals(
            "FREQ=WEEKLY;BYDAY=FR;COUNT=1",
            format(CustomRepeat(end = RepeatEnd.AFTER_COUNT, count = 0))
        )
        assertEquals(
            "FREQ=WEEKLY;BYDAY=FR;UNTIL=20270630",
            format(CustomRepeat(end = RepeatEnd.ON_DATE, until = until))
        )
        assertEquals(
            "FREQ=WEEKLY;BYDAY=FR",
            format(CustomRepeat(end = RepeatEnd.NEVER, until = until, count = 3))
        )
        assertEquals(
            RepeatEnd.AFTER_COUNT to 5,
            CustomRepeat.from(rule("FREQ=DAILY;COUNT=5"), friday).let {
                it.end to
                    it.count
            }
        )
        assertEquals(
            RepeatEnd.ON_DATE to until,
            CustomRepeat.from(rule("FREQ=DAILY;UNTIL=20270630"), friday).let {
                it.end to
                    it.until
            }
        )
        assertEquals(RepeatEnd.NEVER, CustomRepeat.from(rule("FREQ=DAILY"), friday).end)
    }
}
