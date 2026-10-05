// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.reminders

import com.qtekfun.ultimatetasks.data.local.model.DueTaskRow
import java.time.Instant
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReminderPlannerTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val now = Instant.parse("2026-10-03T10:00:00Z")

    private fun task(id: Long, due: String, zone: String? = null, before: Long? = null) =
        DueTaskRow(id, "Task $id", "List", due, zone, before)

    private fun plan(vararg tasks: DueTaskRow, snoozes: Map<Long, Instant> = emptyMap()) =
        ReminderPlanner.plan(tasks.toList(), snoozes, allDayHour = 9, now = now, zone = madrid)

    @Test
    fun `timed tasks remind at their time, in their zone`() {
        assertEquals(
            listOf(
                Reminder(
                    2,
                    1,
                    "Task 1",
                    "List",
                    Instant.parse("2026-10-03T16:00:00Z"),
                    Instant.parse("2026-10-03T16:00:00Z"),
                    false
                )
            ),
            plan(task(1, "2026-10-03T18:00", "Europe/Madrid"))
        )
        assertEquals(
            Instant.parse("2026-10-03T18:00:00Z"),
            plan(task(1, "2026-10-03T18:00", "UTC")).single().at
        )
    }

    @Test
    fun `all-day tasks remind at the chosen hour`() {
        assertEquals(Instant.parse("2026-10-04T07:00:00Z"), plan(task(1, "2026-10-04")).single().at)
    }

    @Test
    fun `early reminders come first, and only when set`() {
        val reminders =
            plan(
                task(1, "2026-10-04T12:00", "Europe/Madrid", before = 3_600),
                task(2, "2026-10-04", before = 0)
            )
        assertEquals(
            listOf(
                Triple(2L, Instant.parse("2026-10-04T10:00:00Z"), false),
                Triple(3L, Instant.parse("2026-10-04T09:00:00Z"), true),
                Triple(4L, Instant.parse("2026-10-04T07:00:00Z"), false)
            ),
            reminders.map { Triple(it.id, it.at, it.early) }
        )
    }

    @Test
    fun `a snooze replaces the due time reminder`() {
        val snoozed = Instant.parse("2026-10-03T10:15:00Z")
        val reminders =
            plan(task(1, "2026-10-03T11:00", "Europe/Madrid"), snoozes = mapOf(1L to snoozed))
        assertEquals(listOf(snoozed), reminders.map { it.at })
        val rang =
            plan(
                task(1, "2026-10-03T13:00", "Europe/Madrid"),
                snoozes = mapOf(1L to Instant.parse("2026-10-03T09:00:00Z"))
            )
        assertEquals(listOf(Instant.parse("2026-10-03T11:00:00Z")), rang.map { it.at })
    }

    @Test
    fun `past reminders and unreadable dates are left out`() {
        assertEquals(
            emptyList<Reminder>(),
            plan(task(1, "2026-10-02T09:00", "Europe/Madrid"), task(2, "yesterday"))
        )
        assertNull(ReminderPlanner.dueInstant(task(2, "yesterday"), 9, madrid))
    }

    @Test
    fun `planning everything keeps past reminders, and a past snooze replaces the due time`() {
        val rang = Instant.parse("2026-10-03T08:30:00Z")
        val all = ReminderPlanner.planAll(
            listOf(
                task(1, "2026-10-03T09:00", "Europe/Madrid", before = 3600),
                task(2, "2026-10-02T09:00", "Europe/Madrid")
            ),
            mapOf(2L to rang),
            allDayHour = 9,
            zone = madrid
        )
        assertEquals(
            listOf(
                2L to Instant.parse("2026-10-03T07:00:00Z"),
                3L to Instant.parse("2026-10-03T06:00:00Z"),
                4L to rang
            ),
            all.map { it.id to it.at }
        )
    }
}
