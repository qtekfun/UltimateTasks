// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.domain.task.DueEdits
import com.qtekfun.ultimatetasks.ui.components.LabelValue
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The zone of a timed due date: floating (the same wall-clock time wherever you are) or a fixed
 * zone. When the fixed zone differs from the device's, the equivalent local time is shown too.
 */
@Composable
fun ZoneRow(task: TaskEntity, editable: Boolean, device: ZoneId, onPick: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val zone = task.dueZone
    val shown = DueEdits.inDeviceZone(task, device)
    val label = zone ?: stringResource(R.string.zone_floating)
    val value = shown?.let {
        stringResource(
            R.string.zone_here,
            label,
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(it)
        )
    } ?: label
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(enabled = editable, role = Role.Button) { open = true }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LabelValue(stringResource(R.string.field_zone), value)
    }
    if (open) {
        ZoneDialog(zone, device, onPick = onPick, onDismiss = { open = false })
    }
}

@Composable
private fun ZoneDialog(
    current: String?,
    device: ZoneId,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(query) {
        ZoneId.getAvailableZoneIds().filter { query.isBlank() || it.contains(query.trim(), true) }
            .sorted().take(MAX_MATCHES)
    }
    val choose = { id: String? ->
        onPick(id)
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.field_zone)) },
        text = {
            Column {
                ZoneChoice(stringResource(R.string.zone_floating_hint), current == null) {
                    choose(null)
                }
                ZoneChoice(device.id, current == device.id) { choose(device.id) }
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.zone_search)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(matches) { id -> ZoneChoice(id, id == current) { choose(id) } }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun ZoneChoice(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text, Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurface)
    }
}

private const val MAX_MATCHES = 50
