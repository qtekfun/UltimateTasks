// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.settings

/** Preferences of this device (RF-14). */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Pure black backgrounds in dark mode, for OLED screens. */
    val amoled: Boolean = false,
    /** Material You colors from the wallpaper (Android 12+). */
    val dynamicColor: Boolean = true,
    /** Where new tasks go when no list is open (RF-14); null: the first list that can be written. */
    val defaultList: String? = null,
    /** Deleting lists is hidden until turned on, so it is never done by accident (RF-08). */
    val allowDeletingLists: Boolean = false,
    /** Aggressive mode: reminders are set like an alarm clock, which no battery saver delays. */
    val alarmClock: Boolean = false,
    /** When all-day tasks remind (RF-10), as an hour of the day. */
    val allDayHour: Int = DEFAULT_ALL_DAY_HOUR
) {
    companion object {
        const val DEFAULT_ALL_DAY_HOUR = 9
    }
}
