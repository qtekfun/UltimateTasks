// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey
import java.time.Instant

/**
 * A reminder snoozed from its notification (RF-10): it rings again at [until]. Only on this
 * device; the task on the server does not change. Goes with its task.
 */
@Entity(
    tableName = "snooze",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SnoozeEntity(@PrimaryKey val taskId: Long, val until: Instant)
