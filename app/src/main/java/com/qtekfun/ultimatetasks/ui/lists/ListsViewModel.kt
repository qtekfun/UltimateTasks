// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.settings.AppSettings
import com.qtekfun.ultimatetasks.data.settings.SettingsRepository
import com.qtekfun.ultimatetasks.data.task.ListChange
import com.qtekfun.ultimatetasks.data.task.ListLook
import com.qtekfun.ultimatetasks.data.task.ListRepository
import com.qtekfun.ultimatetasks.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Creating, editing, reordering and deleting lists (RF-08), with a message when it fails. */
@HiltViewModel
class ListsViewModel @Inject constructor(
    private val repository: ListRepository,
    tasks: TaskRepository,
    settings: SettingsRepository
) : ViewModel() {
    val lists: StateFlow<List<TaskListEntity>> =
        tasks.observeLists().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_MS),
            emptyList()
        )

    val settings: StateFlow<AppSettings> =
        settings.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_MS),
            AppSettings()
        )

    private val mutableMessage = MutableStateFlow<Int?>(null)

    /** A string to show once, after a change that did not happen. */
    val message: StateFlow<Int?> = mutableMessage.asStateFlow()

    fun create(look: ListLook) = change { repository.create(look) }

    fun edit(href: String, look: ListLook) = change { repository.edit(href, look) }

    fun delete(href: String, onDone: () -> Unit) = change {
        repository.delete(href).also {
            if (it ==
                ListChange.DONE
            ) {
                onDone()
            }
        }
    }

    /** Moves the list at [from] one place up (-1) or down (+1) among the visible ones. */
    fun move(order: List<String>, from: Int, step: Int) {
        val to = from + step
        if (to !in order.indices) return
        val moved = order.toMutableList().apply { add(to, removeAt(from)) }
        change { repository.reorder(moved) }
    }

    fun messageShown() {
        mutableMessage.value = null
    }

    private fun change(action: suspend () -> ListChange) {
        viewModelScope.launch {
            mutableMessage.value = when (action()) {
                ListChange.DONE -> null
                ListChange.OFFLINE -> R.string.list_offline
                ListChange.FAILED -> R.string.list_failed
            }
        }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
