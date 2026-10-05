// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.settings

/** The on/off settings, each with its key in the preferences. */
enum class SettingFlag(val key: String) {
    AMOLED("amoled"),
    DYNAMIC_COLOR("dynamic_color"),
    ALLOW_DELETING_LISTS("allow_deleting_lists"),
    ALARM_CLOCK("alarm_clock"),
    ROBUST_MODE("robust_mode")
}
