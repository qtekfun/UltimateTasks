// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.lists

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Shows why a list change did not happen (no connection, server refused). */
@Composable
fun ListMessages(viewModel: ListsViewModel, snackbar: SnackbarHostState) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    LaunchedEffect(message) {
        message?.let {
            viewModel.messageShown()
            snackbar.showSnackbar(resources.getString(it))
        }
    }
}
