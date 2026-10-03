// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TaskSortingTest {
    private fun task(
        id: Long,
        summary: String,
        due: String? = null,
        priority: Int = 0,
        order: Long? = null
    ) = TaskEntity(
        id = id,
        accountId = 1,
        listHref = "/a/",
        href = "/a/$id.ics",
        uid = "$id",
        summary = summary,
        due = due,
        priority = priority,
        sortOrder = order
    )

    private val tasks = listOf(
        task(1, "beta", due = "2026-10-05", priority = 9),
        task(2, "Alpha"),
        task(3, "gamma", due = "2026-10-04T09:00", priority = 1),
        task(4, "delta", due = "2026-10-04", priority = 5)
    )

    @Test
    fun `sorts by due date, priority or title, missing values last`() {
        assertEquals(listOf(1L, 2L, 3L, 4L), TaskSorting.sort(tasks, TaskSort.MANUAL).map { it.id })
        assertEquals(listOf(4L, 3L, 1L, 2L), TaskSorting.sort(tasks, TaskSort.DUE).map { it.id })
        assertEquals(
            listOf(3L, 4L, 1L, 2L),
            TaskSorting.sort(tasks, TaskSort.PRIORITY).map {
                it.id
            }
        )
        assertEquals(listOf(2L, 1L, 4L, 3L), TaskSorting.sort(tasks, TaskSort.TITLE).map { it.id })
    }

    @Test
    fun `moving renumbers only what changed`() {
        val ordered =
            listOf(
                task(1, "a", order = 1000),
                task(2, "b", order = 2000),
                task(3, "c", order = 3000)
            )
        assertEquals(mapOf(2L to 1000L, 1L to 2000L), TaskSorting.move(ordered, 1, -1))
        assertEquals(mapOf(3L to 2000L, 2L to 3000L), TaskSorting.move(ordered, 1, 1))
        assertEquals(emptyMap<Long, Long>(), TaskSorting.move(ordered, 0, -1))
        assertEquals(emptyMap<Long, Long>(), TaskSorting.move(ordered, 2, 1))
        val unordered = listOf(task(1, "a"), task(2, "b"))
        assertEquals(mapOf(2L to 1000L, 1L to 2000L), TaskSorting.move(unordered, 1, -1))
    }
}
