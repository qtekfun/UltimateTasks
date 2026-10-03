// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import java.time.Instant

/**
 * A task (a `VTODO` resource). The fields the app shows and edits are columns; [ics] keeps the
 * last version read from the server, so properties the app does not know survive every write
 * and a three-way merge can tell which side changed what (T08).
 *
 * Dates are ISO-8601 local text, `2026-10-05` (all day) or `2026-10-05T09:30` (with time), with
 * their zone apart (`Europe/Madrid`, `UTC`, or null for floating time). As text they sort and
 * compare in SQL, which the smart lists need (T11).
 */
@Entity(
    tableName = "task",
    foreignKeys = [
        ForeignKey(
            entity = TaskListEntity::class,
            parentColumns = ["accountId", "href"],
            childColumns = ["accountId", "listHref"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accountId", "listHref", "href", unique = true),
        Index("accountId", "uid"),
        Index("accountId", "completed", "due")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val listHref: String,
    /** The resource on the server; chosen by the app for new tasks, before they are uploaded. */
    val href: String,
    val uid: String,
    /** Null until the server has the task. */
    val etag: String? = null,
    /** The task as last read from the server; null for tasks not uploaded yet. */
    val ics: String? = null,
    val summary: String,
    val notes: String = "",
    val url: String? = null,
    val due: String? = null,
    val dueZone: String? = null,
    val start: String? = null,
    val startZone: String? = null,
    val completed: Boolean = false,
    val completedAt: Instant? = null,
    /** `PRIORITY`: 0 none, 1 high, 5 medium, 9 low (RF-05). */
    val priority: Int = 0,
    val tags: List<String> = emptyList(),
    /** `RELATED-TO;RELTYPE=PARENT` (RF-07). */
    val parentUid: String? = null,
    /** `X-APPLE-SORT-ORDER`, the manual order Nextcloud Tasks uses. */
    val sortOrder: Long? = null,
    /** `RRULE` text, without the property name (RF-06). */
    val recurrence: String? = null,
    /** Early reminder, in seconds before [due] (RF-10); null for none. */
    val reminderBefore: Long? = null,
    val modifiedAt: Instant? = null,
    /** Fields changed here and not yet accepted by the server (T08), as a bit set. */
    val dirtyFields: Int = 0,
    /** Deleted here, waiting for the server to delete it too. */
    val deleted: Boolean = false,
    /** The server title while the user chooses between it and the local one (SPEC §5, rule 1). */
    @ColumnInfo(defaultValue = "NULL")
    val conflictSummary: String? = null,
    /** The server notes while the user chooses between them and the local ones. */
    @ColumnInfo(defaultValue = "NULL")
    val conflictNotes: String? = null,
    /** Deleted on the server while changed here: the user keeps a copy or discards it (rule 3). */
    @ColumnInfo(defaultValue = "0")
    val deletedOnServer: Boolean = false
)
