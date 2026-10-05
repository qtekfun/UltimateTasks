// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private const val KEY_THEME = "theme"
private const val KEY_AMOLED = "amoled"
private const val KEY_DYNAMIC_COLOR = "dynamic_color"
private const val KEY_DEFAULT_LIST = "default_list"
private const val KEY_ALLOW_DELETING_LISTS = "allow_deleting_lists"
private const val KEY_ALARM_CLOCK = "alarm_clock"
private const val KEY_ROBUST_MODE = "robust_mode"
private const val KEY_ALL_DAY_HOUR = "all_day_hour"
private const val LAST_HOUR = 23

/**
 * Per-device preferences. They are not task data, so they live in SharedPreferences rather
 * than in Room, and need no extra library.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @Named(SETTINGS_PREFERENCES) private val preferences: SharedPreferences
) {
    /** The current settings, and every change after. */
    val settings: Flow<AppSettings> = callbackFlow {
        trySend(read())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(read())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun setTheme(theme: ThemeMode) = preferences.edit { putString(KEY_THEME, theme.name) }

    fun setAmoled(on: Boolean) = preferences.edit { putBoolean(KEY_AMOLED, on) }

    fun setDynamicColor(on: Boolean) = preferences.edit { putBoolean(KEY_DYNAMIC_COLOR, on) }

    fun setDefaultList(href: String?) = preferences.edit {
        if (href == null) remove(KEY_DEFAULT_LIST) else putString(KEY_DEFAULT_LIST, href)
    }

    fun setAllowDeletingLists(on: Boolean) = preferences.edit {
        putBoolean(KEY_ALLOW_DELETING_LISTS, on)
    }

    fun setAlarmClock(on: Boolean) = preferences.edit { putBoolean(KEY_ALARM_CLOCK, on) }

    fun setRobustMode(on: Boolean) = preferences.edit { putBoolean(KEY_ROBUST_MODE, on) }

    fun setAllDayHour(hour: Int) = preferences.edit {
        putInt(KEY_ALL_DAY_HOUR, hour.coerceIn(0, LAST_HOUR))
    }

    /** Applies every setting at once, from a backup (T23). */
    fun restore(restored: AppSettings) = preferences.edit {
        putString(KEY_THEME, restored.theme.name)
        putBoolean(KEY_AMOLED, restored.amoled)
        putBoolean(KEY_DYNAMIC_COLOR, restored.dynamicColor)
        if (restored.defaultList ==
            null
        ) {
            remove(KEY_DEFAULT_LIST)
        } else {
            putString(KEY_DEFAULT_LIST, restored.defaultList)
        }
        putBoolean(KEY_ALLOW_DELETING_LISTS, restored.allowDeletingLists)
        putBoolean(KEY_ALARM_CLOCK, restored.alarmClock)
        putBoolean(KEY_ROBUST_MODE, restored.robustMode)
        putInt(KEY_ALL_DAY_HOUR, restored.allDayHour.coerceIn(0, LAST_HOUR))
    }

    private fun read(): AppSettings {
        val defaults = AppSettings()
        val theme = preferences.getString(KEY_THEME, null)
        return AppSettings(
            theme = ThemeMode.entries.firstOrNull { it.name == theme } ?: defaults.theme,
            amoled = preferences.getBoolean(KEY_AMOLED, defaults.amoled),
            dynamicColor = preferences.getBoolean(KEY_DYNAMIC_COLOR, defaults.dynamicColor),
            defaultList = preferences.getString(KEY_DEFAULT_LIST, null),
            allowDeletingLists = preferences.getBoolean(
                KEY_ALLOW_DELETING_LISTS,
                defaults.allowDeletingLists
            ),
            alarmClock = preferences.getBoolean(KEY_ALARM_CLOCK, defaults.alarmClock),
            robustMode = preferences.getBoolean(KEY_ROBUST_MODE, defaults.robustMode),
            allDayHour = preferences.getInt(KEY_ALL_DAY_HOUR, defaults.allDayHour)
        )
    }

    companion object {
        const val SETTINGS_PREFERENCES = "settings"
    }
}
