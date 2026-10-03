// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DueEditsTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val task =
        TaskEntity(accountId = 1, listHref = "/l/", href = "/l/t.ics", uid = "t", summary = "T")
    private val day = LocalDate.parse("2026-10-05")

    @Test
    fun `a date alone is all day, without zone`() {
        val dated = DueEdits.withDate(task, day, madrid)
        assertEquals("2026-10-05" to null, dated.due to dated.dueZone)
        assertEquals(day, DueEdits.date(dated))
        assertNull(DueEdits.time(dated))
    }

    @Test
    fun `a time pins the device zone and keeps through date changes`() {
        val timed = DueEdits.withTime(
            DueEdits.withDate(task, day, madrid),
            LocalTime.of(9, 30),
            madrid
        )
        assertEquals("2026-10-05T09:30" to "Europe/Madrid", timed.due to timed.dueZone)
        assertEquals(LocalTime.of(9, 30), DueEdits.time(timed))
        val moved = DueEdits.withDate(timed, day.plusDays(1), madrid)
        assertEquals("2026-10-06T09:30" to "Europe/Madrid", moved.due to moved.dueZone)
        val utc = DueEdits.withDate(timed.copy(dueZone = null), day, madrid)
        assertEquals("Europe/Madrid", utc.dueZone)
        assertEquals(
            "UTC",
            DueEdits.withTime(timed.copy(dueZone = "UTC"), LocalTime.NOON, madrid).dueZone
        )
    }

    @Test
    fun `removing the time keeps the date, removing the date removes everything`() {
        val timed = DueEdits.withTime(
            DueEdits.withDate(task, day, madrid),
            LocalTime.NOON,
            madrid
        ).copy(reminderBefore = 900)
        val allDay = DueEdits.withTime(timed, null, madrid)
        assertEquals("2026-10-05" to null, allDay.due to allDay.dueZone)
        val none = DueEdits.withDate(timed, null, madrid)
        assertEquals(Triple(null, null, null), Triple(none.due, none.dueZone, none.reminderBefore))
        assertNull(DueEdits.date(none))
    }

    @Test
    fun `a time without date is today`() {
        val timed = DueEdits.withTime(task, LocalTime.of(8, 0), madrid)
        assertEquals(LocalDate.now(madrid).toString() + "T08:00", timed.due)
    }
}
