// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.detail

import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.task.ConflictChoice
import com.qtekfun.ultimatetasks.data.task.TaskEditor
import com.qtekfun.ultimatetasks.data.task.TaskRepository
import com.qtekfun.ultimatetasks.sync.conflict.TaskField

/** Actions on the whole task; [run] executes them on the task shown, in the view model's scope. */
class TaskDetailActions(
    private val editor: TaskEditor,
    private val repository: TaskRepository,
    private val run: (suspend (Long) -> Unit) -> Unit
) {
    fun move(listHref: String) = run { editor.move(it, listHref) }

    fun delete() = run { editor.delete(it) }

    fun resolve(field: TaskField, choice: ConflictChoice) = run {
        editor.resolve(it, field, choice)
    }

    fun keepCopy() = run { editor.keepCopy(it) }

    fun discard() = run { editor.discard(it) }

    /** Adds a subtask in the same list (RF-07). */
    fun addSubtask(parent: TaskEntity, title: String) =
        run { repository.create(parent.listHref, title, parent.uid) }

    fun setChildCompleted(child: TaskEntity, completed: Boolean) = run {
        repository.setCompleted(child, completed)
    }
}
