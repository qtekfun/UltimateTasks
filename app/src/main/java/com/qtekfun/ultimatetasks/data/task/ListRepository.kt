// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.task

import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.remote.caldav.CalDav
import com.qtekfun.ultimatetasks.data.remote.caldav.CalDavProvider
import com.qtekfun.ultimatetasks.data.remote.caldav.DavResult
import com.qtekfun.ultimatetasks.data.remote.caldav.then
import com.qtekfun.ultimatetasks.sync.engine.SyncScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** How a change to a list ended: lists are changed on the server at once (RF-08). */
enum class ListChange { DONE, OFFLINE, FAILED }

/** What a list looks like: name and color on the server, icon only here. */
data class ListLook(val name: String, val color: String?, val icon: String?)

/**
 * Changes to the task lists themselves (RF-08, RF-09). Creating, renaming, recoloring,
 * reordering and deleting need the server, so they are not queued: without connection they
 * fail with [ListChange.OFFLINE] and nothing changes here.
 */
class ListRepository @Inject constructor(
    private val database: UltimateTasksDatabase,
    private val session: AccountSession,
    private val provider: CalDavProvider,
    private val scheduler: SyncScheduler
) {
    private val lists = database.taskListDao()

    /** Shows or hides a list everywhere, reminders included (RF-09). Only here. */
    suspend fun setVisible(href: String, visible: Boolean) {
        val accountId = session.activeAccount.first()?.id ?: return
        lists.setVisible(accountId, href, visible)
    }

    /** Creates a list on the server, last in the order, and pulls it. */
    suspend fun create(look: ListLook): ListChange = withServer { account, dav ->
        home(account, dav).then { home ->
            dav.write.createList(home, look.name.trim(), look.color)
        }.then { href ->
            val order = (lists.all(account.id).mapNotNull { it.sortOrder }.maxOrNull() ?: 0) + 1
            dav.write.updateList(href, null, null, order)
            lists.upsert(
                listOf(
                    TaskListEntity(account.id, href, look.name.trim(), look.color, look.icon, order)
                )
            )
            scheduler.requestSync()
            DavResult.Success(Unit)
        }
    }

    /** Renames and recolors on the server; the icon stays here. */
    suspend fun edit(href: String, look: ListLook): ListChange = withServer { account, dav ->
        val list = lists.get(account.id, href)
        when {
            list == null -> DavResult.NotFound

            list.name == look.name.trim() && list.color == look.color -> {
                lists.update(listOf(list.copy(icon = look.icon)))
                DavResult.Success(Unit)
            }

            else -> dav.write.updateList(href, look.name.trim(), look.color, null).then {
                lists.update(
                    listOf(list.copy(name = look.name.trim(), color = look.color, icon = look.icon))
                )
                DavResult.Success(Unit)
            }
        }
    }

    /** Saves the order of the home screen ([hrefs] first to last) on the server. */
    suspend fun reorder(hrefs: List<String>): ListChange = withServer { account, dav ->
        val known = lists.all(account.id).associateBy { it.href }
        var result: DavResult<Unit> = DavResult.Success(Unit)
        hrefs.forEachIndexed { index, href ->
            val list = known[href]
            if (result is DavResult.Success && list != null && list.sortOrder != index) {
                result = dav.write.updateList(href, null, null, index)
                if (result is DavResult.Success) lists.update(listOf(list.copy(sortOrder = index)))
            }
        }
        result
    }

    /** Deletes the list and its tasks on the server, then here. */
    suspend fun delete(href: String): ListChange = withServer { account, dav ->
        dav.write.deleteList(href).then {
            lists.delete(account.id, href)
            DavResult.Success(Unit)
        }
    }

    private suspend fun withServer(
        change: suspend (AccountEntity, CalDav) -> DavResult<Unit>
    ): ListChange {
        val account = session.activeAccount.first()
        val dav = account?.let { provider.connect(it) }
        val result = if (account == null ||
            dav == null
        ) {
            DavResult.Unauthorized
        } else {
            change(account, dav)
        }
        return when (result) {
            is DavResult.Success -> ListChange.DONE
            is DavResult.NetworkError -> ListChange.OFFLINE
            else -> ListChange.FAILED
        }
    }

    private suspend fun home(account: AccountEntity, dav: CalDav): DavResult<String> =
        account.calendarHome?.let { DavResult.Success(it) } ?: dav.read.discover()
}
