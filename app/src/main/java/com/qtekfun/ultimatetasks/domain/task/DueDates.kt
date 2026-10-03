// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import com.qtekfun.ultimatetasks.data.ical.IcsDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Where a due date falls for the user, to word and color it (RF-03). */
data class DueInfo(
    val date: LocalDate,
    val hasTime: Boolean,
    val instant: Instant,
    val overdue: Boolean
)

object DueDates {
    /**
     * Reads [due] in [zone] (the task's own zone, if any) as seen from [deviceZone] at [now].
     * An all-day task is overdue from the next day on; a timed one, from its time.
     */
    fun info(due: String, zone: String?, now: Instant, deviceZone: ZoneId): DueInfo? = runCatching {
        val date = IcsDate(due, zone)
        val instant = date.toInstant(deviceZone)
        val today = now.atZone(deviceZone).toLocalDate()
        val day = if (date.allDay) {
            LocalDate.parse(
                due
            )
        } else {
            instant.atZone(deviceZone).toLocalDate()
        }
        val overdue = if (date.allDay) day.isBefore(today) else instant.isBefore(now)
        DueInfo(day, !date.allDay, instant, overdue)
    }.getOrNull()
}
