// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.model.ListCount
import com.qtekfun.ultimatetasks.data.local.model.SmartCounts
import kotlinx.coroutines.flow.Flow

/** Tasks not deleted, of visible lists; queries add their own conditions after it. */
private const val VISIBLE_TASKS = "FROM task t JOIN task_list l " +
    "ON l.accountId = t.accountId AND l.href = t.listHref WHERE l.visible AND NOT t.deleted "

/**
 * The smart lists of the home screen (RF-02, T11). Only tasks of visible lists count (RF-09).
 * Due dates are ISO text, so "due before tomorrow" is a text comparison with [tomorrow]
 * (`yyyy-MM-dd`): it holds dated and timed tasks of today and every overdue one.
 */
@Dao
interface SmartListDao {
    @Query(
        "SELECT " +
            "COALESCE(SUM(NOT t.completed AND t.due IS NOT NULL AND t.due < :tomorrow), 0) " +
            "AS today, " +
            "COALESCE(SUM(NOT t.completed AND t.due IS NOT NULL), 0) AS scheduled, " +
            "COALESCE(SUM(NOT t.completed), 0) AS `all`, " +
            "COALESCE(SUM(t.completed), 0) AS completed " +
            "$VISIBLE_TASKS" +
            "AND t.accountId = :accountId"
    )
    fun observeCounts(accountId: Long, tomorrow: String): Flow<SmartCounts>

    @Query(
        "SELECT listHref, COUNT(*) AS open FROM task " +
            "WHERE accountId = :accountId AND NOT deleted AND NOT completed GROUP BY listHref"
    )
    fun observeOpenCounts(accountId: Long): Flow<List<ListCount>>

    @Query(
        "SELECT t.* $VISIBLE_TASKS" +
            "AND t.accountId = :accountId AND NOT t.completed " +
            "AND t.due IS NOT NULL AND t.due < :tomorrow ORDER BY t.due, t.id"
    )
    fun observeToday(accountId: Long, tomorrow: String): Flow<List<TaskEntity>>

    @Query(
        "SELECT t.* $VISIBLE_TASKS" +
            "AND t.accountId = :accountId AND NOT t.completed " +
            "AND t.due IS NOT NULL ORDER BY t.due, t.id"
    )
    fun observeScheduled(accountId: Long): Flow<List<TaskEntity>>

    @Query(
        "SELECT t.* $VISIBLE_TASKS" +
            "AND t.accountId = :accountId AND NOT t.completed " +
            "ORDER BY l.sortOrder IS NULL, l.sortOrder, l.name COLLATE NOCASE, " +
            "t.sortOrder IS NULL, t.sortOrder, t.id"
    )
    fun observeAll(accountId: Long): Flow<List<TaskEntity>>

    @Query(
        "SELECT t.* $VISIBLE_TASKS" +
            "AND t.accountId = :accountId AND t.completed " +
            "ORDER BY t.completedAt IS NULL, t.completedAt DESC, t.id DESC"
    )
    fun observeCompleted(accountId: Long): Flow<List<TaskEntity>>
}
