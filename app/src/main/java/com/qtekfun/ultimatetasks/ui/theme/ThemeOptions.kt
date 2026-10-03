// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.theme

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** What the user picks in Settings (T22); the defaults follow the system. */
data class ThemeOptions(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val amoled: Boolean = false,
    val dynamicColor: Boolean = true
)
