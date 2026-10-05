// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.qtekfun.ultimatetasks.data.local.entity.ShownReminderEntity
import com.qtekfun.ultimatetasks.data.local.entity.SnoozeEntity
import com.qtekfun.ultimatetasks.data.local.model.DueTaskRow
import java.time.Instant

/** What finding missed reminders needs, read once rather than observed (T32). */
@Dao
interface ShownReminderDao {
    @Query(
        "SELECT t.id, t.summary, l.name AS listName, t.due, t.dueZone, t.reminderBefore " +
            "FROM task t JOIN task_list l ON l.accountId = t.accountId AND l.href = t.listHref " +
            "WHERE t.accountId = :accountId AND l.visible AND NOT t.deleted AND NOT t.completed " +
            "AND t.due IS NOT NULL"
    )
    suspend fun dueTasks(accountId: Long): List<DueTaskRow>

    @Query("SELECT * FROM snooze")
    suspend fun snoozes(): List<SnoozeEntity>

    @Query("SELECT * FROM shown_reminder")
    suspend fun shown(): List<ShownReminderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markShown(shown: List<ShownReminderEntity>)

    @Query("DELETE FROM shown_reminder WHERE `at` <= :cutoff")
    suspend fun forgetBefore(cutoff: Instant)
}
