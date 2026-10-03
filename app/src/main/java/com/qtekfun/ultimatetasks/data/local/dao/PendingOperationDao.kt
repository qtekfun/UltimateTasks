// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.qtekfun.ultimatetasks.data.local.entity.PendingOperationEntity

@Dao
interface PendingOperationDao {
    @Insert
    suspend fun insert(operation: PendingOperationEntity): Long

    /** Operations in the order they were made, which is the order they must reach the server. */
    @Query("SELECT * FROM pending_operation WHERE accountId = :accountId ORDER BY id")
    suspend fun all(accountId: Long): List<PendingOperationEntity>

    @Query("DELETE FROM pending_operation WHERE id = :id")
    suspend fun delete(id: Long)
}
