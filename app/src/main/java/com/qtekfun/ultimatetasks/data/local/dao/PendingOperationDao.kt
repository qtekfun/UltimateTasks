// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.qtekfun.ultimatetasks.data.local.entity.PendingOperationEntity
import kotlinx.coroutines.flow.Flow

/** Storage of the operation queue; ordering, merging and retries live in OperationQueue (T07). */
@Dao
interface PendingOperationDao {
    @Insert
    suspend fun insert(operation: PendingOperationEntity): Long

    /** Operations in the order they were made, which is the order they must reach the server. */
    @Query("SELECT * FROM pending_operation WHERE accountId = :accountId ORDER BY id")
    suspend fun all(accountId: Long): List<PendingOperationEntity>

    @Query(
        "SELECT * FROM pending_operation WHERE accountId = :accountId AND taskId = :taskId ORDER BY id"
    )
    suspend fun forTask(accountId: Long, taskId: Long): List<PendingOperationEntity>

    /** Replaces the data of an operation that was never sent (merging repeated changes). */
    @Query("UPDATE pending_operation SET payload = :payload WHERE id = :id")
    suspend fun replacePayload(id: Long, payload: String)

    @Query("DELETE FROM pending_operation WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_operation WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM pending_operation WHERE accountId = :accountId")
    fun observeCount(accountId: Long): Flow<Int>
}
