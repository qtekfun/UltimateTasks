// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatetasks.data.task.HomeData
import com.qtekfun.ultimatetasks.data.task.TaskRepository
import com.qtekfun.ultimatetasks.sync.engine.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: TaskRepository,
    private val scheduler: SyncScheduler
) : ViewModel() {
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
