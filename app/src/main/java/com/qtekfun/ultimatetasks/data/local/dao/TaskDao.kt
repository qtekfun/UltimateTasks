// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun get(id: Long): TaskEntity?

    @Query("SELECT * FROM task WHERE id = :id")
    fun observe(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM task WHERE accountId = :accountId AND uid = :uid")
    suspend fun byUid(accountId: Long, uid: String): List<TaskEntity>

    /** A list's tasks in manual order (RF-03); deleted ones are already gone for the user. */
    @Query(
        "SELECT * FROM task WHERE accountId = :accountId AND listHref = :listHref " +
            "AND NOT deleted ORDER BY sortOrder IS NULL, sortOrder, id"
    )
    fun observeList(accountId: Long, listHref: String): Flow<List<TaskEntity>>

    /** Every task of a list, deleted ones included, as the sync needs them. */
    @Query("SELECT * FROM task WHERE accountId = :accountId AND listHref = :listHref")
    suspend fun inList(accountId: Long, listHref: String): List<TaskEntity>

    @Query("SELECT * FROM task WHERE accountId = :accountId AND href = :href")
    suspend fun byHref(accountId: Long, href: String): TaskEntity?

    @Query("DELETE FROM task WHERE id = :id")
    suspend fun delete(id: Long)
}
