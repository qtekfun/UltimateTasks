// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.task.HomeData
import com.qtekfun.ultimatetasks.data.task.TaskRepository
import com.qtekfun.ultimatetasks.data.task.TaskSearch
import com.qtekfun.ultimatetasks.sync.engine.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: TaskRepository,
    private val scheduler: SyncScheduler,
    search: TaskSearch
) : ViewModel() {
    private val mutableQuery = MutableStateFlow("")
    private val mutableIncludeCompleted = MutableStateFlow(false)

    /** What is typed in the search bar (RF-12); blank shows the home screen. */
    val query: StateFlow<String> = mutableQuery.asStateFlow()

    val includeCompleted: StateFlow<Boolean> = mutableIncludeCompleted.asStateFlow()

    val results: StateFlow<List<TaskEntity>> = combine(
        mutableQuery,
        mutableIncludeCompleted,
        ::Pair
    )
        .flatMapLatest { (text, completed) -> search.observe(text, completed) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    val lists: StateFlow<Map<String, TaskListEntity>> = repository.observeLists().map { all ->
        all.associateBy { it.href }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyMap())

    fun search(text: String) {
        mutableQuery.value = text
    }

    fun setIncludeCompleted(on: Boolean) {
        mutableIncludeCompleted.value = on
    }

    /** Null until the first data arrives from Room. */
    val home: StateFlow<HomeData?> = repository.observeHome()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), null)

    val syncing: StateFlow<Boolean> = scheduler.syncing()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), false)

    fun refresh() = scheduler.requestSync()

    private companion object {
        const val STOP_MS = 5_000L
    }
}
