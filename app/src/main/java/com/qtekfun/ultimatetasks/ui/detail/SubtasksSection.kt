// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.detail

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.ui.components.RoundCheckbox
import com.qtekfun.ultimatetasks.ui.theme.ListColors

/** The subtasks of the task, checkable, and a field to add more (RF-07). */
@Composable
fun SubtasksSection(task: TaskEntity, state: TaskDetailState, viewModel: TaskDetailViewModel) {
    val children by viewModel.children.collectAsStateWithLifecycle()
    val color = ListColors.of(state.list?.color)
    if (children.isEmpty() && !state.editable) return
    DetailCard {
        Text(stringResource(R.string.field_subtasks), Modifier.padding(start = 16.dp, top = 12.dp))
        children.forEach { child ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundCheckbox(
                    checked = child.completed,
                    color = color,
                    description = stringResource(
                        if (child.completed) R.string.task_mark_open else R.string.task_mark_done,
                        child.summary
                    ),
                    onCheckedChange = { done: Boolean ->
                        viewModel.actions.setChildCompleted(child, done)
                    }.takeIf { state.editable }
                )
                Text(child.summary, Modifier.weight(1f))
            }
            HorizontalDivider(Modifier.padding(start = 48.dp))
        }
        if (state.editable) NewSubtask { viewModel.actions.addSubtask(task, it) }
    }
}

@Composable
private fun NewSubtask(onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        placeholder = { Text(stringResource(R.string.add_subtask)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            if (text.isNotBlank()) onAdd(text)
            text = ""
        }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
