// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.home

import com.qtekfun.ultimatetasks.domain.task.TaskSource

/** Where the home screen leads. */
class HomeActions(
    val onOpen: (TaskSource) -> Unit,
    val onSettings: () -> Unit,
    /** Opens a list ready to type a new task in it. */
    val onNewTask: (String) -> Unit,
    val onReorder: () -> Unit,
    /** Opens a task found by the search. */
    val onOpenTask: (Long) -> Unit
)
