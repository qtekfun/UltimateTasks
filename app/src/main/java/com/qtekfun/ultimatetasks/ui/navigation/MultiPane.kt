// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.ui.detail.TaskDetailScreen
import com.qtekfun.ultimatetasks.ui.home.HomeScreen
import com.qtekfun.ultimatetasks.ui.list.TaskListScreen

private val SIDEBAR = 360.dp
private val DETAIL = 420.dp

/**
 * Home always on the left. With two panes the task replaces the list on the right; with three
 * it opens beside it. Back closes the rightmost thing open.
 */
@Composable
fun MultiPane(nav: NavState, panes: Panes) {
    val source = nav.opened
    val task = nav.task
    val safe = WindowInsets.safeDrawing
    BackHandler(enabled = task != null || source != null) {
        if (task != null) nav.task = null else nav.closeList()
    }
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.width(SIDEBAR).fillMaxHeight().inner(safe, WindowInsetsSides.End)) {
            HomeScreen(homeActions(nav))
        }
        VerticalDivider()
        Box(Modifier.weight(1f).fillMaxHeight().inner(safe, middleSides(panes, task != null))) {
            when {
                task != null && panes == Panes.TWO -> Detail(nav, task)

                source != null -> TaskListScreen(
                    source,
                    onBack = nav::closeList,
                    onOpenTask = { nav.task = it },
                    startAdding = nav.adding
                )

                else -> Placeholder()
            }
        }
        if (task != null && panes == Panes.THREE) {
            VerticalDivider()
            Box(Modifier.width(DETAIL).fillMaxHeight().inner(safe, WindowInsetsSides.Start)) {
                Detail(nav, task)
            }
        }
    }
}

/**
 * Only the outer panes touch the screen edges: the sides that face another pane drop the
 * cutout and navigation bar insets, so no pane is padded for a camera it does not reach.
 */
private fun Modifier.inner(safe: WindowInsets, sides: WindowInsetsSides) =
    consumeWindowInsets(safe.only(sides))

private fun middleSides(panes: Panes, detailOpen: Boolean) = if (panes == Panes.THREE &&
    detailOpen
) {
    WindowInsetsSides.Horizontal
} else {
    WindowInsetsSides.Start
}

@Composable
private fun Detail(nav: NavState, task: Long) {
    TaskDetailScreen(taskId = task, onBack = { nav.task = null })
}

@Composable
private fun Placeholder() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.panes_choose_list),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
