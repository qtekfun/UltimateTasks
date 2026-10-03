// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.engine

import com.qtekfun.ultimatetasks.data.ical.IcsParser
import com.qtekfun.ultimatetasks.data.ical.IcsWriter
import com.qtekfun.ultimatetasks.data.ical.VtodoMapper
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.remote.caldav.CalDav
import com.qtekfun.ultimatetasks.data.remote.caldav.DavResult
import com.qtekfun.ultimatetasks.sync.conflict.TaskField
import com.qtekfun.ultimatetasks.sync.queue.ExecutionResult
import com.qtekfun.ultimatetasks.sync.queue.OperationExecutor
import com.qtekfun.ultimatetasks.sync.queue.OperationQueue
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation
import java.time.Clock

/** Sends queued task operations with CalDAV (T09). */
class TaskOperationExecutor(
    private val dav: CalDav,
    database: UltimateTasksDatabase,
    queue: OperationQueue,
    private val clock: Clock
) : OperationExecutor {
    private val tasks = database.taskDao()
    private val merger = TaskMerger(database, queue)

    override suspend fun execute(
        taskId: Long,
        operation: QueuedOperation,
        maybeSent: Boolean
    ): ExecutionResult {
        val task = tasks.get(taskId)
        return when {
            operation is QueuedOperation.DeleteTask -> delete(taskId, operation)

            // Gone meanwhile, or waiting for the user: nothing to send.
            task == null || task.deleted || task.deletedOnServer -> ExecutionResult.Done

            operation is QueuedOperation.MoveTask -> move(task, operation)

            else -> upload(task, created = operation == QueuedOperation.CreateTask && maybeSent)
        }
    }

    /**
     * PUTs the task over the version it was based on. Text fields waiting for the user keep the
     * server text. [created] marks a create whose answer was lost: a 412 means it arrived.
     */
    private suspend fun upload(task: TaskEntity, created: Boolean): ExecutionResult {
        val base = task.ics?.let { IcsParser.parse(it).firstOrNull() }
        val serverText = base?.let(VtodoMapper::read)
        var fields = task.fields()
        if (task.conflictSummary != null &&
            serverText != null
        ) {
            fields = fields.copy(summary = serverText.summary)
        }
        if (task.conflictNotes != null &&
            serverText != null
        ) {
            fields = fields.copy(notes = serverText.notes)
        }
        val ics = IcsWriter.write(VtodoMapper.write(base, fields, clock.instant()))
        return when (val result = dav.write.put(task.href, ics, task.etag)) {
            is DavResult.Success -> {
                val current = tasks.get(task.id) ?: return ExecutionResult.Done
                val conflicts = TaskField.fromBits(current.dirtyFields).filter {
                    it.isText &&
                        conflicted(current, it)
                }
                // Changes made while uploading stay dirty: their own operation is queued.
                val dirty = if (current.fields() ==
                    task.fields()
                ) {
                    TaskField.toBits(conflicts.toSet())
                } else {
                    current.dirtyFields
                }
                tasks.update(current.copy(ics = ics, etag = result.value, dirtyFields = dirty))
                ExecutionResult.Done
            }

            DavResult.PreconditionFailed -> refresh(task, created)

            DavResult.NotFound -> {
                merger.gone(task.accountId, task.href)
                ExecutionResult.Done
            }

            else -> failure(result)
        }
    }

    private fun conflicted(task: TaskEntity, field: TaskField) =
        (field == TaskField.SUMMARY && task.conflictSummary != null) ||
            (field == TaskField.NOTES && task.conflictNotes != null)

    /** The server copy changed: merge it in, then send the result on the next run. */
    private suspend fun refresh(task: TaskEntity, created: Boolean): ExecutionResult =
        when (val fetched = dav.read.fetch(task.listHref, listOf(task.href))) {
            is DavResult.Success -> {
                val resource = fetched.value.firstOrNull()
                if (resource == null) {
                    merger.gone(task.accountId, task.href)
                    ExecutionResult.Done
                } else {
                    merger.apply(task.accountId, task.listHref, resource)
                    if (created) ExecutionResult.Done else ExecutionResult.Retry(CONFLICT)
                }
            }

            else -> failure(fetched)
        }

    private suspend fun move(
        task: TaskEntity,
        operation: QueuedOperation.MoveTask
    ): ExecutionResult {
        val name = task.href.substringAfterLast('/')
        return when (
            val result = dav.write.move(
                operation.from + name,
                operation.to + name,
                null
            )
        ) {
            is DavResult.Success, DavResult.NotFound -> ExecutionResult.Done
            else -> failure(result)
        }
    }

    /** The user wants it gone: delete whatever version the server has. */
    private suspend fun delete(
        taskId: Long,
        operation: QueuedOperation.DeleteTask
    ): ExecutionResult = when (val result = dav.write.delete(operation.href, null)) {
        is DavResult.Success, DavResult.NotFound -> {
            tasks.delete(taskId)
            ExecutionResult.Done
        }

        else -> failure(result)
    }

    private fun failure(result: DavResult<*>): ExecutionResult = when (result) {
        DavResult.Forbidden -> ExecutionResult.Failed(READ_ONLY)

        is DavResult.HttpError -> if (result.code < SERVER_ERRORS) {
            ExecutionResult.Failed("HTTP ${result.code}")
        } else {
            ExecutionResult.Retry("HTTP ${result.code}")
        }

        else -> ExecutionResult.Retry(result.toString())
    }

    private companion object {
        const val CONFLICT = "Changed on the server"
        const val READ_ONLY = "Read-only list"
        const val SERVER_ERRORS = 500
    }
}
