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

private const val KEY_ALARM_CLOCK = "alarm_clock"
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

    fun setAlarmClock(on: Boolean) = preferences.edit { putBoolean(KEY_ALARM_CLOCK, on) }

    fun setAllDayHour(hour: Int) = preferences.edit {
        putInt(KEY_ALL_DAY_HOUR, hour.coerceIn(0, LAST_HOUR))
    }

    private fun read(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            alarmClock = preferences.getBoolean(KEY_ALARM_CLOCK, defaults.alarmClock),
            allDayHour = preferences.getInt(KEY_ALL_DAY_HOUR, defaults.allDayHour)
        )
    }

    companion object {
        const val SETTINGS_PREFERENCES = "settings"
    }
}
