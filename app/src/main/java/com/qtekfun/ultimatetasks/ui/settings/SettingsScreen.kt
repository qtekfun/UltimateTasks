// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatetasks.BuildConfig
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.ui.detail.DetailCard

/** Settings (RF-14): lists, appearance, reminders and the account; the version at the foot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    accountName: String,
    onLogOut: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val lists by viewModel.taskLists.collectAsStateWithLifecycle()
    var visibleLists by rememberSaveable { mutableStateOf(false) }
    if (visibleLists) {
        BackHandler { visibleLists = false }
        VisibleListsScreen(viewModel) { visibleLists = false }
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { padding ->
        Column(
            Modifier.padding(
                padding
            ).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(stringResource(R.string.settings_lists))
            ListsSection(settings, lists, viewModel.repository) { visibleLists = true }
            SectionTitle(stringResource(R.string.settings_appearance))
            AppearanceSection(settings, viewModel.repository)
            SectionTitle(stringResource(R.string.settings_reminders))
            RemindersSection(settings, viewModel)
            SectionTitle(stringResource(R.string.settings_backup))
            BackupSection()
            SectionTitle(stringResource(R.string.settings_account))
            AccountSection(accountName, onLogOut)
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Who is signed in, and logging out (with its confirmation). */
@Composable
private fun AccountSection(accountName: String, onLogOut: () -> Unit) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    DetailCard {
        Text(accountName, Modifier.padding(16.dp))
        HorizontalDivider(Modifier.padding(start = 16.dp))
        TextButton(onClick = { confirm = true }, modifier = Modifier.padding(8.dp)) {
            Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_text, accountName)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onLogOut()
                }) { Text(stringResource(R.string.logout)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp).semantics { heading() }
    )
}
