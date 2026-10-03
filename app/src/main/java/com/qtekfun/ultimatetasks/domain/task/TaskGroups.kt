// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What a group of a smart list is about, worded by the screen (RF-02, T16). */
sealed interface GroupKey {
    data object None : GroupKey

    data object Overdue : GroupKey

    data class Day(val date: LocalDate) : GroupKey

    data class InList(val href: String) : GroupKey
}

data class TaskGroup(val key: GroupKey, val tasks: List<TaskEntity>)

/**
 * The sections of each view, as in Apple Reminders: Today shows the overdue tasks apart,
 * Scheduled one section per day (overdue first), All one per list in the home order. A list
 * and Completed are a single section without title.
 */
object TaskGroups {
    fun group(
        source: TaskSource,
        tasks: List<TaskEntity>,
        listOrder: List<String>,
        now: Instant,
        zone: ZoneId
    ): List<TaskGroup> = when (source) {
        TaskSource.Smart(
            SmartList.TODAY
        ), TaskSource.Smart(SmartList.SCHEDULED) -> byDay(source, tasks, now, zone)

        TaskSource.Smart(SmartList.ALL) -> tasks.groupBy { it.listHref }
            .entries.sortedBy { (href, _) ->
                listOrder.indexOf(href).let {
                    if (it <
                        0
                    ) {
                        Int.MAX_VALUE
                    } else {
                        it
                    }
                }
            }
            .map { (href, inList) -> TaskGroup(GroupKey.InList(href), inList) }

        else -> listOf(TaskGroup(GroupKey.None, tasks)).filter { it.tasks.isNotEmpty() }
    }

    private fun byDay(
        source: TaskSource,
        tasks: List<TaskEntity>,
        now: Instant,
        zone: ZoneId
    ): List<TaskGroup> {
        val today = now.atZone(zone).toLocalDate()
        val keyed = tasks.map { task ->
            val due = task.due?.let { DueDates.info(it, task.dueZone, now, zone) }
            val key = when {
                due == null -> GroupKey.None
                due.overdue && due.date.isBefore(today) -> GroupKey.Overdue
                due.overdue && source == TaskSource.Smart(SmartList.SCHEDULED) -> GroupKey.Overdue
                else -> GroupKey.Day(due.date)
            }
            key to task
        }
        val order = keyed.map {
            it.first
        }.distinct().sortedWith(
            compareBy({
                it !is GroupKey.Overdue
            }, { (it as? GroupKey.Day)?.date })
        )
        return order.map { key ->
            TaskGroup(key, keyed.filter { it.first == key }.map { it.second })
        }
    }
}
