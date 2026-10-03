// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.lists

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.task.ListLook

/**
 * "Edit list" and, when allowed in Settings, "Delete list" for the list menu (RF-08).
 * [closeMenu] closes the menu they are in; [onDeleted] leaves the deleted list.
 */
@Composable
fun ListMenuItems(
    list: TaskListEntity,
    taskCount: Int,
    viewModel: ListsViewModel,
    closeMenu: () -> Unit,
    onDeleted: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    if (!list.writable) return
    DropdownMenuItem(text = {
        Text(stringResource(R.string.list_edit))
    }, onClick = { editing = true })
    if (settings.allowDeletingLists) {
        DropdownMenuItem(
            text = {
                Text(stringResource(R.string.list_delete), color = MaterialTheme.colorScheme.error)
            },
            onClick = { deleting = true }
        )
    }
    if (editing) {
        ListEditorDialog(
            initial = ListLook(list.name, list.color, list.icon),
            onSave = {
                editing = false
                closeMenu()
                viewModel.edit(list.href, it)
            },
            onDismiss = {
                editing = false
                closeMenu()
            }
        )
    }
    if (deleting) {
        DeleteListDialog(list.name, taskCount) { confirmed ->
            deleting = false
            closeMenu()
            if (confirmed) viewModel.delete(list.href, onDeleted)
        }
    }
}

/** "Delete <list>? Its N tasks will be deleted too." [onResult] gets true to delete. */
@Composable
private fun DeleteListDialog(name: String, taskCount: Int, onResult: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onResult(false) },
        title = { Text(stringResource(R.string.list_delete_confirm, name)) },
        text = { Text(pluralStringResource(R.plurals.list_delete_tasks, taskCount, taskCount)) },
        confirmButton = {
            TextButton(onClick = { onResult(true) }) {
                Text(stringResource(R.string.list_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { onResult(false) }) { Text(stringResource(R.string.cancel)) }
        }
    )
}
