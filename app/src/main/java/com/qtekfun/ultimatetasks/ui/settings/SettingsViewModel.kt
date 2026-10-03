// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.settings.AppSettings
import com.qtekfun.ultimatetasks.data.settings.SettingsRepository
import com.qtekfun.ultimatetasks.data.task.ListRepository
import com.qtekfun.ultimatetasks.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    /** Settings are set straight on the repository: each is one call, nothing to compute. */
    val repository: SettingsRepository,
    tasks: TaskRepository,
    private val lists: ListRepository
) : ViewModel() {
    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_MS),
            AppSettings()
        )

    val taskLists: StateFlow<List<TaskListEntity>> =
        tasks.observeLists().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_MS),
            emptyList()
        )

    fun setVisible(href: String, visible: Boolean) {
        viewModelScope.launch { lists.setVisible(href, visible) }
    }

    fun setAlarmClock(on: Boolean) = repository.setAlarmClock(on)

    fun setAllDayHour(hour: Int) = repository.setAllDayHour(hour)

    private companion object {
        const val STOP_MS = 5_000L
    }
}
