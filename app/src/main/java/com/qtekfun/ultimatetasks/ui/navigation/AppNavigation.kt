// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.qtekfun.ultimatetasks.domain.task.SmartList
import com.qtekfun.ultimatetasks.domain.task.TaskSource
import com.qtekfun.ultimatetasks.ui.detail.TaskDetailScreen
import com.qtekfun.ultimatetasks.ui.home.HomeActions
import com.qtekfun.ultimatetasks.ui.home.HomeScreen
import com.qtekfun.ultimatetasks.ui.list.TaskListScreen
import com.qtekfun.ultimatetasks.ui.lists.ReorderListsScreen
import com.qtekfun.ultimatetasks.ui.settings.SettingsScreen

/** Home, and the list or smart list opened from it; back returns home. */
@Composable
fun AppNavigation(
    accountName: String,
    onLogOut: () -> Unit,
    link: Long?,
    onLinkOpened: () -> Unit
) {
    // Saved as text so it survives rotation and process death.
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    var task by rememberSaveable { mutableStateOf<Long?>(null) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var reorder by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(link) {
        if (link != null) {
            task = link
            onLinkOpened()
        }
    }
    val source = opened?.let(::decode)
    val openTask = task
    when {
        settings -> {
            BackHandler { settings = false }
            SettingsScreen(accountName = accountName, onLogOut = onLogOut, onBack = {
                settings =
                    false
            })
        }

        reorder -> {
            BackHandler { reorder = false }
            ReorderListsScreen(onBack = { reorder = false })
        }

        openTask != null -> {
            BackHandler { task = null }
            TaskDetailScreen(taskId = openTask, onBack = { task = null })
        }

        source != null -> {
            BackHandler {
                opened = null
                adding = false
            }
            TaskListScreen(
                source = source,
                onBack = {
                    opened = null
                    adding = false
                },
                onOpenTask = { task = it },
                startAdding = adding
            )
        }

        else -> HomeScreen(
            HomeActions(
                onOpen = { opened = encode(it) },
                onSettings = { settings = true },
                onNewTask = {
                    adding = true
                    opened = encode(TaskSource.List(it))
                },
                onReorder = { reorder = true },
                onOpenTask = { task = it }
            )
        )
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
