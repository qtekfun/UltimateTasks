// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.task

import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.local.model.SmartCounts
import com.qtekfun.ultimatetasks.domain.recurrence.RepeatingTasks
import com.qtekfun.ultimatetasks.domain.task.SmartList
import com.qtekfun.ultimatetasks.domain.task.TaskSource
import com.qtekfun.ultimatetasks.sync.conflict.TaskField
import com.qtekfun.ultimatetasks.sync.engine.SyncScheduler
import com.qtekfun.ultimatetasks.sync.queue.OperationQueue
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** A list on the home screen with its open tasks. */
data class ListSummary(val list: TaskListEntity, val open: Int)

/** Everything the home screen shows (RF-02). */
data class HomeData(val counts: SmartCounts, val lists: List<ListSummary>)

/**
 * Tasks as the screens see them. Every change is saved here at once and queued for the
 * server, then a sync is requested: the app never waits for the network.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepository @Inject constructor(
    private val database: UltimateTasksDatabase,
    private val session: AccountSession,
    private val queue: OperationQueue,
    private val scheduler: SyncScheduler,
    private val clock: Clock
) {
    private val tasks = database.taskDao()
    private val smart = database.smartListDao()

    private val accountId: Flow<Long> =
        session.activeAccount.filterNotNull().map { it.id }.distinctUntilChanged()

    fun observeHome(): Flow<HomeData> = accountId.flatMapLatest { id ->
        combine(
            smart.observeCounts(id, tomorrow()),
            database.taskListDao().observeAll(id),
            smart.observeOpenCounts(id)
        ) { counts, lists, open ->
            val byList = open.associate { it.listHref to it.open }
            HomeData(
                counts,
                lists.filter {
                    it.visible
                }.map { ListSummary(it, byList[it.href] ?: 0) }
            )
        }
    }

    fun observeLists(): Flow<List<TaskListEntity>> =
        accountId.flatMapLatest { database.taskListDao().observeAll(it) }

    fun observeTasks(source: TaskSource): Flow<List<TaskEntity>> = accountId.flatMapLatest { id ->
        when (source) {
            is TaskSource.List -> tasks.observeList(id, source.href)

            is TaskSource.Smart -> when (source.kind) {
                SmartList.TODAY -> smart.observeToday(id, tomorrow())
                SmartList.SCHEDULED -> smart.observeScheduled(id)
                SmartList.ALL -> smart.observeAll(id)
                SmartList.COMPLETED -> smart.observeCompleted(id)
            }
        }
    }

    /**
     * Marks a task done or open (RF-04). A repeating task is not completed but moved to its
     * next occurrence (RF-06), unless its series ended.
     */
    suspend fun setCompleted(task: TaskEntity, completed: Boolean) {
        val now = clock.instant()
        val next = task.recurrence?.takeIf { completed }?.let {
            RepeatingTasks.next(
                task.due,
                task.start,
                it,
                LocalDate.now(clock.withZone(ZoneId.systemDefault()))
            )
        }
        val updated = if (next != null) {
            task.copy(
                due = next.due,
                start = next.start,
                recurrence = next.recurrence,
                dirtyFields = task.dirtyFields or REPEAT_FIELDS,
                modifiedAt = now
            )
        } else {
            task.copy(
                completed = completed,
                completedAt = if (completed) now else null,
                dirtyFields = task.dirtyFields or TaskField.COMPLETION.bit,
                modifiedAt = now
            )
        }
        tasks.update(updated)
        changed(task)
    }

    /** Puts back a task as it was before a change, for Undo; the server gets that state. */
    suspend fun restore(previous: TaskEntity) {
        val current = tasks.get(previous.id) ?: return
        tasks.update(
            previous.copy(
                ics = current.ics,
                etag = current.etag,
                dirtyFields = current.dirtyFields or previous.dirtyFields,
                modifiedAt = clock.instant()
            )
        )
        changed(previous)
    }

    /** Adds a task at the end of [listHref] (RF-03); blank titles are ignored. */
    suspend fun create(listHref: String, title: String, parentUid: String? = null): Long? {
        val summary = title.trim()
        val accountId = session.activeAccount.first()?.id
        return if (summary.isEmpty() ||
            accountId == null
        ) {
            null
        } else {
            insert(accountId, listHref, summary, parentUid)
        }
    }

    private suspend fun insert(
        accountId: Long,
        listHref: String,
        summary: String,
        parentUid: String?
    ): Long {
        val uid = UUID.randomUUID().toString()
        val id = tasks.insert(
            TaskEntity(
                accountId = accountId,
                listHref = listHref,
                href = listHref + uid + ".ics",
                uid = uid,
                summary = summary,
                parentUid = parentUid,
                modifiedAt = clock.instant(),
                dirtyFields = TaskField.toBits(TaskField.entries.toSet())
            )
        )
        queue.enqueue(accountId, id, QueuedOperation.CreateTask)
        scheduler.requestSync()
        return id
    }

    private suspend fun changed(task: TaskEntity) {
        queue.enqueue(task.accountId, task.id, QueuedOperation.UpdateTask)
        scheduler.requestSync()
    }

    /** Due dates are local text, so "today" is the device's local date. */
    private companion object {
        val REPEAT_FIELDS = TaskField.toBits(
            setOf(TaskField.DUE, TaskField.START, TaskField.RECURRENCE)
        )
    }

    private fun tomorrow(): String =
        LocalDate.now(clock.withZone(ZoneId.systemDefault())).plusDays(1).toString()
}
