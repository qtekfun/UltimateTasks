// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import com.qtekfun.ultimatetasks.data.ical.IcsDate
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The Date and Time switches of the detail screen (RF-05). A date alone is an all-day task
 * without zone; a time without zone is floating, "9:00 wherever you are", so it follows the
 * device through travel and daylight saving. A zone is only set on purpose, by [withZone].
 * Removing the date removes the time and the early reminder too.
 */
object DueEdits {
    private const val DATE_LENGTH = 10

    fun withDate(task: TaskEntity, date: LocalDate?): TaskEntity = when {
        date == null -> task.copy(due = null, dueZone = null, reminderBefore = null)

        task.due == null || task.due.length == DATE_LENGTH -> task.copy(
            due = date.toString(),
            dueZone = null
        )

        else -> task.copy(due = date.toString() + task.due.drop(DATE_LENGTH))
    }

    fun withTime(task: TaskEntity, time: LocalTime?, today: LocalDate): TaskEntity {
        val date = task.due?.take(DATE_LENGTH)?.let(LocalDate::parse) ?: today
        return if (time == null) {
            task.copy(due = date.toString(), dueZone = null)
        } else {
            task.copy(due = LocalDateTime.of(date, time).toString())
        }
    }

    /**
     * Sets the zone of a timed due date, keeping its wall-clock time: null is floating, else
     * "UTC" or an IANA id. Without a time there is nothing to zone, so the task is unchanged.
     */
    fun withZone(task: TaskEntity, zone: String?): TaskEntity =
        if (time(task) == null) task else task.copy(dueZone = zone)

    /**
     * The wall-clock time the device shows for a timed due date in another zone, or null when
     * floating, unknown or already in [device]'s zone (nothing to translate).
     */
    fun inDeviceZone(task: TaskEntity, device: ZoneId): LocalDateTime? {
        val zone = task.dueZone?.let(IcsDate::zoneOf)
        val local = task.due?.takeIf { it.length > DATE_LENGTH }?.let(LocalDateTime::parse)
        val shown = zone?.let { local?.atZone(it)?.withZoneSameInstant(device)?.toLocalDateTime() }
        return shown?.takeIf { it != local }
    }

    /** The time of a timed due date, else null. */
    fun time(task: TaskEntity): LocalTime? =
        task.due?.takeIf { it.length > DATE_LENGTH }?.let { LocalDateTime.parse(it).toLocalTime() }

    fun date(task: TaskEntity): LocalDate? = task.due?.take(DATE_LENGTH)?.let(LocalDate::parse)
}
