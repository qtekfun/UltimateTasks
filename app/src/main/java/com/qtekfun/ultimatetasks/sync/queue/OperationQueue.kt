// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.queue

import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.PendingOperationEntity
import com.qtekfun.ultimatetasks.data.local.model.OperationType
import java.time.Clock
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException

/**
 * Persistent queue of local changes waiting for the server (SPEC §5). Changes are never lost:
 * an operation only leaves the queue once the server applied it or the user discards it.
 */
class OperationQueue @Inject constructor(
    database: UltimateTasksDatabase,
    private val clock: Clock,
    random: Random
) {
    private val dao = database.pendingOperationDao()
    private val retryDao = database.pendingOperationRetryDao()
    private val backoff = RetryBackoff(random)

    /**
     * Queues [operation] for [taskId], folding it into what is already waiting:
     * - an update is covered by a create or update not sent yet, which send the task as it is;
     * - a move joins a move not sent yet, and is not needed before the task was ever created;
     * - a delete drops what was never sent and, for a task the server never had, everything.
     *
     * Returns false when the server will never hear of the task: it was created and deleted
     * here before reaching it, so the caller can simply forget it.
     */
    suspend fun enqueue(accountId: Long, taskId: Long, operation: QueuedOperation): Boolean {
        val waiting = dao.forTask(accountId, taskId)
        val unsentCreate = waiting.any { it.type == OperationType.CREATE && it.neverSent() }
        when (operation) {
            QueuedOperation.CreateTask, is QueuedOperation.UploadAttachment -> insert(
                accountId,
                taskId,
                operation
            )

            QueuedOperation.UpdateTask -> if (!unsentCreate &&
                waiting.none { it.neverSent(OperationType.UPDATE) }
            ) {
                insert(accountId, taskId, operation)
            }

            is QueuedOperation.MoveTask -> if (!unsentCreate) {
                move(
                    accountId,
                    taskId,
                    operation,
                    waiting
                )
            }

            is QueuedOperation.DeleteTask -> {
                dao.delete(waiting.filter { it.neverSent() || it.failed }.map { it.id })
                if (!unsentCreate) insert(accountId, taskId, operation)
            }
        }
        return !(operation is QueuedOperation.DeleteTask && unsentCreate)
    }

    /** Two moves not sent yet are one move from the first list to the last; back home is none. */
    private suspend fun move(
        accountId: Long,
        taskId: Long,
        operation: QueuedOperation.MoveTask,
        waiting: List<PendingOperationEntity>
    ) {
        val pending = waiting.lastOrNull { it.neverSent(OperationType.MOVE) }
            ?: return insert(accountId, taskId, operation)
        val from = (QueuedOperation.decode(pending.payload) as QueuedOperation.MoveTask).from
        if (from == operation.to) {
            dao.delete(pending.id)
        } else {
            dao.replacePayload(pending.id, QueuedOperation.encode(operation.copy(from = from)))
        }
    }

    private suspend fun insert(accountId: Long, taskId: Long, operation: QueuedOperation) {
        dao.insert(
            PendingOperationEntity(
                accountId = accountId,
                type = operation.type,
                taskId = taskId,
                payload = QueuedOperation.encode(operation),
                createdAt = clock.instant()
            )
        )
    }

    /**
     * Runs the operations that are due, in the order they were queued. A task whose operation
     * is waiting or failed keeps its later operations waiting too; other tasks go on.
     */
    suspend fun process(accountId: Long, executor: OperationExecutor): ProcessResult {
        val blocked = mutableSetOf<Long>()
        var result = ProcessResult()
        while (true) {
            val now = clock.instant()
            val next = dao.all(accountId).firstOrNull { op ->
                val runnable = op.taskId !in blocked && !op.failed && !op.nextAttemptAt.isAfter(now)
                if (!runnable) blocked += op.taskId
                runnable
            } ?: break
            val outcome = run(next, executor)
            result = result.plus(outcome)
            when (outcome) {
                ExecutionResult.Done -> dao.delete(next.id)

                is ExecutionResult.Retry -> {
                    retryDao.recordFailure(
                        next.id,
                        now.plus(backoff.delay(next.attempts)),
                        outcome.reason
                    )
                    blocked += next.taskId
                }

                is ExecutionResult.Failed -> {
                    retryDao.markFailed(next.id, outcome.reason)
                    blocked += next.taskId
                }
            }
        }
        return result
    }

    /** Unexpected errors count as temporary, so an operation is never dropped by accident. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun run(
        operation: PendingOperationEntity,
        executor: OperationExecutor
    ): ExecutionResult = try {
        val maybeSent = operation.startedAt != null
        val decoded = QueuedOperation.decode(operation.payload)
        retryDao.markStarted(operation.id, clock.instant())
        executor.execute(operation.taskId, decoded, maybeSent)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SerializationException) {
        ExecutionResult.Failed(UNREADABLE)
    } catch (error: Exception) {
        ExecutionResult.Retry(error.message)
    }

    /** Makes a failed operation run again on the next sync. */
    suspend fun retry(operationId: Long) = retryDao.resetForRetry(operationId, clock.instant())

    /** Drops a failed operation; the local change stays, it is just not sent. */
    suspend fun discard(operationId: Long) = dao.delete(operationId)

    fun observePendingCount(accountId: Long): Flow<Int> = dao.observeCount(accountId)

    fun observeFailed(accountId: Long): Flow<List<PendingOperationEntity>> =
        retryDao.observeFailed(accountId)

    /**
     * Never handed to the server, not even partly: changing or dropping it is safe. Every run
     * marks the operation started first, so retried and failed ones count as sent.
     */
    private fun PendingOperationEntity.neverSent(ofType: OperationType? = null) =
        startedAt == null && (ofType == null || type == ofType)

    private companion object {
        const val UNREADABLE = "Unreadable operation"
    }
}

/** How many operations a [OperationQueue.process] run sent, postponed and failed. */
data class ProcessResult(val done: Int = 0, val retried: Int = 0, val failed: Int = 0) {
    fun plus(outcome: ExecutionResult) = when (outcome) {
        ExecutionResult.Done -> copy(done = done + 1)
        is ExecutionResult.Retry -> copy(retried = retried + 1)
        is ExecutionResult.Failed -> copy(failed = failed + 1)
    }
}
