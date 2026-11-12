// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DueEditsTest {
    private val task =
        TaskEntity(accountId = 1, listHref = "/l/", href = "/l/t.ics", uid = "t", summary = "T")
    private val day = LocalDate.parse("2026-10-05")
    private val today = LocalDate.parse("2026-11-12")

    @Test
    fun `a date alone is all day, without zone`() {
        val dated = DueEdits.withDate(task, day)
        assertEquals("2026-10-05" to null, dated.due to dated.dueZone)
        assertEquals(day, DueEdits.date(dated))
        assertNull(DueEdits.time(dated))
    }

    @Test
    fun `a new time is floating and date changes keep the zone as it was`() {
        val timed = DueEdits.withTime(DueEdits.withDate(task, day), LocalTime.of(9, 30), today)
        assertEquals("2026-10-05T09:30" to null, timed.due to timed.dueZone)
        assertEquals(LocalTime.of(9, 30), DueEdits.time(timed))
        val moved = DueEdits.withDate(timed, day.plusDays(1))
        assertEquals("2026-10-06T09:30" to null, moved.due to moved.dueZone)
        val zoned = timed.copy(dueZone = "Europe/Madrid")
        assertEquals("Europe/Madrid", DueEdits.withDate(zoned, day).dueZone)
        assertEquals(
            "UTC",
            DueEdits.withTime(zoned.copy(dueZone = "UTC"), LocalTime.NOON, today).dueZone
        )
    }

    @Test
    fun `changing the zone keeps the wall clock time`() {
        val timed = DueEdits.withTime(DueEdits.withDate(task, day), LocalTime.of(9, 30), today)
        val madrid = DueEdits.withZone(timed, "Europe/Madrid")
        assertEquals("2026-10-05T09:30" to "Europe/Madrid", madrid.due to madrid.dueZone)
        assertNull(DueEdits.withZone(madrid, null).dueZone)
    }

    @Test
    fun `the device time is shown only for a fixed zone that differs`() {
        val timed = DueEdits.withTime(DueEdits.withDate(task, day), LocalTime.of(9, 0), today)
        val beijing = ZoneId.of("Asia/Shanghai")
        assertNull(DueEdits.inDeviceZone(timed, beijing))
        val madrid = DueEdits.withZone(timed, "Europe/Madrid")
        assertEquals(
            LocalDateTime.parse("2026-10-05T15:00"),
            DueEdits.inDeviceZone(madrid, beijing)
        )
        assertNull(DueEdits.inDeviceZone(madrid, ZoneId.of("Europe/Madrid")))
        assertNull(DueEdits.inDeviceZone(madrid.copy(dueZone = "Nope/Zone"), beijing))
        assertNull(
            DueEdits.inDeviceZone(DueEdits.withDate(task, day).copy(dueZone = "UTC"), beijing)
        )
    }

    @Test
    fun `an all day task has no zone to change`() {
        val dated = DueEdits.withDate(task, day)
        assertEquals(dated, DueEdits.withZone(dated, "Europe/Madrid"))
        assertEquals(task, DueEdits.withZone(task, "UTC"))
    }

    @Test
    fun `removing the time keeps the date, removing the date removes everything`() {
        val timed = DueEdits.withTime(DueEdits.withDate(task, day), LocalTime.NOON, today)
            .copy(dueZone = "Europe/Madrid", reminderBefore = 900)
        val allDay = DueEdits.withTime(timed, null, today)
        assertEquals("2026-10-05" to null, allDay.due to allDay.dueZone)
        val none = DueEdits.withDate(timed, null)
        assertEquals(Triple(null, null, null), Triple(none.due, none.dueZone, none.reminderBefore))
        assertNull(DueEdits.date(none))
    }

    @Test
    fun `a time without date is today`() {
        val timed = DueEdits.withTime(task, LocalTime.of(8, 0), today)
        assertEquals("2026-11-12T08:00", timed.due)
    }
}
