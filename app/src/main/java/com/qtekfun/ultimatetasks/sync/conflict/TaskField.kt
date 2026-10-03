// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.conflict

import com.qtekfun.ultimatetasks.data.ical.VtodoFields

/**
 * The fields of a task that can change on either side, each with its bit in
 * `TaskEntity.dirtyFields` and how to read and set it on [VtodoFields].
 */
enum class TaskField(
    val read: (VtodoFields) -> Any?,
    val write: (VtodoFields, VtodoFields) -> VtodoFields
) {
    SUMMARY(VtodoFields::summary, { to, from -> to.copy(summary = from.summary) }),
    NOTES(VtodoFields::notes, { to, from -> to.copy(notes = from.notes) }),
    URL(VtodoFields::url, { to, from -> to.copy(url = from.url) }),
    DUE(VtodoFields::due, { to, from -> to.copy(due = from.due) }),
    START(VtodoFields::start, { to, from -> to.copy(start = from.start) }),
    COMPLETION(
        { it.completed to it.completedAt },
        { to, from -> to.copy(completed = from.completed, completedAt = from.completedAt) }
    ),
    PRIORITY(VtodoFields::priority, { to, from -> to.copy(priority = from.priority) }),
    TAGS(VtodoFields::tags, { to, from -> to.copy(tags = from.tags) }),
    PARENT(VtodoFields::parentUid, { to, from -> to.copy(parentUid = from.parentUid) }),
    ORDER(VtodoFields::sortOrder, { to, from -> to.copy(sortOrder = from.sortOrder) }),
    RECURRENCE(VtodoFields::recurrence, { to, from -> to.copy(recurrence = from.recurrence) }),
    REMINDER(VtodoFields::reminderBefore, { to, from ->
        to.copy(reminderBefore = from.reminderBefore)
    }),
    ATTACHMENTS(VtodoFields::attachments, { to, from -> to.copy(attachments = from.attachments) });

    val bit: Int get() = 1 shl ordinal

    /** Title and notes are never overwritten when both sides changed them (SPEC §5, rule 1). */
    val isText: Boolean get() = this == SUMMARY || this == NOTES

    companion object {
        fun fromBits(bits: Int): Set<TaskField> = entries.filter { bits and it.bit != 0 }.toSet()

        fun toBits(fields: Set<TaskField>): Int = fields.fold(0) { bits, field ->
            bits or field.bit
        }
    }
}
