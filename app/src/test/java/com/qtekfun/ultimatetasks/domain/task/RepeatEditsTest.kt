// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import java.time.LocalDate
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RepeatEditsTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.parse("2026-10-03")
    private val task =
        TaskEntity(accountId = 1, listHref = "/l/", href = "/l/t.ics", uid = "t", summary = "T")

    @Test
    fun `the date moves to the first occurrence and keeps its time`() {
        val monday = task.copy(due = "2026-10-05T18:00", dueZone = "Europe/Madrid")
        val third = RepeatEdits.withRule(monday, "FREQ=MONTHLY;BYDAY=3FR", today, zone)
        assertEquals("2026-10-16T18:00" to "FREQ=MONTHLY;BYDAY=3FR", third.due to third.recurrence)
        // Already an occurrence: it stays.
        assertEquals(
            "2026-10-05T18:00",
            RepeatEdits.withRule(monday, "FREQ=WEEKLY;BYDAY=MO", today, zone).due
        )
    }

    @Test
    fun `a task without date starts on the first occurrence from today`() {
        assertEquals(
            "2026-10-16",
            RepeatEdits.withRule(task, "FREQ=MONTHLY;BYDAY=3FR", today, zone).due
        )
    }

    @Test
    fun `never and rules the app cannot follow leave the date alone`() {
        val dated = task.copy(due = "2026-10-05", recurrence = "FREQ=DAILY")
        assertEquals(dated.copy(recurrence = null), RepeatEdits.withRule(dated, null, today, zone))
        assertEquals(
            dated.copy(recurrence = "FREQ=HOURLY"),
            RepeatEdits.withRule(dated, "FREQ=HOURLY", today, zone)
        )
        // A series that already ended keeps the date.
        assertEquals(
            "2026-10-05",
            RepeatEdits.withRule(dated, "FREQ=DAILY;UNTIL=20261001", today, zone).due
        )
    }
}
