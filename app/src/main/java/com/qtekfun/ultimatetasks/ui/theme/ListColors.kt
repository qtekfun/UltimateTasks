// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.theme

import androidx.compose.ui.graphics.Color

/** Colors of Apple Reminders, used for the smart lists and for lists without their own. */
object ListColors {
    val Blue = Color(0xFF007AFF)
    val Red = Color(0xFFFF3B30)
    val Graphite = Color(0xFF5B5B60)
    val Gray = Color(0xFF8E8E93)

    /** A `#RRGGBB` list color, or blue when the list has none. */
    fun of(hex: String?): Color = hex?.removePrefix("#")?.takeIf {
        it.length == 6
    }?.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
        ?: Blue

    /** New lists start blue, as in Apple Reminders. */
    const val DEFAULT = "#007AFF"

    /** The palette of Apple Reminders, as `#RRGGBB` for the server (RF-08). */
    val palette = listOf(
        "#FF3B30", "#FF9500", "#FFCC00", "#34C759", "#5AC8FA", "#007AFF",
        "#5856D6", "#AF52DE", "#FF2D55", "#A2845E", "#8E8E93"
    )
}
