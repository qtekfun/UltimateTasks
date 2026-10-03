// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.engine

import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.remote.caldav.CalDavProvider
import com.qtekfun.ultimatetasks.data.remote.caldav.DavResult
import com.qtekfun.ultimatetasks.di.IoDispatcher
import com.qtekfun.ultimatetasks.sync.queue.ProcessResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** How a sync ended. */
sealed interface SyncOutcome {
    /** Pulled; [pushed] tells what happened to the queued changes. */
    data class Ok(val pushed: ProcessResult) : SyncOutcome

    data object NoAccount : SyncOutcome

    data object Offline : SyncOutcome

    data object Unauthorized : SyncOutcome

    data class Error(val reason: String) : SyncOutcome
}

/**
 * Syncs the signed-in account: first sends the queued local changes, then pulls the server
 * state, so the conflict resolver only sees real conflicts. Syncs never overlap: one started
 * while another runs waits for it.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val session: AccountSession,
    private val provider: CalDavProvider,
    private val database: UltimateTasksDatabase,
    private val push: PushSync,
    private val pull: PullSync,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    private val mutex = Mutex()
    private val mutableLastOutcome = MutableStateFlow<SyncOutcome?>(null)

    /** How the latest sync of this process ended; null until one finishes. */
    val lastOutcome: StateFlow<SyncOutcome?> = mutableLastOutcome.asStateFlow()

    suspend fun sync(): SyncOutcome = withContext(dispatcher) {
        mutex.withLock { run().also { mutableLastOutcome.value = it } }
    }

    private suspend fun run(): SyncOutcome {
        // A background sync may start in a fresh process, before the UI loaded the credentials.
        if (session.credentials() == null) session.restore()
        val account = session.activeAccount.first()
        val dav = account?.let { provider.connect(it) }
        return if (account == null || dav == null) {
            SyncOutcome.NoAccount
        } else {
            val pushed = push.push(dav, account.id)
            // The account row may have changed meanwhile (calendar home found), so read it again.
            val current = database.accountDao().get(account.id) ?: account
            pull.pull(dav, current)?.toOutcome() ?: SyncOutcome.Ok(pushed)
        }
    }

    private fun DavResult<*>.toOutcome(): SyncOutcome = when (this) {
        is DavResult.NetworkError -> SyncOutcome.Offline
        DavResult.Unauthorized -> SyncOutcome.Unauthorized
        is DavResult.HttpError -> SyncOutcome.Error("HTTP $code")
        else -> SyncOutcome.Error(toString())
    }
}
