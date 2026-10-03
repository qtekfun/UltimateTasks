// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

/** The four smart lists of the home screen (RF-02). */
enum class SmartList { TODAY, SCHEDULED, ALL, COMPLETED }

/** What a task screen shows: one list, or a smart list across the visible ones. */
sealed interface TaskSource {
    data class List(val href: String) : TaskSource

    data class Smart(val kind: SmartList) : TaskSource
}
