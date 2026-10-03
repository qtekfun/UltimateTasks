// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.theme

/** Drawing of the round checkbox. */
object CheckboxTokens {
    /** The tick, as fractions of the checkbox size. */
    val tick = listOf(0.3f to 0.52f, 0.45f to 0.66f, 0.71f to 0.38f)

    /** Read-only tasks show a faint fill: they cannot be checked here. */
    const val DISABLED_ALPHA = 0.2f
}
