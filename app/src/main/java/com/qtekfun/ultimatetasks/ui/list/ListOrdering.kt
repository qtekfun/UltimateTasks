// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.list

import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.settings.ListSortPrefs
import com.qtekfun.ultimatetasks.data.task.TaskEditor
import com.qtekfun.ultimatetasks.domain.task.TaskSort
import com.qtekfun.ultimatetasks.domain.task.TaskSorting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Sort order of the list shown and its Reorder mode (RF-03); [launch] runs in the view model. */
class ListOrdering(
    private val prefs: ListSortPrefs,
    private val editor: TaskEditor,
    private val launch: (suspend () -> Unit) -> Unit
) {
    private val mutableSort = MutableStateFlow(TaskSort.MANUAL)
    private val mutableReordering = MutableStateFlow(false)
    private var href: String? = null

    val sort: StateFlow<TaskSort> = mutableSort.asStateFlow()

    /** Up and down buttons on the rows, to change the manual order. */
    val reordering: StateFlow<Boolean> = mutableReordering.asStateFlow()

    fun show(listHref: String?) {
        href = listHref
        mutableSort.value = listHref?.let(prefs::get) ?: TaskSort.MANUAL
        mutableReordering.value = false
    }

    fun setSort(sort: TaskSort) {
        href?.let { prefs.set(it, sort) }
        mutableSort.value = sort
        if (sort != TaskSort.MANUAL) mutableReordering.value = false
    }

    fun toggleReordering() {
        mutableReordering.value = !mutableReordering.value && mutableSort.value == TaskSort.MANUAL
    }

    /** Moves the task at [index] of [tasks] (top-level tasks, manual order) up or down. */
    fun move(tasks: List<TaskEntity>, index: Int, step: Int) {
        val changes = TaskSorting.move(tasks, index, step)
        if (changes.isNotEmpty()) launch { editor.reorder(changes) }
    }
}
