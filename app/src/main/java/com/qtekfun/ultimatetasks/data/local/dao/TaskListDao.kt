// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringExisting(lists: List<TaskListEntity>): List<Long>

    @Update
    suspend fun update(lists: List<TaskListEntity>)

    /**
     * Inserts new rows and updates existing ones in place. Unlike REPLACE, existing rows are
     * never deleted, so their tasks are kept.
     */
    @Transaction
    suspend fun upsert(lists: List<TaskListEntity>) {
        val rowIds = insertIgnoringExisting(lists)
        update(lists.filterIndexed { index, _ -> rowIds[index] == -1L })
    }

    @Query("SELECT * FROM task_list WHERE accountId = :accountId AND href = :href")
    suspend fun get(accountId: Long, href: String): TaskListEntity?

    /** Lists in the order of the home screen: server order, lists without one last, then name. */
    @Query(
        "SELECT * FROM task_list WHERE accountId = :accountId " +
            "ORDER BY sortOrder IS NULL, sortOrder, name COLLATE NOCASE"
    )
    fun observeAll(accountId: Long): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM task_list WHERE accountId = :accountId")
    suspend fun all(accountId: Long): List<TaskListEntity>

    /** Where the next incremental pull of the list starts (T09). */
    @Query("UPDATE task_list SET syncToken = :token WHERE accountId = :accountId AND href = :href")
    suspend fun setSyncToken(accountId: Long, href: String, token: String?)

    @Query("UPDATE task_list SET visible = :visible WHERE accountId = :accountId AND href = :href")
    suspend fun setVisible(accountId: Long, href: String, visible: Boolean)

    @Query("DELETE FROM task_list WHERE accountId = :accountId AND href = :href")
    suspend fun delete(accountId: Long, href: String)
}
