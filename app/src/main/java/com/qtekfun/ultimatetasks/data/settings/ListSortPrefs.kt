// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.qtekfun.ultimatetasks.domain.task.TaskSort
import javax.inject.Inject
import javax.inject.Named

/** How each list is sorted, on this device only (RF-03). */
class ListSortPrefs @Inject constructor(
    @Named(SettingsRepository.SETTINGS_PREFERENCES) private val preferences: SharedPreferences
) {
    fun get(href: String): TaskSort = preferences.getString(KEY + href, null)?.let { name ->
        TaskSort.entries.firstOrNull {
            it.name ==
                name
        }
    }
        ?: TaskSort.MANUAL

    fun set(href: String, sort: TaskSort) = preferences.edit { putString(KEY + href, sort.name) }

    private companion object {
        const val KEY = "sort:"
    }
}
