// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The Date and Time switches of the detail screen (RF-05). A date alone is an all-day task
 * without zone; adding a time pins it to the device's zone, so it means the same moment on
 * every device. Removing the date removes the time and the early reminder too.
 */
object DueEdits {
    private const val DATE_LENGTH = 10

    fun withDate(task: TaskEntity, date: LocalDate?, zone: ZoneId): TaskEntity = when {
        date == null -> task.copy(due = null, dueZone = null, reminderBefore = null)

        task.due == null || task.due.length == DATE_LENGTH -> task.copy(
            due = date.toString(),
            dueZone = null
        )

        else -> task.copy(
            due = date.toString() + task.due.drop(DATE_LENGTH),
            dueZone =
                task.dueZone ?: zone.id
        )
    }

    fun withTime(task: TaskEntity, time: LocalTime?, zone: ZoneId): TaskEntity {
        val date = task.due?.take(DATE_LENGTH)?.let(LocalDate::parse) ?: LocalDate.now(zone)
        return if (time == null) {
            task.copy(due = date.toString(), dueZone = null)
        } else {
            task.copy(
                due = LocalDateTime.of(date, time).toString(),
                dueZone =
                    task.dueZone ?: zone.id
            )
        }
    }

    /** The time of a timed due date, else null. */
    fun time(task: TaskEntity): LocalTime? =
        task.due?.takeIf { it.length > DATE_LENGTH }?.let { LocalDateTime.parse(it).toLocalTime() }

    fun date(task: TaskEntity): LocalDate? = task.due?.take(DATE_LENGTH)?.let(LocalDate::parse)
}
