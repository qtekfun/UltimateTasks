// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.list

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.task.TaskSort

/** "Sort by" choices and, in manual order of a writable list, "Reorder" (RF-03). */
@Composable
fun SortMenuItems(ordering: ListOrdering, writable: Boolean, closeMenu: () -> Unit) {
    val sort by ordering.sort.collectAsStateWithLifecycle()
    val reordering by ordering.reordering.collectAsStateWithLifecycle()
    HorizontalDivider()
    TaskSort.entries.forEach { option ->
        DropdownMenuItem(
            text = { Text(stringResource(sortName(option))) },
            trailingIcon = {
                if (option ==
                    sort
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            },
            onClick = {
                closeMenu()
                ordering.setSort(option)
            }
        )
    }
    if (writable && sort == TaskSort.MANUAL) {
        DropdownMenuItem(
            text = {
                Text(stringResource(if (reordering) R.string.reorder_done else R.string.reorder))
            },
            onClick = {
                closeMenu()
                ordering.toggleReordering()
            }
        )
    }
    HorizontalDivider()
}

private fun sortName(sort: TaskSort): Int = when (sort) {
    TaskSort.MANUAL -> R.string.sort_manual
    TaskSort.DUE -> R.string.sort_due
    TaskSort.PRIORITY -> R.string.sort_priority
    TaskSort.TITLE -> R.string.sort_title
}
