// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Named
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What only lives on the device for a list: shown or hidden, and its icon (RF-08, RF-09). */
@Serializable
data class ListPrefs(val href: String, val visible: Boolean, val icon: String?)

/**
 * List preferences restored from a backup before the lists exist here (T23): they are applied
 * when the first sync brings each list, then forgotten.
 */
class PendingListPrefs @Inject constructor(
    @Named(SettingsRepository.SETTINGS_PREFERENCES) private val preferences: SharedPreferences
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun save(lists: List<ListPrefs>) = preferences.edit {
        putString(KEY, json.encodeToString(lists))
    }

    /** The preferences waiting for the list [href], removed from the waiting ones. */
    fun take(href: String): ListPrefs? {
        val waiting = read()
        val found = waiting.firstOrNull { it.href == href } ?: return null
        save(waiting - found)
        return found
    }

    private fun read(): List<ListPrefs> = preferences.getString(KEY, null)?.let {
        runCatching { json.decodeFromString<List<ListPrefs>>(it) }.getOrNull()
    }.orEmpty()

    private companion object {
        const val KEY = "pending_list_prefs"
    }
}
