// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.lists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.ui.detail.DetailCard

/** The order of "My lists" (RF-08), with up and down buttons that also work with TalkBack. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderListsScreen(onBack: () -> Unit, viewModel: ListsViewModel = viewModel()) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val visible = lists.filter { it.visible }
    val order = visible.map { it.href }
    val snackbar = remember { SnackbarHostState() }
    ListMessages(viewModel, snackbar)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lists_reorder)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { padding ->
        Column(
            Modifier.padding(
                padding
            ).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            DetailCard {
                visible.forEachIndexed { index, list ->
                    ReorderRow(
                        list.name,
                        up = index > 0,
                        down = index < visible.lastIndex
                    ) { step ->
                        viewModel.move(order, index, step)
                    }
                    if (index <
                        visible.lastIndex
                    ) {
                        HorizontalDivider(Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderRow(name: String, up: Boolean, down: Boolean, onMove: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, Modifier.weight(1f))
        IconButton(onClick = {
            onMove(-1)
        }, enabled = up) {
            Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.move_up, name))
        }
        IconButton(onClick = {
            onMove(1)
        }, enabled = down) {
            Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.move_down, name))
        }
    }
}
