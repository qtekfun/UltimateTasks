// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.task.SmartList
import com.qtekfun.ultimatetasks.domain.task.TaskSource
import com.qtekfun.ultimatetasks.ui.components.RoundCheckbox
import com.qtekfun.ultimatetasks.ui.theme.ListColors

/** One list, or a smart list, as in Apple Reminders (RF-03, RF-04). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    source: TaskSource,
    onBack: () -> Unit,
    viewModel: TaskListViewModel = viewModel()
) {
    LaunchedEffect(source) { viewModel.show(source) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val color = colorOf(source, state.list?.color)
    var adding by rememberSaveable { mutableStateOf(false) }
    UndoSnackbar(state, snackbar, viewModel)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = { ListMenu(state.showCompleted, source, viewModel::toggleShowCompleted) }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (source is TaskSource.List &&
                state.list?.writable == true
            ) {
                NewTaskButton(color) { adding = true }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = syncing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            TaskListContent(state, source, adding, viewModel, onAddDone = { adding = false })
        }
    }
}

/** "Completed · Undo" after checking a task (RF-04). */
@Composable
private fun UndoSnackbar(
    state: TaskListState,
    snackbar: SnackbarHostState,
    viewModel: TaskListViewModel
) {
    val completedText = stringResource(R.string.task_completed)
    val undoText = stringResource(R.string.undo)
    LaunchedEffect(state.lastCompleted) {
        if (state.lastCompleted != null) {
            val result = snackbar.showSnackbar(
                completedText,
                undoText,
                duration = SnackbarDuration.Short
            )
            if (result ==
                SnackbarResult.ActionPerformed
            ) {
                viewModel.undo()
            } else {
                viewModel.dismissUndo()
            }
        }
    }
}

@Composable
private fun NewTaskButton(color: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(8.dp)) {
        Icon(Icons.Default.Add, contentDescription = null, tint = color)
        Text(stringResource(R.string.new_task), color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TaskListContent(
    state: TaskListState,
    source: TaskSource,
    adding: Boolean,
    viewModel: TaskListViewModel,
    onAddDone: () -> Unit
) {
    val color = colorOf(source, state.list?.color)
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                text = titleOf(source, state.list?.name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp).semantics {
                    heading()
                }
            )
        }
        items(state.tasks, key = { it.id }) { task ->
            val list = state.lists[task.listHref]
            TaskRow(
                task = task,
                color = if (source is TaskSource.List) color else ListColors.of(list?.color),
                listName = if (source is TaskSource.List) null else list?.name,
                // Completing a repeating task must schedule its next time: that comes with T10.
                writable = (list?.writable ?: false) && task.recurrence == null,
                onCheckedChange = { viewModel.setCompleted(task, it) }
            )
        }
        if (adding) {
            item(key = "new") { NewTaskRow(color, onAdd = viewModel::add, onDone = onAddDone) }
        }
        if (state.tasks.isEmpty() && !adding) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_tasks),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Typing a title and pressing Enter adds it and opens a new empty row, as in Apple Reminders. */
@Composable
private fun NewTaskRow(color: Color, onAdd: (String) -> Unit, onDone: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RoundCheckbox(
            checked = false,
            color = color,
            description = stringResource(R.string.new_task),
            onCheckedChange = null
        )
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.new_task)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                if (text.isBlank()) {
                    onDone()
                } else {
                    onAdd(text)
                    text = ""
                }
            }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.weight(1f).focusRequester(focus)
        )
    }
}

@Composable
private fun ListMenu(showCompleted: Boolean, source: TaskSource, onToggle: () -> Unit) {
    if (source == TaskSource.Smart(SmartList.COMPLETED)) return
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = {
        open = true
    }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options)) }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(
                        if (showCompleted) R.string.hide_completed else R.string.show_completed
                    )
                )
            },
            onClick = {
                open = false
                onToggle()
            }
        )
    }
}

@Composable
private fun titleOf(source: TaskSource, listName: String?): String = when (source) {
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

private fun colorOf(source: TaskSource, listColor: String?): Color = when (source) {
    is TaskSource.List -> ListColors.of(listColor)
    is TaskSource.Smart -> smartColor(source.kind)
}
