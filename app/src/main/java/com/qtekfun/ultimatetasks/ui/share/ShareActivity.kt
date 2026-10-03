// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.task.SharedText
import com.qtekfun.ultimatetasks.ui.detail.Choice
import com.qtekfun.ultimatetasks.ui.theme.UltimateTasksTheme
import dagger.hilt.android.AndroidEntryPoint

/** "Share → UltimateTasks" from any app (RF-13): a new task prefilled with what was shared. */
@AndroidEntryPoint
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = SharedText.parse(
            intent.getStringExtra(Intent.EXTRA_SUBJECT),
            intent.getStringExtra(Intent.EXTRA_TEXT)
        )
        setContent {
            UltimateTasksTheme {
                val viewModel: ShareViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                when {
                    !state.loaded -> Unit

                    state.lists.isEmpty() -> {
                        Toast.makeText(this, R.string.share_sign_in_first, Toast.LENGTH_LONG).show()
                        finish()
                    }

                    else -> ShareDialog(state, shared.title, onCancel = ::finish) { list, title ->
                        viewModel.save(list, shared.copy(title = title)) {
                            Toast.makeText(this, R.string.share_added, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareDialog(
    state: ShareState,
    initialTitle: String,
    onCancel: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var list by rememberSaveable { mutableStateOf(state.default ?: state.lists.first().href) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.new_task)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = {
                    title = it
                }, label = { Text(stringResource(R.string.field_title)) })
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.field_list), Modifier.weight(1f))
                    Choice(state.lists.first { it.href == list }.name, state.lists, { it.name }) {
                        list =
                            it.href
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(list, title)
            }, enabled = title.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } }
    )
}
