// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

/** A CalDAV collection that holds tasks (RF-08), identified by its [href] on the server. */
@Entity(
    tableName = "task_list",
    primaryKeys = ["accountId", "href"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId", "sortOrder")]
)
data class TaskListEntity(
    val accountId: Long,
    val href: String,
    val name: String,
    /** `#RRGGBB`, from `calendar-color`; null when the server has none. */
    val color: String? = null,
    /** One of the app's icons; only stored on the device (Nextcloud has no place for it). */
    val icon: String? = null,
    /** `calendar-order`; lists without it go last. */
    val sortOrder: Int? = null,
    /** Chosen in Settings (RF-09): a hidden list is left out of everything, reminders included. */
    val visible: Boolean = true,
    /** False for lists shared read-only with the user. */
    val writable: Boolean = true,
    /** Where the next incremental pull starts (RFC 6578), or null to pull it all. */
    val syncToken: String? = null,
    /** `getctag`, for servers without sync tokens. */
    val ctag: String? = null
)
