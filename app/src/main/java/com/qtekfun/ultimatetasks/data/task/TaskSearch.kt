// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.task

import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** Search on the home screen (RF-12): title, notes and tags of the visible lists. */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskSearch @Inject constructor(
    database: UltimateTasksDatabase,
    private val session: AccountSession
) {
    private val dao = database.smartListDao()

    /** Nothing for a blank query; otherwise every match, live. */
    fun observe(query: String, includeCompleted: Boolean): Flow<List<TaskEntity>> {
        val text = query.trim()
        if (text.isEmpty()) return flowOf(emptyList())
        return session.activeAccount.filterNotNull().flatMapLatest {
            dao.search(it.id, pattern(text), includeCompleted)
        }
    }

    /** A LIKE pattern that matches [text] anywhere, its own % and _ taken literally. */
    internal fun pattern(text: String): String =
        "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
}
