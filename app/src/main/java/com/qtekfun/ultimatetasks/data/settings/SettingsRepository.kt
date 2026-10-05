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
private const val KEY_DEFAULT_LIST = "default_list"
private const val KEY_ALL_DAY_HOUR = "all_day_hour"
private const val KEY_MISSED_WINDOW = "missed_window_hours"
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

    fun setFlag(flag: SettingFlag, on: Boolean) = preferences.edit { putBoolean(flag.key, on) }

    fun setDefaultList(href: String?) = preferences.edit {
        if (href == null) remove(KEY_DEFAULT_LIST) else putString(KEY_DEFAULT_LIST, href)
    }

    fun setAllDayHour(hour: Int) = preferences.edit {
        putInt(KEY_ALL_DAY_HOUR, hour.coerceIn(0, LAST_HOUR))
    }

    fun setMissedWindowHours(hours: Int) = preferences.edit {
        putInt(KEY_MISSED_WINDOW, hours.coerceAtLeast(0))
    }

    /** Applies every setting at once, from a backup (T23). */
    fun restore(restored: AppSettings) = preferences.edit {
        putString(KEY_THEME, restored.theme.name)
        putBoolean(SettingFlag.AMOLED.key, restored.amoled)
        putBoolean(SettingFlag.DYNAMIC_COLOR.key, restored.dynamicColor)
        if (restored.defaultList ==
            null
        ) {
            remove(KEY_DEFAULT_LIST)
        } else {
            putString(KEY_DEFAULT_LIST, restored.defaultList)
        }
        putBoolean(SettingFlag.ALLOW_DELETING_LISTS.key, restored.allowDeletingLists)
        putBoolean(SettingFlag.ALARM_CLOCK.key, restored.alarmClock)
        putBoolean(SettingFlag.ROBUST_MODE.key, restored.robustMode)
        putInt(KEY_ALL_DAY_HOUR, restored.allDayHour.coerceIn(0, LAST_HOUR))
        putInt(KEY_MISSED_WINDOW, restored.missedWindowHours.coerceAtLeast(0))
    }

    private fun read(): AppSettings {
        val defaults = AppSettings()
        val theme = preferences.getString(KEY_THEME, null)
        return AppSettings(
            theme = ThemeMode.entries.firstOrNull { it.name == theme } ?: defaults.theme,
            amoled = preferences.getBoolean(SettingFlag.AMOLED.key, defaults.amoled),
            dynamicColor = preferences.getBoolean(
                SettingFlag.DYNAMIC_COLOR.key,
                defaults.dynamicColor
            ),
            defaultList = preferences.getString(KEY_DEFAULT_LIST, null),
            allowDeletingLists = preferences.getBoolean(
                SettingFlag.ALLOW_DELETING_LISTS.key,
                defaults.allowDeletingLists
            ),
            alarmClock = preferences.getBoolean(SettingFlag.ALARM_CLOCK.key, defaults.alarmClock),
            robustMode = preferences.getBoolean(SettingFlag.ROBUST_MODE.key, defaults.robustMode),
            allDayHour = preferences.getInt(KEY_ALL_DAY_HOUR, defaults.allDayHour),
            missedWindowHours = preferences.getInt(KEY_MISSED_WINDOW, defaults.missedWindowHours)
        )
    }

    companion object {
        const val SETTINGS_PREFERENCES = "settings"
    }
}
