// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.engine

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.remote.caldav.CalDav
import com.qtekfun.ultimatetasks.data.remote.caldav.DavChanges
import com.qtekfun.ultimatetasks.data.remote.caldav.DavCollection
import com.qtekfun.ultimatetasks.data.remote.caldav.DavResource
import com.qtekfun.ultimatetasks.data.remote.caldav.DavResult
import com.qtekfun.ultimatetasks.data.remote.caldav.then
import com.qtekfun.ultimatetasks.sync.queue.OperationQueue
import javax.inject.Inject

/**
 * Brings the server state into Room: the account's task lists, then the tasks of each list
 * that changed since its sync token. Each list is written in one transaction, so a pull cut
 * off halfway leaves every list either as before or fully updated.
 */
class PullSync @Inject constructor(
    private val database: UltimateTasksDatabase,
    queue: OperationQueue
) {
    private val accounts = database.accountDao()
    private val lists = database.taskListDao()
    private val tasks = database.taskDao()
    private val merger = TaskMerger(database, queue)

    /** Pulls everything; returns the first failure, or null when all is up to date. */
    suspend fun pull(dav: CalDav, account: AccountEntity): DavResult<*>? {
        val result = home(dav, account)
            .then { home -> dav.read.taskLists(home) }
            .then { collections ->
                DavResult.Success(
                    saveLists(account.id, collections).zip(
                        collections.map {
                            it.ctag
                        }
                    )
                )
            }
        if (result !is DavResult.Success) return result
        return result.value.firstNotNullOfOrNull { (list, ctag) ->
            pullList(dav, account.id, list, ctag)
        }
    }

    private suspend fun home(dav: CalDav, account: AccountEntity): DavResult<String> =
        account.calendarHome?.let { DavResult.Success(it) } ?: dav.read.discover().then { home ->
            accounts.update(account.copy(calendarHome = home))
            DavResult.Success(home)
        }

    /**
     * Saves the server lists, keeping what only lives here (icon, visibility, last sync token).
     * Lists gone from the server go with their tasks, unless a task has unsent changes.
     */
    private suspend fun saveLists(
        accountId: Long,
        collections: List<DavCollection>
    ): List<TaskListEntity> {
        val known = lists.all(accountId).associateBy { it.href }
        val saved = collections.map { collection ->
            val old = known[collection.href]
            TaskListEntity(
                accountId = accountId,
                href = collection.href,
                name = collection.name,
                color = collection.color,
                icon = old?.icon,
                sortOrder = collection.order,
                visible = old?.visible ?: true,
                writable = collection.writable,
                syncToken = old?.syncToken,
                // The ctag of the last pull; the server's current one is compared with it.
                ctag = old?.ctag
            )
        }
        lists.upsert(saved)
        val onServer = collections.map { it.href }.toSet()
        known.keys.filter { it !in onServer }
            .filter { href ->
                tasks.inList(accountId, href).none {
                    it.dirtyFields != 0 ||
                        it.etag == null
                }
            }
            .forEach { lists.delete(accountId, it) }
        return saved
    }

    /**
     * Pulls one list: with its sync token when the server supports sync-collection, otherwise
     * (as for the lists Deck publishes) every task with its ETag, and only when [ctag] changed.
     */
    private suspend fun pullList(
        dav: CalDav,
        accountId: Long,
        list: TaskListEntity,
        ctag: String?
    ): DavResult<*>? {
        if (list.syncToken == null && ctag != null && ctag == list.ctag) return null
        var full = list.syncToken == null
        var changes = dav.read.changes(list.href, list.syncToken)
        if (changes == DavResult.SyncTokenExpired) {
            full = true
            changes = dav.read.changes(list.href, null)
        }
        if (changes == DavResult.HttpError(UNSUPPORTED_REPORT)) {
            full = true
            changes =
                dav.read.allTasks(list.href).then {
                    DavResult.Success(DavChanges(it, emptyList(), null))
                }
        }
        return changes.then { found -> fetchChanged(dav, accountId, list, found) }
            .then { (found, fetched) ->
                save(accountId, list, found, fetched, full)
                lists.setCtag(accountId, list.href, ctag)
                DavResult.Success(Unit)
            }.takeIf { it !is DavResult.Success }
    }

    /** Downloads the tasks whose ETag differs from the local one, a batch at a time. */
    private suspend fun fetchChanged(
        dav: CalDav,
        accountId: Long,
        list: TaskListEntity,
        changes: DavChanges
    ): DavResult<Pair<DavChanges, List<DavResource>>> {
        val local = tasks.inList(accountId, list.href).associateBy { it.href }
        val stale = changes.changed.filter {
            it.etag == null || local[it.href]?.etag != it.etag
        }.map { it.href }
        val fetched = mutableListOf<DavResource>()
        for (batch in stale.chunked(BATCH)) {
            when (val result = dav.read.fetch(list.href, batch)) {
                is DavResult.Success -> fetched += result.value
                else -> return result.then { error("unreachable") }
            }
        }
        return DavResult.Success(changes to fetched)
    }

    /** On a full pull, tasks the server did not list are gone too, unless never uploaded. */
    private suspend fun save(
        accountId: Long,
        list: TaskListEntity,
        changes: DavChanges,
        fetched: List<DavResource>,
        full: Boolean
    ) = inTransaction {
        fetched.forEach { merger.apply(accountId, list.href, it) }
        val listed = changes.changed.map { it.href }.toSet()
        val missing = if (full) {
            tasks.inList(accountId, list.href).filter {
                it.etag != null && it.href !in listed
            }.map { it.href }
        } else {
            emptyList()
        }
        (changes.deleted + missing).forEach { merger.gone(accountId, it) }
        lists.setSyncToken(accountId, list.href, changes.syncToken)
    }

    private suspend fun <R> inTransaction(block: suspend () -> R): R =
        database.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }

    private companion object {
        /** Tasks per calendar-multiget request. */
        const val BATCH = 50

        /** Sabre's answer to a REPORT the collection does not support. */
        const val UNSUPPORTED_REPORT = 415
    }
}
