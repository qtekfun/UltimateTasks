// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.navigation

/**
 * How many panes fit side by side (RF-15), as Reminders on iPad: lists, tasks, then the task.
 * The limits are Material's window size classes: medium from 600 dp, large from 1200 dp.
 */
enum class Panes {
    ONE,
    TWO,
    THREE;

    companion object {
        private const val MEDIUM_DP = 600
        private const val LARGE_DP = 1200

        fun forWidth(widthDp: Int): Panes = when {
            widthDp >= LARGE_DP -> THREE
            widthDp >= MEDIUM_DP -> TWO
            else -> ONE
        }
    }
}
