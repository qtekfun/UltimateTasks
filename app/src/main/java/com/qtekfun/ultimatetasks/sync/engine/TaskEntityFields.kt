// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.engine

import com.qtekfun.ultimatetasks.data.ical.IcsDate
import com.qtekfun.ultimatetasks.data.ical.VtodoFields
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity

/** The task as iCalendar fields. */
fun TaskEntity.fields() = VtodoFields(
    uid = uid,
    summary = summary,
    notes = notes,
    url = url,
    due = due?.let { IcsDate(it, dueZone) },
    start = start?.let { IcsDate(it, startZone) },
    completed = completed,
    completedAt = completedAt,
    priority = priority,
    tags = tags,
    parentUid = parentUid,
    sortOrder = sortOrder,
    recurrence = recurrence,
    reminderBefore = reminderBefore,
    attachments = attachments,
    modifiedAt = modifiedAt
)

/** The task with [fields] in its columns; nothing else changes. */
fun TaskEntity.withFields(fields: VtodoFields) = copy(
    uid = fields.uid,
    summary = fields.summary,
    notes = fields.notes,
    url = fields.url,
    due = fields.due?.local,
    dueZone = fields.due?.zone,
    start = fields.start?.local,
    startZone = fields.start?.zone,
    completed = fields.completed,
    completedAt = fields.completedAt,
    priority = fields.priority,
    tags = fields.tags,
    parentUid = fields.parentUid,
    sortOrder = fields.sortOrder,
    recurrence = fields.recurrence,
    reminderBefore = fields.reminderBefore,
    attachments = fields.attachments,
    modifiedAt = fields.modifiedAt
)
