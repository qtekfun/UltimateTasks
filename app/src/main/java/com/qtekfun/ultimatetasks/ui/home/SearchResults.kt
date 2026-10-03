// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.task.TaskRowItem
import com.qtekfun.ultimatetasks.ui.list.TaskRow
import com.qtekfun.ultimatetasks.ui.list.TaskRowActions
import com.qtekfun.ultimatetasks.ui.theme.ListColors

/** The search bar at the top of the home screen, as in Apple Reminders (RF-12). */
@Composable
fun SearchBar(viewModel: HomeViewModel) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    TextField(
        value = query,
        onValueChange = viewModel::search,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = {
                    viewModel.search("")
                }) { Icon(Icons.Default.Clear, stringResource(R.string.search_clear)) }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    )
}

/** Matches by list, each with the list it is in; tapping one opens it. */
@Composable
fun SearchResults(viewModel: HomeViewModel, onOpenTask: (Long) -> Unit) {
    val results by viewModel.results.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val includeCompleted by viewModel.includeCompleted.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = includeCompleted,
                    onClick = { viewModel.setIncludeCompleted(!includeCompleted) },
                    label = { Text(stringResource(R.string.search_include_completed)) }
                )
            }
        }
        items(results, key = { it.id }) { task ->
            val list = lists[task.listHref]
            TaskRow(
                row = TaskRowItem(task, depth = 0, children = 0),
                color = ListColors.of(list?.color),
                listName = list?.name,
                actions = TaskRowActions(onCheckedChange = null, onClick = { onOpenTask(task.id) })
            )
        }
        if (results.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.search_no_results),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
