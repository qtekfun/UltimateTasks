// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.qtekfun.ultimatetasks.data.local.model.OperationType
import java.time.Instant

/**
 * A change made offline, waiting to reach the server (T07). Survives restarts. Tasks are the
 * only thing queued: creating, renaming or deleting lists needs a connection (RF-08).
 */
@Entity(
    tableName = "pending_operation",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId", "nextAttemptAt"), Index("accountId", "taskId")]
)
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val type: OperationType,
    val taskId: Long,
    val payload: String = "",
    val createdAt: Instant,
    val attempts: Int = 0,
    val nextAttemptAt: Instant = createdAt,
    val lastError: String? = null,
    /** Refused for good by the server: no automatic retries until the user retries or discards it. */
    val failed: Boolean = false,
    /** When it was last handed to the server; it may have arrived even without an answer. */
    val startedAt: Instant? = null
)
