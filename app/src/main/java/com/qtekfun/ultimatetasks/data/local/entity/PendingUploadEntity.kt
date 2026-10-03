// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * A file chosen to attach to a task, waiting to be uploaded (RF-11). [uri] is the content the
 * app was granted; once uploaded the row goes and the task gets the ATTACH link.
 */
@Entity(
    tableName = "pending_upload",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class PendingUploadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val uri: String,
    val name: String,
    val mimeType: String?,
    /** Why the last try failed for good (too large, file gone…); null while it can still go. */
    val error: String? = null
)
