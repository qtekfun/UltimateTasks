// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.qtekfun.ultimatetasks.data.local.entity.PendingUploadEntity
import kotlinx.coroutines.flow.Flow

/** Files waiting to be attached (RF-11). */
@Dao
interface UploadDao {
    @Insert
    suspend fun insert(upload: PendingUploadEntity): Long

    @Query("SELECT * FROM pending_upload WHERE id = :id")
    suspend fun get(id: Long): PendingUploadEntity?

    @Query("SELECT * FROM pending_upload WHERE taskId = :taskId ORDER BY id")
    fun observeForTask(taskId: Long): Flow<List<PendingUploadEntity>>

    @Query("UPDATE pending_upload SET error = :error WHERE id = :id")
    suspend fun fail(id: Long, error: String?)

    @Query("DELETE FROM pending_upload WHERE id = :id")
    suspend fun delete(id: Long)
}
