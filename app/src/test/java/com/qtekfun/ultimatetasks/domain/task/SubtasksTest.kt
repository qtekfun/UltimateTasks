// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SubtasksTest {
    private fun task(uid: String, parent: String? = null) = TaskEntity(
        accountId = 1,
        listHref = "/a/",
        href = "/a/$uid.ics",
        uid = uid,
        summary = uid,
        parentUid = parent
    )

    private fun shape(items: List<TaskRowItem>) = items.map {
        Triple(it.task.uid, it.depth, it.children)
    }

    @Test
    fun `subtasks go under their parent, in order`() {
        val tasks = listOf(task("b1", "b"), task("a"), task("b"), task("a1", "a"), task("a2", "a"))
        assertEquals(
            listOf(
                Triple("a", 0, 2),
                Triple("a1", 1, 0),
                Triple("a2", 1, 0),
                Triple("b", 0, 1),
                Triple("b1", 1, 0)
            ),
            shape(Subtasks.arrange(tasks))
        )
    }

    @Test
    fun `deeper levels go under the top parent, orphans and cycles stand alone`() {
        val tasks =
            listOf(
                task("a"),
                task("a1", "a"),
                task("a11", "a1"),
                task("orphan", "missing"),
                task("x", "y"),
                task("y", "x")
            )
        assertEquals(
            listOf(
                Triple("a", 0, 2),
                Triple("a1", 1, 0),
                Triple("a11", 1, 0),
                Triple("orphan", 0, 0),
                Triple("x", 0, 0),
                Triple("y", 0, 0)
            ).sortedBy {
                it.first
            },
            shape(Subtasks.arrange(tasks)).sortedBy { it.first }
        )
    }

    @Test
    fun `collapsed parents hide their subtasks but keep the count`() {
        val tasks = listOf(task("a"), task("a1", "a"))
        assertEquals(
            listOf(Triple("a", 0, 1)),
            shape(Subtasks.arrange(tasks, collapsed = setOf("a")))
        )
    }
}
