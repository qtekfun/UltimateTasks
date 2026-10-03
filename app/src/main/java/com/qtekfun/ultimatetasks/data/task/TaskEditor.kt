// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.task

import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.sync.conflict.TaskField
import com.qtekfun.ultimatetasks.sync.engine.SyncScheduler
import com.qtekfun.ultimatetasks.sync.engine.fields
import com.qtekfun.ultimatetasks.sync.queue.OperationQueue
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Which side of a text conflict the user keeps (SPEC §5, rule 1). */
enum class ConflictChoice { MINE, SERVER }

/**
 * Edits of one task from its detail screen (RF-05): saved at once, marked dirty field by field
 * and queued for the server.
 */
class TaskEditor @Inject constructor(
    database: UltimateTasksDatabase,
    private val queue: OperationQueue,
    private val scheduler: SyncScheduler,
    private val clock: Clock
) {
    private val tasks = database.taskDao()

    fun observe(id: Long): Flow<TaskEntity?> = tasks.observe(id)

    fun observeChildren(task: TaskEntity): Flow<List<TaskEntity>> =
        tasks.observeChildren(task.accountId, task.uid)

    /** Saves [edit] of the current task; only the fields that really changed become dirty. */
    suspend fun update(id: Long, edit: (TaskEntity) -> TaskEntity) {
        val current = tasks.get(id) ?: return
        val edited = edit(current)
        val changed = TaskField.entries.filter {
            it.read(current.fields()) !=
                it.read(edited.fields())
        }
        if (changed.isEmpty()) return
        tasks.update(
            edited.copy(
                dirtyFields = current.dirtyFields or TaskField.toBits(changed.toSet()),
                modifiedAt = clock.instant()
            )
        )
        send(current, QueuedOperation.UpdateTask)
    }

    /** Saves a new manual order (RF-03): task id to `X-APPLE-SORT-ORDER`. */
    suspend fun reorder(orders: Map<Long, Long>) = orders.forEach { (id, order) ->
        update(id) { it.copy(sortOrder = order) }
    }

    /** Moves the task to another list: a new resource there, same name (RF-05). */
    suspend fun move(id: Long, listHref: String) {
        val task = tasks.get(id) ?: return
        if (task.listHref == listHref) return
        tasks.update(
            task.copy(
                listHref = listHref,
                href =
                    listHref + task.href.substringAfterLast('/')
            )
        )
        send(task, QueuedOperation.MoveTask(task.listHref, listHref))
    }

    /** Deletes the task here at once and on the server when it gets there. */
    suspend fun delete(id: Long) {
        val task = tasks.get(id) ?: return
        tasks.update(task.copy(deleted = true))
        val needsServer = queue.enqueue(
            task.accountId,
            task.id,
            QueuedOperation.DeleteTask(task.href, task.etag)
        )
        if (needsServer) scheduler.requestSync() else tasks.delete(task.id)
    }

    /** Ends a text conflict with the version the user chose; the other one is dropped. */
    suspend fun resolve(id: Long, field: TaskField, choice: ConflictChoice) {
        val task = tasks.get(id) ?: return
        val server = if (field == TaskField.SUMMARY) task.conflictSummary else task.conflictNotes
        val resolved = when {
            server == null -> task

            choice == ConflictChoice.SERVER && field == TaskField.SUMMARY -> task.copy(
                summary = server
            )

            choice == ConflictChoice.SERVER -> task.copy(notes = server)

            else -> task
        }
        val cleared = if (field ==
            TaskField.SUMMARY
        ) {
            resolved.copy(conflictSummary = null)
        } else {
            resolved.copy(conflictNotes = null)
        }
        // Keeping mine sends it; taking the server's leaves nothing to send for that field.
        val dirty = if (choice ==
            ConflictChoice.MINE
        ) {
            task.dirtyFields
        } else {
            task.dirtyFields and field.bit.inv()
        }
        tasks.update(cleared.copy(dirtyFields = dirty))
        if (choice == ConflictChoice.MINE) send(task, QueuedOperation.UpdateTask)
    }

    /** A task deleted on the server while changed here: upload it again as a new one (rule 3). */
    suspend fun keepCopy(id: Long) {
        val task = tasks.get(id) ?: return
        tasks.update(
            task.copy(deletedOnServer = false, etag = null, ics = null, dirtyFields = ALL_FIELDS)
        )
        send(task, QueuedOperation.CreateTask)
    }

    /** …or let it go, as the server did. */
    suspend fun discard(id: Long) = tasks.delete(id)

    private suspend fun send(task: TaskEntity, operation: QueuedOperation) {
        queue.enqueue(task.accountId, task.id, operation)
        scheduler.requestSync()
    }

    private companion object {
        val ALL_FIELDS = TaskField.toBits(TaskField.entries.toSet())
    }
}
