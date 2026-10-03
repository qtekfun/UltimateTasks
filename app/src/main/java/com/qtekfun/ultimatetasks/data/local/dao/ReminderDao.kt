// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.qtekfun.ultimatetasks.data.local.entity.SnoozeEntity
import com.qtekfun.ultimatetasks.data.local.model.DueTaskRow
import kotlinx.coroutines.flow.Flow

/** What the reminders need: tasks with dates in visible lists, and snoozes (RF-10, RF-09). */
@Dao
interface ReminderDao {
    @Query(
        "SELECT t.id, t.summary, l.name AS listName, t.due, t.dueZone, t.reminderBefore " +
            "FROM task t JOIN task_list l ON l.accountId = t.accountId AND l.href = t.listHref " +
            "WHERE t.accountId = :accountId AND l.visible AND NOT t.deleted AND NOT t.completed " +
            "AND t.due IS NOT NULL"
    )
    fun observeDueTasks(accountId: Long): Flow<List<DueTaskRow>>

    @Query("SELECT * FROM snooze")
    fun observeSnoozes(): Flow<List<SnoozeEntity>>

    @Upsert
    suspend fun snooze(snooze: SnoozeEntity)

    @Query("DELETE FROM snooze WHERE taskId = :taskId")
    suspend fun clearSnooze(taskId: Long)
}
