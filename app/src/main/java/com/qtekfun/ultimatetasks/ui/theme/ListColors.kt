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
}
