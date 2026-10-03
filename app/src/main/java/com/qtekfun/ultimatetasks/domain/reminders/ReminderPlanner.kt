// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.reminders

import com.qtekfun.ultimatetasks.data.ical.IcsDate
import com.qtekfun.ultimatetasks.data.local.model.DueTaskRow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** A notification to show at [at] (RF-10). [id] tells the alarms apart: two per task. */
data class Reminder(
    val id: Long,
    val taskId: Long,
    val title: String,
    val listName: String,
    val due: Instant,
    val at: Instant,
    val early: Boolean
)

/**
 * Which reminders to schedule: a pure decision, the alarms are elsewhere. Each task reminds at
 * its due time (all-day tasks at [allDayHour]) and, if it has one, earlier by its early
 * reminder. A snoozed task reminds at its snooze time instead. Only future ones count.
 */
object ReminderPlanner {
    fun plan(
        tasks: List<DueTaskRow>,
        snoozes: Map<Long, Instant>,
        allDayHour: Int,
        now: Instant,
        zone: ZoneId
    ): List<Reminder> = tasks.flatMap { task ->
        val due = dueInstant(task, allDayHour, zone) ?: return@flatMap emptyList()
        // A snooze that already rang no longer hides the due time (it may have changed since).
        val main = snoozes[task.id]?.takeIf { it.isAfter(now) } ?: due
        val early = task.reminderBefore?.takeIf { it > 0 }?.let { due.minusSeconds(it) }
        listOfNotNull(
            Reminder(task.id * 2, task.id, task.summary, task.listName, due, main, early = false),
            early?.let {
                Reminder(
                    task.id * 2 + 1,
                    task.id,
                    task.summary,
                    task.listName,
                    due,
                    it,
                    early = true
                )
            }
        )
    }.filter { it.at.isAfter(now) }

    /** When the task is due; an all-day task, at [allDayHour] of its day in [zone]. */
    fun dueInstant(task: DueTaskRow, allDayHour: Int, zone: ZoneId): Instant? = runCatching {
        val date = IcsDate(task.due, task.dueZone)
        if (date.allDay) {
            LocalDate.parse(task.due).atTime(LocalTime.of(allDayHour, 0)).atZone(zone).toInstant()
        } else {
            date.toInstant(zone)
        }
    }.getOrNull()
}
