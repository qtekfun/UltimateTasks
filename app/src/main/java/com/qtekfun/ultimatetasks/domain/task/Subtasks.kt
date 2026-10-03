// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity

/** A task in a list with its place in the tree: subtasks go right under their parent (RF-07). */
data class TaskRowItem(val task: TaskEntity, val depth: Int, val children: Int)

object Subtasks {
    /**
     * Puts each subtask under its parent, keeping the order of [tasks] among siblings. One level
     * is shown: deeper subtasks go under the top parent. A subtask whose parent is not in
     * [tasks] (another list, completed, filtered out), or in a loop of parents, shows as a task
     * of its own. Subtasks of a
     * parent in [collapsed] are hidden.
     */
    fun arrange(tasks: List<TaskEntity>, collapsed: Set<String> = emptySet()): List<TaskRowItem> {
        val byUid = tasks.associateBy { it.uid }
        fun root(task: TaskEntity): TaskEntity {
            var current = task
            val seen = mutableSetOf(task.uid)
            while (true) {
                val parent = current.parentUid?.let(byUid::get) ?: return current
                // Parents pointing at each other: no tree, each task stands alone.
                if (!seen.add(parent.uid)) return task
                current = parent
            }
        }
        val children = tasks.filter { root(it) !== it }.groupBy { root(it).uid }
        return tasks.filter { root(it) === it }.flatMap { top ->
            val own = children[top.uid].orEmpty()
            listOf(TaskRowItem(top, 0, own.size)) +
                if (top.uid in collapsed) emptyList() else own.map { TaskRowItem(it, 1, 0) }
        }
    }
}
