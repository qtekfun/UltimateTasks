// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.reminders

import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HeartbeatTest {
    private val now = Instant.parse("2026-10-05T10:00:00Z")

    private fun reminder(at: Instant) = Reminder(2, 1, "Task", "List", at, at, early = false)

    @Test
    fun `beats every half hour while a reminder is still to come`() {
        assertEquals(
            now.plus(Duration.ofMinutes(30)),
            Heartbeat.next(
                now,
                listOf(reminder(now.minusSeconds(60)), reminder(now.plusSeconds(60)))
            )
        )
        assertEquals(
            now.plusSeconds(600),
            Heartbeat.next(now, listOf(reminder(now.plusSeconds(1))), Duration.ofMinutes(10))
        )
    }

    @Test
    fun `stops when nothing is left to protect`() {
        assertNull(Heartbeat.next(now, emptyList()))
        assertNull(Heartbeat.next(now, listOf(reminder(now), reminder(now.minusSeconds(1)))))
    }
}
