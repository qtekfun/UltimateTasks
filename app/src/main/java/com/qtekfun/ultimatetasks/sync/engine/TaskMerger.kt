// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.engine

import com.qtekfun.ultimatetasks.data.ical.IcsParser
import com.qtekfun.ultimatetasks.data.ical.VtodoFields
import com.qtekfun.ultimatetasks.data.ical.VtodoMapper
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.remote.caldav.DavResource
import com.qtekfun.ultimatetasks.sync.conflict.ConflictResolver
import com.qtekfun.ultimatetasks.sync.conflict.LocalVersion
import com.qtekfun.ultimatetasks.sync.conflict.Resolution
import com.qtekfun.ultimatetasks.sync.conflict.ServerVersion
import com.qtekfun.ultimatetasks.sync.conflict.TaskField
import com.qtekfun.ultimatetasks.sync.queue.OperationQueue
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation

/**
 * Brings one server task into Room. Tasks without local changes simply take the server
 * version; changed ones go through [ConflictResolver] (SPEC §5).
 */
class TaskMerger(database: UltimateTasksDatabase, private val queue: OperationQueue) {
    private val tasks = database.taskDao()
    private val operations = database.pendingOperationDao()

    /** Applies [resource] (with its data) of the list [listHref]; resources without tasks are ignored. */
    suspend fun apply(accountId: Long, listHref: String, resource: DavResource) {
        val ics = resource.data ?: return
        val server = IcsParser.parse(ics).firstOrNull()?.let(VtodoMapper::read) ?: return
        val local = tasks.byHref(accountId, resource.href)
        when {
            local == null -> tasks.insert(
                TaskEntity(
                    accountId = accountId,
                    listHref = listHref,
                    href = resource.href,
                    uid = server.uid,
                    summary = ""
                ).withFields(server).copy(ics = ics, etag = resource.etag)
            )

            // Deleted here: the queued delete wins, whatever the server changed.
            local.deleted -> Unit

            local.dirtyFields == 0 -> tasks.update(
                local.withFields(server).copy(ics = ics, etag = resource.etag)
            )

            else -> merge(
                local,
                server,
                ServerVersion(server, server.modifiedAt),
                ics,
                resource.etag
            )
        }
    }

    /** The server no longer has [href]. */
    suspend fun gone(accountId: Long, href: String) {
        val local = tasks.byHref(accountId, href) ?: return
        val resolution = ConflictResolver.resolve(
            base(local),
            LocalVersion(local.fields(), dirty(local), local.modifiedAt),
            ServerVersion(null, null)
        )
        if (local.deleted || resolution == Resolution.DeleteLocally) {
            operations.deleteForTask(accountId, local.id)
            tasks.delete(local.id)
        } else {
            // Kept with its local changes until the user keeps a copy or discards it.
            operations.deleteForTask(accountId, local.id)
            tasks.update(local.copy(deletedOnServer = true, etag = null, ics = null))
        }
    }

    private suspend fun merge(
        local: TaskEntity,
        server: VtodoFields,
        serverVersion: ServerVersion,
        ics: String,
        etag: String?
    ) {
        val resolution = ConflictResolver.resolve(
            base(local),
            LocalVersion(local.fields(), dirty(local), local.modifiedAt),
            serverVersion
        ) as Resolution.Merge
        val conflicts = resolution.conflicts.associateBy { it.field }
        tasks.update(
            local.withFields(resolution.fields).copy(
                ics = ics,
                etag = etag,
                dirtyFields = TaskField.toBits(resolution.push + conflicts.keys),
                conflictSummary = conflicts[TaskField.SUMMARY]?.server,
                conflictNotes = conflicts[TaskField.NOTES]?.server
            )
        )
        if (resolution.push.isNotEmpty()) {
            queue.enqueue(
                local.accountId,
                local.id,
                QueuedOperation.UpdateTask
            )
        }
    }

    private fun dirty(task: TaskEntity) = TaskField.fromBits(task.dirtyFields)

    private fun base(task: TaskEntity): VtodoFields? =
        task.ics?.let { IcsParser.parse(it).firstOrNull() }?.let(VtodoMapper::read)
}
