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
    /**
     * Robust mode: a foreground service keeps the process alive on phones that kill apps in the
     * background, so their alarms are not lost. Off by default: it shows a fixed notification.
     */
    val robustMode: Boolean = false,
    /** When all-day tasks remind (RF-10), as an hour of the day. */
    val allDayHour: Int = DEFAULT_ALL_DAY_HOUR,
    /** How far back reminders the system kept from showing are brought back (T32); 0: never. */
    val missedWindowHours: Int = DEFAULT_MISSED_WINDOW_HOURS
) {
    companion object {
        const val DEFAULT_ALL_DAY_HOUR = 9
        const val DEFAULT_MISSED_WINDOW_HOURS = 24

        /** The choices offered for [missedWindowHours]. */
        val MISSED_WINDOW_CHOICES = listOf(6, 24, 48, 0)
    }
}
