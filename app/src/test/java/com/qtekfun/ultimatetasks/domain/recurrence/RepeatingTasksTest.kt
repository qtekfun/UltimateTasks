// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.recurrence

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RepeatingTasksTest {
    private val today = LocalDate.parse("2026-10-03")

    @Test
    fun `due and start move together and keep their time`() {
        assertEquals(
            NextOccurrence("2026-10-08T11:00", "2026-10-07", "FREQ=WEEKLY;BYDAY=TU,TH"),
            RepeatingTasks.next("2026-10-06T11:00", "2026-10-05", "FREQ=WEEKLY;BYDAY=TU,TH", today)
        )
    }

    @Test
    fun `without due date the start leads, and without either, today`() {
        assertEquals(
            NextOccurrence(null, "2026-10-02", "FREQ=DAILY"),
            RepeatingTasks.next(null, "2026-10-01", "FREQ=DAILY", today)
        )
        assertEquals(
            NextOccurrence(null, null, "FREQ=DAILY"),
            RepeatingTasks.next(null, null, "FREQ=DAILY", today)
        )
    }

    @Test
    fun `count goes down and the last occurrence has no next`() {
        assertEquals(
            "FREQ=DAILY;COUNT=2",
            RepeatingTasks.next("2026-10-03", null, "FREQ=DAILY;COUNT=3", today)!!.recurrence
        )
        assertNull(RepeatingTasks.next("2026-10-03", null, "FREQ=DAILY;COUNT=1", today))
    }

    @Test
    fun `rules the app cannot follow`() {
        assertNull(RepeatingTasks.next("2026-10-03", null, "FREQ=HOURLY", today))
        assertTrue(RepeatingTasks.understood(null))
        assertTrue(RepeatingTasks.understood("FREQ=MONTHLY;BYDAY=SA;BYSETPOS=-1"))
        assertFalse(RepeatingTasks.understood("FREQ=MINUTELY"))
    }
}
