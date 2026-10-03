// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.qtekfun.ultimatetasks.domain.task.SmartList
import com.qtekfun.ultimatetasks.domain.task.TaskSource
import com.qtekfun.ultimatetasks.ui.detail.TaskDetailScreen
import com.qtekfun.ultimatetasks.ui.home.HomeScreen
import com.qtekfun.ultimatetasks.ui.list.TaskListScreen

/** Home, and the list or smart list opened from it; back returns home. */
@Composable
fun AppNavigation(accountName: String, onLogOut: () -> Unit) {
    // Saved as text so it survives rotation and process death.
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    var task by rememberSaveable { mutableStateOf<Long?>(null) }
    val source = opened?.let(::decode)
    val openTask = task
    when {
        openTask != null -> {
            BackHandler { task = null }
            TaskDetailScreen(taskId = openTask, onBack = { task = null })
        }

        source != null -> {
            BackHandler { opened = null }
            TaskListScreen(source = source, onBack = { opened = null }, onOpenTask = { task = it })
        }

        else -> HomeScreen(accountName = accountName, onOpen = {
            opened = encode(it)
        }, onLogOut = onLogOut)
    }
}

private fun encode(source: TaskSource): String = when (source) {
    is TaskSource.List -> "list:${source.href}"
    is TaskSource.Smart -> "smart:${source.kind.name}"
}

private fun decode(text: String): TaskSource? = when {
    text.startsWith("list:") -> TaskSource.List(text.removePrefix("list:"))

    text.startsWith("smart:") -> SmartList.entries.firstOrNull {
        it.name ==
            text.removePrefix("smart:")
    }?.let { TaskSource.Smart(it) }

    else -> null
}
