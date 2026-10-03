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
import androidx.compose.runtime.MutableState
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
import com.qtekfun.ultimatetasks.domain.recurrence.RepeatingTasks
import com.qtekfun.ultimatetasks.domain.task.GroupKey
import com.qtekfun.ultimatetasks.domain.task.SmartList
import com.qtekfun.ultimatetasks.domain.task.Subtasks
import com.qtekfun.ultimatetasks.domain.task.TaskSource
import com.qtekfun.ultimatetasks.ui.components.RoundCheckbox
import com.qtekfun.ultimatetasks.ui.lists.ListMenuItems
import com.qtekfun.ultimatetasks.ui.lists.ListMessages
import com.qtekfun.ultimatetasks.ui.lists.ListsViewModel
import com.qtekfun.ultimatetasks.ui.theme.ListColors

/** One list, or a smart list, as in Apple Reminders (RF-03, RF-04). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    source: TaskSource,
    onBack: () -> Unit,
    onOpenTask: (Long) -> Unit,
    startAdding: Boolean = false,
    viewModel: TaskListViewModel = viewModel(),
    lists: ListsViewModel = viewModel()
) {
    LaunchedEffect(source) { viewModel.show(source) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val color = colorOf(source, state.list?.color)
    val adding = rememberSaveable { mutableStateOf(startAdding) }
    UndoSnackbar(state, snackbar, viewModel)
    ListMessages(lists, snackbar)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    ListMenu(state, source, viewModel::toggleShowCompleted) { menu ->
                        SortMenuItems(
                            viewModel.ordering,
                            writable = state.list?.writable == true,
                            closeMenu = menu
                        )
                        state.list?.let {
                            ListMenuItems(
                                it,
                                state.tasks.count { t ->
                                    !t.completed
                                },
                                lists,
                                menu,
                                onBack
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (source is TaskSource.List &&
                state.list?.writable == true
            ) {
                NewTaskButton(color) { adding.value = true }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = syncing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            TaskListContent(state, source, adding, viewModel, onOpenTask)
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
    adding: MutableState<Boolean>,
    viewModel: TaskListViewModel,
    onOpenTask: (Long) -> Unit
) {
    val color = colorOf(source, state.list?.color)
    val reordering by viewModel.ordering.reordering.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize()) {
        item { ListTitle(titleOf(source, state.list?.name)) }
        state.groups.forEach { group ->
            if (group.key != GroupKey.None) {
                item(key = "header:${group.key}") { GroupHeader(group.key, state) }
            }
            val rows = Subtasks.arrange(group.tasks, state.collapsed)
            val topLevel = rows.filter { it.depth == 0 }.map { it.task }
            items(rows, key = { it.task.id }) { row ->
                val task = row.task
                val list = state.lists[task.listHref]
                TaskRow(
                    row = row,
                    color = if (source is TaskSource.List) color else ListColors.of(list?.color),
                    // Grouped by list, the list name is already the header.
                    listName = list?.name.takeIf {
                        source !is TaskSource.List &&
                            group.key !is GroupKey.InList
                    },
                    actions = TaskRowActions(
                        // Repeating rules the app cannot follow are left to other clients.
                        onCheckedChange = { done: Boolean -> viewModel.setCompleted(task, done) }
                            .takeIf {
                                list?.writable == true &&
                                    RepeatingTasks.understood(task.recurrence)
                            },
                        onClick = { onOpenTask(task.id) },
                        onToggleChildren = { viewModel.toggleCollapsed(task.uid) }.takeIf {
                            row.children >
                                0
                        },
                        onMove = { step: Int ->
                            viewModel.ordering.move(topLevel, topLevel.indexOf(task), step)
                        }
                            .takeIf { reordering && row.depth == 0 }
                    ),
                    collapsed = task.uid in state.collapsed
                )
            }
        }
        if (adding.value) {
            item(key = "new") {
                NewTaskRow(color, onAdd = viewModel::add, onDone = {
                    adding.value =
                        false
                })
            }
        }
        if (state.tasks.isEmpty() && !adding.value) item { EmptyTasks() }
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
private fun ListMenu(
    state: TaskListState,
    source: TaskSource,
    onToggle: () -> Unit,
    listItems: @Composable (closeMenu: () -> Unit) -> Unit
) {
    if (source == TaskSource.Smart(SmartList.COMPLETED)) return
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = {
        open = true
    }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options)) }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = {
                val label = when {
                    state.showCompleted -> R.string.hide_completed
                    else -> R.string.show_completed
                }
                Text(stringResource(label))
            },
            onClick = {
                open = false
                onToggle()
            }
        )
        if (source is TaskSource.List) {
            listItems { open = false }
        }
    }
}

@Composable
private fun EmptyTasks() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.no_tasks), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The big title of the list, in a neutral color (requested). */
@Composable
private fun ListTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp).semantics {
            heading()
        }
    )
}
