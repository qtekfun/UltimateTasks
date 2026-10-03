// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.list

/** What a row does: [onCheckedChange] is null when the task cannot be checked here. */
class TaskRowActions(
    val onCheckedChange: ((Boolean) -> Unit)?,
    val onClick: () -> Unit,
    /** Folds or unfolds the subtasks; null for rows without them. */
    val onToggleChildren: (() -> Unit)? = null
)
