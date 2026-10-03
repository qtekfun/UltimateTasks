// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

private const val HIGHEST = 1
private const val HIGH_UNTIL = 4
private const val MEDIUM = 5
private const val LOWEST = 9

/** `!!!` high, `!!` medium, `!` low (PRIORITY 1–4, 5, 6–9), as Apple shows them (RF-03). */
fun priorityMarks(priority: Int): String = when (priority) {
    in HIGHEST..HIGH_UNTIL -> "!!!"
    MEDIUM -> "!!"
    in MEDIUM + 1..LOWEST -> "!"
    else -> ""
}
