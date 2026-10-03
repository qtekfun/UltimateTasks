// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.data.task.ListLook
import com.qtekfun.ultimatetasks.ui.theme.ListColors
import com.qtekfun.ultimatetasks.ui.theme.ListIcons

/** Name, color and icon of a new or existing list (RF-08), as Apple's list editor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListEditorDialog(initial: ListLook?, onSave: (ListLook) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var color by rememberSaveable { mutableStateOf(initial?.color ?: ListColors.DEFAULT) }
    var icon by rememberSaveable { mutableStateOf(initial?.icon ?: ListIcons.all.keys.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial ==
                        null
                    ) {
                        R.string.list_new
                    } else {
                        R.string.list_edit
                    }
                )
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ListBadge(color, icon, Modifier.align(Alignment.CenterHorizontally))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.list_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                ColorPicker(color) { color = it }
                IconPicker(icon) { icon = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(ListLook(name, color, icon))
            }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPicker(selected: String, onPick: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ListColors.palette.forEach { hex ->
            Swatch(selected = hex == selected, description = hex, onClick = { onPick(hex) }) {
                Box(Modifier.size(32.dp).background(ListColors.of(hex), CircleShape))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPicker(selected: String, onPick: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ListIcons.all.forEach { (key, vector) ->
            Swatch(selected = key == selected, description = key, onClick = { onPick(key) }) {
                Icon(vector, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** The list's icon on its color, as a big circle. */
@Composable
private fun ListBadge(color: String, icon: String, modifier: Modifier) {
    Box(
        modifier.size(64.dp).background(ListColors.of(color), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            ListIcons.of(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}

/** A 48 dp choice with a ring when selected. */
@Composable
private fun Swatch(
    selected: Boolean,
    description: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val ring = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
    Box(
        Modifier.size(48.dp).border(2.dp, ring, CircleShape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { content() }
}
