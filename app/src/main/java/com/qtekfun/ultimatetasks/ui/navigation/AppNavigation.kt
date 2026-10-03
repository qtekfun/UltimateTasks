// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.qtekfun.ultimatetasks.data.settings.OnboardingPrefs
import com.qtekfun.ultimatetasks.domain.task.TaskSource
import com.qtekfun.ultimatetasks.ui.detail.TaskDetailScreen
import com.qtekfun.ultimatetasks.ui.home.HomeActions
import com.qtekfun.ultimatetasks.ui.home.HomeScreen
import com.qtekfun.ultimatetasks.ui.list.TaskListScreen
import com.qtekfun.ultimatetasks.ui.lists.ReorderListsScreen
import com.qtekfun.ultimatetasks.ui.settings.ReliabilityWizardScreen
import com.qtekfun.ultimatetasks.ui.settings.SettingsScreen

/**
 * Home and what opens from it; back returns home. The reminders wizard shows once after
 * signing in (RF-01), then from Settings.
 */
@Composable
fun AppNavigation(
    accountName: String,
    onLogOut: () -> Unit,
    link: Long?,
    onLinkOpened: () -> Unit,
    onboarding: OnboardingPrefs
) {
    val nav =
        rememberSaveable(saver = NavState.Saver) { NavState(wizard = !onboarding.wizardShown()) }
    LaunchedEffect(link) {
        if (link != null) {
            nav.task = link
            onLinkOpened()
        }
    }
    val source = nav.opened
    val task = nav.task
    when {
        nav.wizard -> ReliabilityWizardScreen(onDone = {
            onboarding.markWizardShown()
            nav.wizard = false
        })

        nav.settings -> {
            BackHandler { nav.settings = false }
            SettingsScreen(accountName, onLogOut, onBack = { nav.settings = false }, onWizard = {
                nav.wizard =
                    true
            })
        }

        nav.reorder -> {
            BackHandler { nav.reorder = false }
            ReorderListsScreen(onBack = { nav.reorder = false })
        }

        task != null -> {
            BackHandler { nav.task = null }
            TaskDetailScreen(taskId = task, onBack = { nav.task = null })
        }

        source != null -> {
            BackHandler { nav.closeList() }
            TaskListScreen(source, onBack = nav::closeList, onOpenTask = {
                nav.task = it
            }, startAdding = nav.adding)
        }

        else -> HomeScreen(homeActions(nav))
    }
}

private fun homeActions(nav: NavState) = HomeActions(
    onOpen = { nav.opened = it },
    onSettings = { nav.settings = true },
    onNewTask = {
        nav.adding = true
        nav.opened = TaskSource.List(it)
    },
    onReorder = { nav.reorder = true },
    onOpenTask = { nav.task = it }
)
