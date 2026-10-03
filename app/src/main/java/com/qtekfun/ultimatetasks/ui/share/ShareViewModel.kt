// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.settings.SettingsRepository
import com.qtekfun.ultimatetasks.data.task.TaskEditor
import com.qtekfun.ultimatetasks.data.task.TaskRepository
import com.qtekfun.ultimatetasks.domain.task.SharedTask
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The writable lists, with the default one first, or null before the user signed in. */
data class ShareState(
    val loaded: Boolean = false,
    val lists: List<TaskListEntity> = emptyList(),
    val default: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShareViewModel @Inject constructor(
    session: AccountSession,
    private val repository: TaskRepository,
    private val editor: TaskEditor,
    settings: SettingsRepository
) : ViewModel() {
    // Without account, no lists: the activity explains and closes instead of waiting.
    private val lists = session.activeAccount.flatMapLatest { account ->
        if (account == null) flowOf(emptyList()) else repository.observeLists()
    }

    val state: StateFlow<ShareState> = combine(lists, settings.settings) { all, prefs ->
        val writable = all.filter { it.writable }
        ShareState(
            true,
            writable,
            (
                writable.firstOrNull { it.href == prefs.defaultList }
                    ?: writable.firstOrNull()
                )?.href
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), ShareState())

    init {
        viewModelScope.launch { session.restore() }
    }

    /** Creates the task, then [onDone] runs. */
    fun save(listHref: String, task: SharedTask, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.create(listHref, task.title)?.let { id ->
                editor.update(id) { it.copy(notes = task.notes, url = task.url) }
            }
            onDone()
        }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
