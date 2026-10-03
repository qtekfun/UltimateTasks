// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.model

/** An open task with a due date in a visible list, as the reminders need it (RF-10). */
data class DueTaskRow(
    val id: Long,
    val summary: String,
    val listName: String,
    val due: String,
    val dueZone: String?,
    val reminderBefore: Long?
)
