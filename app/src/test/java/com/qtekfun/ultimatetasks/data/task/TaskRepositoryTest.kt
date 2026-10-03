// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.task

import app.cash.turbine.test
import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.local.inMemoryDatabase
import com.qtekfun.ultimatetasks.data.local.model.SmartCounts
import com.qtekfun.ultimatetasks.domain.task.SmartList
import com.qtekfun.ultimatetasks.domain.task.TaskSource
import com.qtekfun.ultimatetasks.sync.conflict.TaskField
import com.qtekfun.ultimatetasks.sync.engine.SyncScheduler
import com.qtekfun.ultimatetasks.sync.queue.FixedRandom
import com.qtekfun.ultimatetasks.sync.queue.MutableClock
import com.qtekfun.ultimatetasks.sync.queue.OperationQueue
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskRepositoryTest {
    private val db = inMemoryDatabase()
    private val clock = MutableClock()
    private val queue = OperationQueue(db, clock, FixedRandom(0.5))
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val account = MutableStateFlow<AccountEntity?>(null)
    private val session = mockk<AccountSession> { every { activeAccount } returns account }
    private val repository = TaskRepository(db, session, queue, scheduler, clock)

    // The test clock ignores zones and reads as UTC.
    private val today = LocalDate.ofInstant(clock.now, ZoneOffset.UTC)

    @AfterEach
    fun close() = db.close()

    private suspend fun setUp(): Long {
        val id = db.accountDao().insert(
            AccountEntity(serverUrl = "https://c.example/", userId = "ana", displayName = "Ana")
        )
        account.value = db.accountDao().get(id)
        db.taskListDao().upsert(
            listOf(
                TaskListEntity(accountId = id, href = "/a/", name = "A", sortOrder = 1),
                TaskListEntity(accountId = id, href = "/b/", name = "B", sortOrder = 2),
                TaskListEntity(accountId = id, href = "/hidden/", name = "Hidden", visible = false)
            )
        )
        fun task(
            list: String,
            uid: String,
            due: String? = null,
            done: Boolean = false,
            deleted: Boolean = false
        ) = TaskEntity(
            accountId = id,
            listHref = list,
            href = "$list$uid.ics",
            uid = uid,
            summary = uid,
            due = due,
            completed = done,
            deleted = deleted
        )
        listOf(
            task("/a/", "overdue", due = today.minusDays(3).toString()),
            task("/a/", "today", due = "${today}T09:00"),
            task("/b/", "later", due = today.plusDays(5).toString()),
            task("/b/", "undated"),
            task("/b/", "done", done = true),
            task("/b/", "deleted", due = today.toString(), deleted = true),
            task("/hidden/", "hidden", due = today.toString())
        ).forEach { db.taskDao().insert(it) }
        return id
    }

    @Test
    fun `home counts only visible lists and lists their open tasks`() = runTest {
        setUp()
        repository.observeHome().test {
            val home = awaitItem()
            assertEquals(SmartCounts(today = 2, scheduled = 3, all = 4, completed = 1), home.counts)
            assertEquals(listOf("A" to 2, "B" to 2), home.lists.map { it.list.name to it.open })
        }
    }

    @Test
    fun `smart lists`() = runTest {
        setUp()
        suspend fun uids(kind: SmartList) =
            repository.observeTasks(TaskSource.Smart(kind)).let { flow ->
                var result = emptyList<String>()
                flow.test {
                    result = awaitItem().map { it.uid }
                    cancelAndIgnoreRemainingEvents()
                }
                result
            }
        assertEquals(listOf("overdue", "today"), uids(SmartList.TODAY))
        assertEquals(listOf("overdue", "today", "later"), uids(SmartList.SCHEDULED))
        assertEquals(listOf("overdue", "today", "later", "undated"), uids(SmartList.ALL))
        assertEquals(listOf("done"), uids(SmartList.COMPLETED))
        repository.observeTasks(TaskSource.List("/b/")).test {
            assertEquals(listOf("later", "undated", "done"), awaitItem().map { it.uid })
        }
    }

    @Test
    fun `completing saves at once, queues the change and asks for a sync`() = runTest {
        val id = setUp()
        val task = db.taskDao().byUid(id, "today").single()
        repository.setCompleted(task, true)
        val done = db.taskDao().get(task.id)!!
        assertTrue(done.completed)
        assertEquals(clock.now, done.completedAt)
        assertEquals(TaskField.COMPLETION.bit, done.dirtyFields)
        repository.setCompleted(done, false)
        assertNull(db.taskDao().get(task.id)!!.completedAt)
        assertEquals(
            listOf(QueuedOperation.UpdateTask),
            db.pendingOperationDao().all(id).map {
                QueuedOperation.decode(it.payload)
            }
        )
        verify(exactly = 2) { scheduler.requestSync() }
    }

    @Test
    fun `new tasks get their own resource and a create operation`() = runTest {
        val id = setUp()
        assertNull(repository.create("/a/", "   "))
        val created = db.taskDao().get(repository.create("/a/", "  Comprar pan ")!!)!!
        assertEquals("Comprar pan", created.summary)
        assertEquals("/a/${created.uid}.ics", created.href)
        assertEquals(
            listOf(QueuedOperation.CreateTask),
            db.pendingOperationDao().all(id).map {
                QueuedOperation.decode(it.payload)
            }
        )
        account.value = null
        assertNull(repository.create("/a/", "Sin cuenta"))
    }

    @Test
    fun `completing a repeating task moves it to its next date, undo puts it back`() = runTest {
        val id = setUp()
        val original = db.taskDao().byUid(id, "today").single()
        val weekly = original.copy(due = "2026-10-06T11:00", recurrence = "FREQ=WEEKLY;BYDAY=TU,TH")
        db.taskDao().update(weekly)
        repository.setCompleted(weekly, true)
        val moved = db.taskDao().get(weekly.id)!!
        assertEquals("2026-10-08T11:00", moved.due)
        assertEquals(false, moved.completed)
        assertEquals(
            setOf(TaskField.DUE, TaskField.START, TaskField.RECURRENCE),
            TaskField.fromBits(moved.dirtyFields)
        )
        repository.restore(weekly)
        assertEquals("2026-10-06T11:00", db.taskDao().get(weekly.id)!!.due)
        db.taskDao().delete(weekly.id)
        repository.restore(weekly)
        assertNull(db.taskDao().get(weekly.id))
    }

    @Test
    fun `the last occurrence of a series completes the task`() = runTest {
        val id = setUp()
        val last = db.taskDao().byUid(id, "today").single().copy(recurrence = "FREQ=DAILY;COUNT=1")
        db.taskDao().update(last)
        repository.setCompleted(last, true)
        assertTrue(db.taskDao().get(last.id)!!.completed)
    }

    @Test
    fun `hiding a list takes it out of the home screen and its counts`() = runTest {
        setUp()
        ListRepository(db, session, mockk(), mockk(relaxed = true)).setVisible("/b/", false)
        repository.observeHome().test {
            val home = awaitItem()
            assertEquals(listOf("A"), home.lists.map { it.list.name })
            assertEquals(SmartCounts(today = 2, scheduled = 2, all = 2, completed = 0), home.counts)
        }
        account.value = null
        ListRepository(db, session, mockk(), mockk(relaxed = true)).setVisible("/b/", true)
    }

    @Test
    fun `subtasks are created under their parent and observed with it`() = runTest {
        val id = setUp()
        val parent = db.taskDao().byUid(id, "today").single()
        val child = db.taskDao().get(repository.create(parent.listHref, "Hija", parent.uid)!!)!!
        assertEquals(parent.uid, child.parentUid)
        db.taskDao().observeChildren(id, parent.uid).test {
            assertEquals(listOf("Hija"), awaitItem().map { it.summary })
        }
    }
}
