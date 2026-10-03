// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.task

import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Changes to the task lists themselves (RF-08, RF-09). */
class ListRepository @Inject constructor(
    private val database: UltimateTasksDatabase,
    private val session: AccountSession
) {
    /** Shows or hides a list everywhere, reminders included (RF-09). */
    suspend fun setVisible(href: String, visible: Boolean) {
        val accountId = session.activeAccount.first()?.id ?: return
        database.taskListDao().setVisible(accountId, href, visible)
    }
}
