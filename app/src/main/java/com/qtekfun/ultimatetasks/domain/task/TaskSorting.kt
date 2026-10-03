// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity

/** How a list is sorted on this device (RF-03). */
enum class TaskSort { MANUAL, DUE, PRIORITY, TITLE }

object TaskSorting {
    /**
     * [tasks] (given in manual order) in [sort] order. Tasks without due date or priority go
     * last; ties keep the manual order.
     */
    fun sort(tasks: List<TaskEntity>, sort: TaskSort): List<TaskEntity> = when (sort) {
        TaskSort.MANUAL -> tasks
        TaskSort.DUE -> tasks.sortedWith(compareBy(nullsLast()) { it.due })
        TaskSort.PRIORITY -> tasks.sortedBy { if (it.priority == 0) Int.MAX_VALUE else it.priority }
        TaskSort.TITLE -> tasks.sortedBy { it.summary.lowercase() }
    }

    /**
     * The new manual order after moving the task at [from] one place up (-1) or down (+1): the
     * `X-APPLE-SORT-ORDER` of each task that has to change. Tasks keep spaced numbers so later
     * moves usually touch two of them.
     */
    fun move(tasks: List<TaskEntity>, from: Int, step: Int): Map<Long, Long> {
        val to = from + step
        if (to !in tasks.indices) return emptyMap()
        val moved = tasks.toMutableList().apply { add(to, removeAt(from)) }
        return moved.withIndex()
            .associate { (index, task) -> task.id to (index + 1L) * GAP }
            .filter { (id, order) -> tasks.first { it.id == id }.sortOrder != order }
    }

    private const val GAP = 1_000L
}
