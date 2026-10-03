// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.task.SmartList
import com.qtekfun.ultimatetasks.domain.task.TaskSource
import com.qtekfun.ultimatetasks.ui.theme.ListColors

@Composable
fun titleOf(source: TaskSource, listName: String?): String = when (source) {
    is TaskSource.List -> listName.orEmpty()
    is TaskSource.Smart -> stringResource(smartName(source.kind))
}

fun smartName(kind: SmartList): Int = when (kind) {
    SmartList.TODAY -> R.string.smart_today
    SmartList.SCHEDULED -> R.string.smart_scheduled
    SmartList.ALL -> R.string.smart_all
    SmartList.COMPLETED -> R.string.smart_completed
}

fun smartColor(kind: SmartList): Color = when (kind) {
    SmartList.TODAY -> ListColors.Blue
    SmartList.SCHEDULED -> ListColors.Red
    SmartList.ALL -> ListColors.Graphite
    SmartList.COMPLETED -> ListColors.Gray
}

fun colorOf(source: TaskSource, listColor: String?): Color = when (source) {
    is TaskSource.List -> ListColors.of(listColor)
    is TaskSource.Smart -> smartColor(source.kind)
}
