// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.engine

import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.remote.caldav.CalDav
import com.qtekfun.ultimatetasks.sync.queue.OperationQueue
import com.qtekfun.ultimatetasks.sync.queue.ProcessResult
import java.time.Clock
import javax.inject.Inject

/** Sends the queued local changes of an account (SPEC §5). */
class PushSync @Inject constructor(
    private val database: UltimateTasksDatabase,
    private val queue: OperationQueue,
    private val clock: Clock
) {
    suspend fun push(dav: CalDav, accountId: Long): ProcessResult =
        queue.process(accountId, TaskOperationExecutor(dav, database, queue, clock))
}
