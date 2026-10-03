// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.domain.recurrence.RecurrenceRules
import com.qtekfun.ultimatetasks.domain.recurrence.Recurrences
import java.time.LocalDate
import java.time.ZoneId

/**
 * Setting a repetition (RF-06). The task's date moves to the first occurrence on or after it
 * (choosing "the 3rd Friday" on a Monday moves it to that Friday), keeping its time; a task
 * without date starts on the first occurrence from [today].
 */
object RepeatEdits {
    fun withRule(task: TaskEntity, rule: String?, today: LocalDate, zone: ZoneId): TaskEntity {
        val parsed = rule?.let(RecurrenceRules::parse) ?: return task.copy(recurrence = rule)
        val from = DueEdits.date(task) ?: today
        val first = Recurrences.next(parsed, from, from.minusDays(1)) ?: from
        return DueEdits.withDate(task, first, zone).copy(recurrence = rule)
    }
}
