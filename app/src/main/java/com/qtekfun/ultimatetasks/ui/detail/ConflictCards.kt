// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.task.ConflictChoice
import com.qtekfun.ultimatetasks.sync.conflict.TaskField

/** Nothing is overwritten silently (SPEC §5): the user picks a version, or keeps a copy. */
@Composable
fun ConflictCards(task: TaskEntity, viewModel: TaskDetailViewModel) {
    task.conflictSummary?.let { server ->
        TextConflict(R.string.field_title, task.summary, server) {
            viewModel.actions.resolve(TaskField.SUMMARY, it)
        }
    }
    task.conflictNotes?.let { server ->
        TextConflict(R.string.field_notes, task.notes, server) {
            viewModel.actions.resolve(TaskField.NOTES, it)
        }
    }
    if (task.deletedOnServer) {
        Warning(stringResource(R.string.deleted_on_server)) {
            OutlinedButton(onClick = viewModel.actions::keepCopy) {
                Text(stringResource(R.string.keep_copy))
            }
            OutlinedButton(onClick = viewModel.actions::discard) {
                Text(stringResource(R.string.discard))
            }
        }
    }
}

@Composable
private fun TextConflict(
    field: Int,
    mine: String,
    server: String,
    onChoose: (ConflictChoice) -> Unit
) {
    Warning(stringResource(R.string.conflict_text, stringResource(field), mine, server)) {
        OutlinedButton(onClick = {
            onChoose(ConflictChoice.MINE)
        }) { Text(stringResource(R.string.keep_mine)) }
        OutlinedButton(onClick = {
            onChoose(ConflictChoice.SERVER)
        }) { Text(stringResource(R.string.keep_server)) }
    }
}

@Composable
private fun Warning(text: String, actions: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.conflict_title), fontWeight = FontWeight.Bold)
            Text(text)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
        }
    }
}
