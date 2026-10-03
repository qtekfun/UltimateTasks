// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.domain.task.DueDates
import com.qtekfun.ultimatetasks.domain.task.priorityMarks
import com.qtekfun.ultimatetasks.ui.components.RoundCheckbox
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A task as in Apple Reminders (RF-03): round checkbox, priority marks and title, two lines of
 * notes, then the due date (red when overdue), repetition, tags and, in smart lists, the list.
 */
@Composable
fun TaskRow(
    task: TaskEntity,
    color: Color,
    listName: String?,
    /** Null when the task cannot be checked here (read-only list, unknown repetition). */
    onCheckedChange: ((Boolean) -> Unit)?,
    onClick: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val now = Instant.now()
    val due = task.due?.let { DueDates.info(it, task.dueZone, now, zone) }
    Row(
        Modifier.fillMaxWidth().clickable(
            role = Role.Button,
            onClick = onClick
        ).padding(end = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        RoundCheckbox(
            checked = task.completed,
            color = color,
            description = stringResource(
                if (task.completed) R.string.task_mark_open else R.string.task_mark_done,
                task.summary
            ),
            onCheckedChange = onCheckedChange
        )
        Column(
            Modifier.weight(1f).padding(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    val marks = priorityMarks(task.priority)
                    if (marks.isNotEmpty()) {
                        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                            append("$marks ")
                        }
                    }
                    append(task.summary)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = textColor(task.completed)
            )
            if (task.notes.isNotBlank()) {
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Details(
                task,
                due?.let { dueLabel(it, LocalDate.now(zone)) },
                due?.overdue == true && !task.completed,
                listName
            )
        }
    }
    HorizontalDivider(Modifier.padding(start = 48.dp))
}

@Composable
private fun Details(task: TaskEntity, due: String?, overdue: Boolean, listName: String?) {
    val parts = listOfNotNull(listName) + task.tags.map { "#$it" }
    if (due == null && task.recurrence == null && parts.isEmpty()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        due?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (overdue) {
                    MaterialTheme.colorScheme.error
                } else {
                    textColor(
                        completed = true
                    )
                }
            )
        }
        if (task.recurrence != null) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.task_repeats),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (parts.isNotEmpty()) {
            Text(
                text = parts.joinToString("  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun textColor(completed: Boolean): Color {
    val colors = MaterialTheme.colorScheme
    return if (completed) colors.onSurfaceVariant else colors.onSurface
}
