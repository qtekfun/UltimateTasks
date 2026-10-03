// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DueDatesTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val now = Instant.parse("2026-10-03T10:00:00Z") // 12:00 in Madrid

    @Test
    fun `an all-day task is overdue from the next day`() {
        assertFalse(DueDates.info("2026-10-03", null, now, madrid)!!.overdue)
        assertTrue(DueDates.info("2026-10-02", null, now, madrid)!!.overdue)
        val info = DueDates.info("2026-10-03", null, now, madrid)!!
        assertEquals(LocalDate.parse("2026-10-03"), info.date)
        assertFalse(info.hasTime)
    }

    @Test
    fun `a timed task is overdue from its time, in its own zone`() {
        assertTrue(DueDates.info("2026-10-03T11:59", null, now, madrid)!!.overdue)
        assertFalse(DueDates.info("2026-10-03T12:01", null, now, madrid)!!.overdue)
        val utc = DueDates.info("2026-10-03T23:30", "UTC", now, madrid)!!
        assertEquals(LocalDate.parse("2026-10-04"), utc.date)
        assertTrue(utc.hasTime)
    }

    @Test
    fun `unreadable dates have no info`() {
        assertNull(DueDates.info("mañana", null, now, madrid))
    }

    @Test
    fun `priority marks follow Apple`() {
        assertEquals(
            listOf("", "!!!", "!!!", "!!", "!", "!", ""),
            listOf(0, 1, 4, 5, 6, 9, 10).map(::priorityMarks)
        )
    }
}
