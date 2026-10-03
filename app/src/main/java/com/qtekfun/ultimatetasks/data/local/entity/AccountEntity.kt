// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** A Nextcloud account. Credentials are stored encrypted apart, in [AccountCredentialsEntity]. */
@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverUrl: String,
    val userId: String,
    val displayName: String,
    /** Where the account's CalDAV collections live, found by discovery (T05). */
    val calendarHome: String? = null
)
