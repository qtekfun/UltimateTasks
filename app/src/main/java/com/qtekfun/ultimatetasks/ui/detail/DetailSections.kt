// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity

/** A white rounded group of rows, as in iOS settings-like screens. */
@Composable
fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun plainColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent
)

/** Title, notes and URL; the text is kept here while typing so the cursor never jumps. */
@Composable
fun TextSection(task: TaskEntity, editable: Boolean, viewModel: TaskDetailViewModel) {
    var title by rememberSaveable(task.id) { mutableStateOf(task.summary) }
    var notes by rememberSaveable(task.id) { mutableStateOf(task.notes) }
    var url by rememberSaveable(task.id) { mutableStateOf(task.url.orEmpty()) }
    DetailCard {
        TextField(
            value = title,
            onValueChange = {
                title = it
                viewModel.setSummary(it)
            },
            enabled = editable,
            placeholder = { Text(stringResource(R.string.field_title)) },
            textStyle = MaterialTheme.typography.titleMedium,
            colors = plainColors(),
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider(Modifier.padding(start = 16.dp))
        TextField(
            value = notes,
            onValueChange = {
                notes = it
                viewModel.setNotes(it)
            },
            enabled = editable,
            placeholder = { Text(stringResource(R.string.field_notes)) },
            colors = plainColors(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)
        )
        HorizontalDivider(Modifier.padding(start = 16.dp))
        TextField(
            value = url,
            onValueChange = {
                url = it
                viewModel.setUrl(it)
            },
            enabled = editable,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.field_url)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = plainColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Priority and tags (RF-05). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsSection(task: TaskEntity, editable: Boolean, viewModel: TaskDetailViewModel) {
    DetailCard {
        Text(stringResource(R.string.field_priority), Modifier.padding(start = 16.dp, top = 12.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(12.dp)) {
            PRIORITIES.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = priorityGroup(task.priority) == value,
                    onClick = { viewModel.edit { it.copy(priority = value) } },
                    enabled = editable,
                    shape = SegmentedButtonDefaults.itemShape(index, PRIORITIES.size)
                ) { Text(stringResource(label)) }
            }
        }
        HorizontalDivider(Modifier.padding(start = 16.dp))
        Text(stringResource(R.string.field_tags), Modifier.padding(start = 16.dp, top = 12.dp))
        FlowRow(
            Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            task.tags.forEach { tag ->
                InputChip(
                    selected = false,
                    onClick = { if (editable) viewModel.edit { it.copy(tags = it.tags - tag) } },
                    label = { Text("#$tag") },
                    enabled = editable,
                    trailingIcon = {
                        Icon(Icons.Default.Close, stringResource(R.string.remove_tag, tag))
                    }
                )
            }
        }
        if (editable) {
            NewTag { tag ->
                viewModel.edit { it.copy(tags = (it.tags + tag).distinct()) }
            }
        }
    }
}

@Composable
private fun NewTag(onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it.replace(",", "") },
        singleLine = true,
        placeholder = { Text(stringResource(R.string.add_tag)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onAdd(text.trim().removePrefix("#"))
            text = ""
        }),
        colors = plainColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Apple's four levels on PRIORITY: none 0, low 9, medium 5, high 1. */
private val PRIORITIES = listOf(
    0 to R.string.priority_none,
    PRIORITY_LOW to R.string.priority_low,
    PRIORITY_MEDIUM to R.string.priority_medium,
    PRIORITY_HIGH to R.string.priority_high
)

private const val PRIORITY_HIGH = 1
private const val PRIORITY_MEDIUM = 5
private const val PRIORITY_LOW = 9

/** Values written by other clients fall into the nearest of the four levels. */
private fun priorityGroup(priority: Int): Int = when (priority) {
    0 -> 0
    in PRIORITY_HIGH until PRIORITY_MEDIUM -> PRIORITY_HIGH
    PRIORITY_MEDIUM -> PRIORITY_MEDIUM
    else -> PRIORITY_LOW
}

/** The list the task is in; changing it moves the task (RF-05). */
@Composable
fun ListSection(state: TaskDetailState, viewModel: TaskDetailViewModel) {
    var open by remember { mutableStateOf(false) }
    DetailCard {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .clickable(enabled = state.editable, role = Role.DropdownList) { open = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.field_list), Modifier.weight(1f))
            Text(state.list?.name.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                state.lists.forEach { list ->
                    DropdownMenuItem(
                        text = { Text(list.name) },
                        onClick = {
                            open = false
                            viewModel.actions.move(list.href)
                        }
                    )
                }
            }
        }
        if (!state.editable) {
            Text(
                stringResource(R.string.read_only_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
        }
    }
}
