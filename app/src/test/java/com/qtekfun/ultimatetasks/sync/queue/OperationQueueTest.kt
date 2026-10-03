// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.queue

import app.cash.turbine.test
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.PendingOperationEntity
import com.qtekfun.ultimatetasks.data.local.inMemoryDatabase
import com.qtekfun.ultimatetasks.data.local.model.OperationType
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation.CreateTask
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation.DeleteTask
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation.MoveTask
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation.UpdateTask
import java.time.Duration
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OperationQueueTest {
    private val db = inMemoryDatabase()
    private val dao = db.pendingOperationDao()
    private val clock = MutableClock()
    private val queue = OperationQueue(db, clock, FixedRandom(0.5))
    private val executor = ScriptedExecutor()

    @AfterEach
    fun close() = db.close()

    private suspend fun account() = db.accountDao().insert(
        AccountEntity(
            serverUrl = "https://cloud.example.com/",
            userId = "ana",
            displayName = "Ana"
        )
    )

    private suspend fun operations(accountId: Long) = dao.all(accountId).map {
        it.taskId to
            QueuedOperation.decode(it.payload)
    }

    /** Marks every queued operation as handed to the server, as a run cut short would. */
    private suspend fun started(accountId: Long) =
        dao.all(accountId).forEach { db.pendingOperationRetryDao().markStarted(it.id, clock.now) }

    @Nested
    inner class Enqueue {
        @Test
        fun `stores the operation with its type, task and time`() = runTest {
            val id = account()
            assertTrue(queue.enqueue(id, 7, MoveTask("/a/", "/b/")))
            val stored = dao.all(id).single()
            assertEquals(OperationType.MOVE, stored.type)
            assertEquals(7L, stored.taskId)
            assertEquals(clock.now, stored.createdAt)
            assertEquals(listOf(7L to MoveTask("/a/", "/b/")), operations(id))
        }

        @Test
        fun `updates are covered by a create or update not sent yet`() = runTest {
            val id = account()
            queue.enqueue(id, 1, CreateTask)
            queue.enqueue(id, 1, UpdateTask)
            queue.enqueue(id, 2, UpdateTask)
            queue.enqueue(id, 2, UpdateTask)
            assertEquals(listOf(1L to CreateTask, 2L to UpdateTask), operations(id))
        }

        @Test
        fun `an update after one that may have been sent is queued again`() = runTest {
            val id = account()
            queue.enqueue(id, 1, UpdateTask)
            started(id)
            queue.enqueue(id, 1, UpdateTask)
            queue.enqueue(id, 2, CreateTask)
            started(id)
            queue.enqueue(id, 2, UpdateTask)
            assertEquals(
                listOf(
                    1L to UpdateTask,
                    1L to UpdateTask,
                    2L to CreateTask,
                    2L to UpdateTask
                ),
                operations(id)
            )
        }

        @Test
        fun `moves not sent yet join into one, and back home cancels them`() = runTest {
            val id = account()
            queue.enqueue(id, 1, MoveTask("/a/", "/b/"))
            queue.enqueue(id, 1, MoveTask("/b/", "/c/"))
            assertEquals(listOf(1L to MoveTask("/a/", "/c/")), operations(id))
            queue.enqueue(id, 1, MoveTask("/c/", "/a/"))
            assertEquals(emptyList<Pair<Long, QueuedOperation>>(), operations(id))
        }

        @Test
        fun `a move after one that may have been sent is its own`() = runTest {
            val id = account()
            queue.enqueue(id, 1, MoveTask("/a/", "/b/"))
            started(id)
            queue.enqueue(id, 1, MoveTask("/b/", "/a/"))
            assertEquals(
                listOf(1L to MoveTask("/a/", "/b/"), 1L to MoveTask("/b/", "/a/")),
                operations(id)
            )
        }

        @Test
        fun `a task never created on the server needs no move`() = runTest {
            val id = account()
            queue.enqueue(id, 1, CreateTask)
            queue.enqueue(id, 1, MoveTask("/a/", "/b/"))
            assertEquals(listOf(1L to CreateTask), operations(id))
        }

        @Test
        fun `deleting drops what was never sent or failed, then queues the delete`() = runTest {
            val id = account()
            queue.enqueue(id, 1, UpdateTask)
            started(id)
            queue.enqueue(id, 1, MoveTask("/a/", "/b/"))
            val move = dao.all(id).last()
            db.pendingOperationRetryDao().markFailed(move.id, "read-only")
            queue.enqueue(id, 1, UpdateTask)
            queue.enqueue(id, 2, UpdateTask)
            assertTrue(queue.enqueue(id, 1, DeleteTask("/a/1.ics", "\"3\"")))
            assertEquals(
                listOf(
                    1L to UpdateTask,
                    2L to UpdateTask,
                    1L to DeleteTask("/a/1.ics", "\"3\"")
                ),
                operations(id)
            )
        }

        @Test
        fun `a task created and deleted here never reaches the server`() = runTest {
            val id = account()
            queue.enqueue(id, 1, CreateTask)
            queue.enqueue(id, 1, UpdateTask)
            assertFalse(queue.enqueue(id, 1, DeleteTask("/a/1.ics", null)))
            assertEquals(emptyList<Pair<Long, QueuedOperation>>(), operations(id))
        }

        @Test
        fun `a create that may have been sent still needs its delete`() = runTest {
            val id = account()
            queue.enqueue(id, 1, CreateTask)
            started(id)
            assertTrue(queue.enqueue(id, 1, DeleteTask("/a/1.ics", null)))
            assertEquals(
                listOf(1L to CreateTask, 1L to DeleteTask("/a/1.ics", null)),
                operations(id)
            )
        }
    }

    @Nested
    inner class Process {
        @Test
        fun `runs everything in order and empties the queue`() = runTest {
            val id = account()
            queue.enqueue(id, 1, CreateTask)
            queue.enqueue(id, 2, UpdateTask)
            queue.enqueue(id, 1, DeleteTask("/a/1.ics", null).also { started(id) })
            assertEquals(ProcessResult(done = 3), queue.process(id, executor))
            assertEquals(
                listOf(
                    1L to CreateTask,
                    2L to UpdateTask,
                    1L to DeleteTask("/a/1.ics", null)
                ),
                executor.calls
            )
            assertEquals(emptyList<PendingOperationEntity>(), dao.all(id))
        }

        @Test
        fun `a temporary error postpones the task with backoff and lets others go on`() = runTest {
            val id = account()
            queue.enqueue(id, 1, CreateTask)
            queue.enqueue(id, 1, MoveTask("/a/", "/b/").also { started(id) })
            queue.enqueue(id, 2, UpdateTask)
            executor.on(1, { ExecutionResult.Retry("timeout") })
            assertEquals(ProcessResult(done = 1, retried = 1), queue.process(id, executor))
            assertEquals(listOf(1L to CreateTask, 2L to UpdateTask), executor.calls)
            val waiting = dao.all(id).first()
            assertEquals(1, waiting.attempts)
            assertEquals("timeout", waiting.lastError)
            assertEquals(clock.now.plusSeconds(5), waiting.nextAttemptAt)
            assertEquals(ProcessResult(), queue.process(id, executor))
            clock.advance(Duration.ofSeconds(5))
            assertEquals(ProcessResult(done = 2), queue.process(id, executor))
            assertEquals(listOf(true, false, true, false), executor.maybeSent)
        }

        @Test
        fun `a refusal fails the operation until the user retries or discards it`() = runTest {
            val id = account()
            queue.enqueue(id, 1, UpdateTask)
            executor.on(1, { ExecutionResult.Failed("read-only list") })
            assertEquals(ProcessResult(failed = 1), queue.process(id, executor))
            queue.observeFailed(id).test {
                assertEquals("read-only list", awaitItem().single().lastError)
                assertEquals(ProcessResult(), queue.process(id, executor))
                queue.retry(dao.all(id).single().id)
                assertEquals(emptyList<PendingOperationEntity>(), awaitItem())
            }
            executor.on(1, { ExecutionResult.Failed("still read-only") })
            queue.process(id, executor)
            queue.observePendingCount(id).test {
                assertEquals(1, awaitItem())
                queue.discard(dao.all(id).single().id)
                assertEquals(0, awaitItem())
            }
        }

        @Test
        fun `unexpected errors are retried, unreadable payloads fail`() = runTest {
            val id = account()
            queue.enqueue(id, 1, UpdateTask)
            queue.enqueue(id, 2, UpdateTask)
            executor.on(1, { error("boom") })
            dao.replacePayload(dao.all(id).last().id, "{\"op\":\"unknown\"}")
            assertEquals(ProcessResult(retried = 1, failed = 1), queue.process(id, executor))
            assertEquals(listOf("boom", "Unreadable operation"), dao.all(id).map { it.lastError })
            queue.enqueue(id, 3, UpdateTask)
            dao.replacePayload(dao.all(id).last().id, "not json")
            assertEquals(ProcessResult(failed = 1), queue.process(id, executor))
        }

        @Test
        fun `cancellation stops the run and keeps the operation`() = runTest {
            val id = account()
            queue.enqueue(id, 1, UpdateTask)
            executor.on(1, { throw CancellationException("stopped") })
            assertThrows<CancellationException> { queue.process(id, executor) }
            assertEquals(1, dao.all(id).size)
        }
    }

    @Test
    fun `backoff doubles up to an hour, spread by up to 20 percent`() {
        assertEquals(Duration.ofSeconds(5), RetryBackoff(FixedRandom(0.5)).delay(0))
        assertEquals(Duration.ofSeconds(40), RetryBackoff(FixedRandom(0.5)).delay(3))
        assertEquals(Duration.ofHours(1), RetryBackoff(FixedRandom(0.5)).delay(99))
        assertEquals(Duration.ofSeconds(4), RetryBackoff(FixedRandom(0.0)).delay(0))
        assertEquals(Duration.ofSeconds(6), RetryBackoff(FixedRandom(1.0)).delay(0))
    }

    @Test
    fun `every upload is its own operation`() = runTest {
        val id = account()
        queue.enqueue(id, 1, QueuedOperation.UploadAttachment(1))
        queue.enqueue(id, 1, QueuedOperation.UploadAttachment(2))
        assertEquals(
            listOf(
                1L to QueuedOperation.UploadAttachment(1),
                1L to QueuedOperation.UploadAttachment(2)
            ),
            operations(id)
        )
    }

    @Test
    fun `an update does not merge into a waiting move`() = runTest {
        val id = account()
        queue.enqueue(id, 1, MoveTask("/a/", "/b/"))
        queue.enqueue(id, 1, UpdateTask)
        assertEquals(listOf(1L to MoveTask("/a/", "/b/"), 1L to UpdateTask), operations(id))
    }

    @Test
    fun `payloads missing a field are unreadable`() {
        listOf(
            """{"op":"move_task","from":"/a/"}""",
            """{"op":"delete_task","href":"/a/1.ics"}"""
        ).forEach {
            assertThrows<SerializationException> { QueuedOperation.decode(it) }
        }
    }

    @Test
    fun `payloads survive encoding`() {
        listOf(
            CreateTask,
            UpdateTask,
            MoveTask("/a/", "/b/"),
            DeleteTask("/a/1.ics", null)
        ).forEach {
            assertEquals(it, QueuedOperation.decode(QueuedOperation.encode(it)))
        }
    }

    @Test
    fun `an upload without its id is unreadable`() {
        assertThrows<SerializationException> {
            QueuedOperation.decode("""{"op":"upload_attachment"}""")
        }
        assertEquals(
            QueuedOperation.UploadAttachment(3),
            QueuedOperation.decode(QueuedOperation.encode(QueuedOperation.UploadAttachment(3)))
        )
    }
}
