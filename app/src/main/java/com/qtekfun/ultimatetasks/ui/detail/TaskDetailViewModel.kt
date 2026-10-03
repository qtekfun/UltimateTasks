// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.task.TaskEditor
import com.qtekfun.ultimatetasks.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The task and the lists it could move to. [task] is null until loaded, or once deleted. */
data class TaskDetailState(
    val loaded: Boolean = false,
    val task: TaskEntity? = null,
    val list: TaskListEntity? = null,
    val lists: List<TaskListEntity> = emptyList()
) {
    val editable: Boolean get() = list?.writable == true && task?.deletedOnServer == false
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val editor: TaskEditor,
    private val repository: TaskRepository
) : ViewModel() {
    private val taskId = MutableStateFlow<Long?>(null)
    private var pendingText: Job? = null

    val state: StateFlow<TaskDetailState> = taskId.filterNotNull().flatMapLatest { id ->
        combine(editor.observe(id), repository.observeLists()) { task, lists ->
            TaskDetailState(
                loaded = true,
                task = task?.takeIf { !it.deleted },
                list = lists.firstOrNull { it.href == task?.listHref },
                lists = lists.filter { it.writable }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), TaskDetailState())

    fun show(id: Long) {
        if (taskId.value != id) {
            flush()
            taskId.value = id
        }
    }

    /** Title and notes save shortly after typing stops, and when leaving the screen. */
    fun setSummary(text: String) = typed { it.copy(summary = text) }

    fun setNotes(text: String) = typed { it.copy(notes = text) }

    fun setUrl(text: String) = typed { it.copy(url = text.trim().ifEmpty { null }) }

    /** Saves a change at once: dates, reminder, priority, tags (RF-05). */
    fun edit(change: (TaskEntity) -> TaskEntity) = act { editor.update(it, change) }

    /** Moving, deleting and settling conflicts. */
    val actions =
        TaskDetailActions(editor, repository) { id ->
            taskId.value?.let { viewModelScope.launch { id(it) } }
        }

    /** The subtasks of the task shown (RF-07). */
    val children: StateFlow<List<TaskEntity>> = taskId.filterNotNull()
        .flatMapLatest { id ->
            editor.observe(id).filterNotNull().flatMapLatest { editor.observeChildren(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    /** Saves typing that has not been saved yet. */
    fun flush() {
        pendingText?.let { job ->
            if (job.isActive) {
                job.cancel()
                lastTyped?.let { edit(it) }
            }
        }
        pendingText = null
    }

    override fun onCleared() = flush()

    private var lastTyped: ((TaskEntity) -> TaskEntity)? = null

    private fun typed(change: (TaskEntity) -> TaskEntity) {
        pendingText?.cancel()
        lastTyped = change
        pendingText = viewModelScope.launch {
            delay(TYPING_MS)
            edit(change)
        }
    }

    private fun act(action: suspend (Long) -> Unit) {
        val id = taskId.value ?: return
        viewModelScope.launch { action(id) }
    }

    private companion object {
        const val STOP_MS = 5_000L
        const val TYPING_MS = 600L
    }
}
