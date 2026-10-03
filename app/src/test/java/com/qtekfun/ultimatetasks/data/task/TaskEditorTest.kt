// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.task

import app.cash.turbine.test
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.local.inMemoryDatabase
import com.qtekfun.ultimatetasks.sync.conflict.TaskField
import com.qtekfun.ultimatetasks.sync.engine.SyncScheduler
import com.qtekfun.ultimatetasks.sync.queue.FixedRandom
import com.qtekfun.ultimatetasks.sync.queue.MutableClock
import com.qtekfun.ultimatetasks.sync.queue.OperationQueue
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskEditorTest {
    private val db = inMemoryDatabase()
    private val clock = MutableClock()
    private val queue = OperationQueue(db, clock, FixedRandom(0.5))
    private val editor = TaskEditor(db, queue, mockk<SyncScheduler>(relaxed = true), clock)
    private val tasks = db.taskDao()

    @AfterEach
    fun close() = db.close()

    private suspend fun task(etag: String? = "\"1\""): TaskEntity {
        val account = db.accountDao().insert(
            AccountEntity(serverUrl = "https://c.example/", userId = "ana", displayName = "Ana")
        )
        db.taskListDao().upsert(
            listOf(TaskListEntity(account, "/a/", "A"), TaskListEntity(account, "/b/", "B"))
        )
        val id = tasks.insert(
            TaskEntity(
                accountId = account,
                listHref = "/a/",
                href = "/a/t.ics",
                uid = "t",
                summary = "T",
                etag = etag
            )
        )
        return tasks.get(id)!!
    }

    private suspend fun queued(task: TaskEntity) =
        db.pendingOperationDao().all(task.accountId).map {
            QueuedOperation.decode(it.payload)
        }

    @Test
    fun `edits mark only the fields that changed`() = runTest {
        val task = task()
        editor.update(task.id) { it.copy(summary = "Nuevo", priority = 1) }
        val edited = tasks.get(task.id)!!
        assertEquals(
            setOf(TaskField.SUMMARY, TaskField.PRIORITY),
            TaskField.fromBits(edited.dirtyFields)
        )
        assertEquals(clock.now, edited.modifiedAt)
        assertEquals(listOf(QueuedOperation.UpdateTask), queued(task))
        editor.update(task.id) { it }
        editor.update(99) { it.copy(summary = "x") }
        assertEquals(listOf(QueuedOperation.UpdateTask), queued(task))
        editor.observe(task.id).test { assertEquals("Nuevo", awaitItem()?.summary) }
    }

    @Test
    fun `moving changes list and resource, to the same list does nothing`() = runTest {
        val task = task()
        editor.move(task.id, "/a/")
        editor.move(task.id, "/b/")
        editor.move(99, "/b/")
        val moved = tasks.get(task.id)!!
        assertEquals("/b/" to "/b/t.ics", moved.listHref to moved.href)
        assertEquals(listOf(QueuedOperation.MoveTask("/a/", "/b/")), queued(task))
    }

    @Test
    fun `deleting waits for the server, unless it never had the task`() = runTest {
        val task = task()
        editor.delete(task.id)
        assertTrue(tasks.get(task.id)!!.deleted)
        assertEquals(listOf(QueuedOperation.DeleteTask("/a/t.ics", "\"1\"")), queued(task))
        val local = tasks.insert(task.copy(id = 0, href = "/a/new.ics", uid = "new", etag = null))
        queue.enqueue(task.accountId, local, QueuedOperation.CreateTask)
        editor.delete(local)
        assertNull(tasks.get(local))
        editor.delete(99)
    }

    @Test
    fun `text conflicts end with the chosen version`() = runTest {
        val task = task()
        val conflicted = task.copy(
            summary = "Mío",
            notes = "Mías",
            conflictSummary = "Suyo",
            conflictNotes = "Suyas",
            dirtyFields = TaskField.SUMMARY.bit or TaskField.NOTES.bit
        )
        tasks.update(conflicted)
        editor.resolve(task.id, TaskField.SUMMARY, ConflictChoice.SERVER)
        var now = tasks.get(task.id)!!
        assertEquals("Suyo" to null, now.summary to now.conflictSummary)
        assertEquals(TaskField.NOTES.bit, now.dirtyFields)
        assertEquals(emptyList<QueuedOperation>(), queued(task))
        editor.resolve(task.id, TaskField.NOTES, ConflictChoice.MINE)
        now = tasks.get(task.id)!!
        assertEquals("Mías" to null, now.notes to now.conflictNotes)
        assertEquals(listOf(QueuedOperation.UpdateTask), queued(task))
        tasks.update(now.copy(conflictNotes = "Otra"))
        editor.resolve(task.id, TaskField.NOTES, ConflictChoice.SERVER)
        assertEquals("Otra", tasks.get(task.id)!!.notes)
        // Nothing to resolve: nothing changes.
        editor.resolve(task.id, TaskField.SUMMARY, ConflictChoice.SERVER)
        assertEquals("Suyo", tasks.get(task.id)!!.summary)
        editor.resolve(99, TaskField.SUMMARY, ConflictChoice.MINE)
    }

    @Test
    fun `a task deleted on the server is uploaded again or discarded`() = runTest {
        val task = task()
        tasks.update(task.copy(deletedOnServer = true, etag = null))
        editor.keepCopy(task.id)
        val kept = tasks.get(task.id)!!
        assertFalse(kept.deletedOnServer)
        assertNull(kept.ics)
        assertEquals(listOf(QueuedOperation.CreateTask), queued(task))
        editor.discard(task.id)
        assertNull(tasks.get(task.id))
        editor.keepCopy(99)
    }
}
