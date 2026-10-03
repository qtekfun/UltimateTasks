// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.settings

/** Preferences of this device (RF-14). More come with the full settings screen (T22). */
data class AppSettings(
    /** Aggressive mode: reminders are set like an alarm clock, which no battery saver delays. */
    val alarmClock: Boolean = false,
    /** When all-day tasks remind (RF-10), as an hour of the day. */
    val allDayHour: Int = DEFAULT_ALL_DAY_HOUR
) {
    companion object {
        const val DEFAULT_ALL_DAY_HOUR = 9
    }
}
