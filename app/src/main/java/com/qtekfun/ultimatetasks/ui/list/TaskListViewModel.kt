// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.settings.ListSortPrefs
import com.qtekfun.ultimatetasks.data.task.TaskEditor
import com.qtekfun.ultimatetasks.data.task.TaskRepository
import com.qtekfun.ultimatetasks.domain.task.SmartList
import com.qtekfun.ultimatetasks.domain.task.TaskGroup
import com.qtekfun.ultimatetasks.domain.task.TaskGroups
import com.qtekfun.ultimatetasks.domain.task.TaskSorting
import com.qtekfun.ultimatetasks.domain.task.TaskSource
import com.qtekfun.ultimatetasks.sync.engine.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What a task screen shows. [lingering] are just completed tasks still on screen (RF-04). */
data class TaskListState(
    val source: TaskSource? = null,
    val list: TaskListEntity? = null,
    val lists: Map<String, TaskListEntity> = emptyMap(),
    val tasks: List<TaskEntity> = emptyList(),
    val showCompleted: Boolean = false,
    /** [tasks] in the sections of the view (T16). */
    val groups: List<TaskGroup> = emptyList(),
    val lastCompleted: TaskEntity? = null,
    /** Parents whose subtasks are folded away (RF-07); only while the screen lives. */
    val collapsed: Set<String> = emptySet()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val scheduler: SyncScheduler,
    sortPrefs: ListSortPrefs,
    editor: TaskEditor
) : ViewModel() {
    /** Sort order and manual reordering of the list shown (RF-03). */
    val ordering = ListOrdering(sortPrefs, editor) { block -> viewModelScope.launch { block() } }

    private val source = MutableStateFlow<TaskSource?>(null)
    private val showCompleted = MutableStateFlow(false)
    private val lingering = MutableStateFlow<Map<Long, TaskEntity>>(emptyMap())
    private val lastCompleted = MutableStateFlow<TaskEntity?>(null)
    private val collapsed = MutableStateFlow<Set<String>>(emptySet())

    val syncing: StateFlow<Boolean> = scheduler.syncing().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_MS),
        false
    )

    val state: StateFlow<TaskListState> = source.filterNotNull().flatMapLatest { current ->
        combine(
            repository.observeTasks(current),
            repository.observeLists(),
            showCompleted,
            lingering,
            combine(lastCompleted, collapsed, ordering.sort, ::Triple)
        ) { unsorted, lists, showDone, kept, (last, folded, sort) ->
            val tasks = if (current is TaskSource.List) {
                TaskSorting.sort(
                    unsorted,
                    sort
                )
            } else {
                unsorted
            }
            val byHref = lists.associateBy { it.href }
            val visible = when {
                current == TaskSource.Smart(SmartList.COMPLETED) || showDone -> tasks
                else -> tasks.filter { !it.completed || it.id in kept }
            }
            // Completed in a smart list leaves the query at once: keep it on screen a moment.
            val missing = kept.values.filter { k -> visible.none { it.id == k.id } }
            val shown = visible.map { kept[it.id] ?: it } + missing
            TaskListState(
                source = current,
                list = (current as? TaskSource.List)?.let { byHref[it.href] },
                lists = byHref,
                tasks = shown,
                showCompleted = showDone,
                lastCompleted = last,
                collapsed = folded,
                groups = TaskGroups.group(
                    current,
                    shown,
                    lists.map {
                        it.href
                    },
                    Instant.now(),
                    ZoneId.systemDefault()
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), TaskListState())

    fun show(new: TaskSource) {
        if (source.value != new) {
            ordering.show((new as? TaskSource.List)?.href)
            source.value = new
            lingering.value = emptyMap()
            lastCompleted.value = null
        }
    }

    fun toggleCollapsed(uid: String) = collapsed.update { if (uid in it) it - uid else it + uid }

    fun toggleShowCompleted() = showCompleted.update { !it }

    /**
     * Completing keeps the task on screen ~2 s, as Apple does, with an undo (RF-04). A repeating
     * task shows as done for that moment, then moves to its next date.
     */
    fun setCompleted(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch {
            repository.setCompleted(task, completed)
            if (completed) {
                lingering.update { it + (task.id to task.copy(completed = true)) }
                lastCompleted.value = task
                delay(LINGER_MS)
                lingering.update { it - task.id }
            } else {
                lingering.update { it - task.id }
            }
        }
    }

    /** Puts the task back exactly as it was, dates of a repeating task included. */
    fun undo() {
        val task = lastCompleted.value ?: return
        lastCompleted.value = null
        lingering.update { it - task.id }
        viewModelScope.launch { repository.restore(task) }
    }

    fun dismissUndo() {
        lastCompleted.value = null
    }

    fun add(title: String) {
        val list = (source.value as? TaskSource.List)?.href ?: return
        viewModelScope.launch { repository.create(list, title) }
    }

    fun refresh() = scheduler.requestSync()

    private companion object {
        const val STOP_MS = 5_000L
        const val LINGER_MS = 2_000L
    }
}
