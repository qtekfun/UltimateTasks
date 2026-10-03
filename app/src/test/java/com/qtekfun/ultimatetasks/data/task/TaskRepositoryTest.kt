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
        repository.observeList("/b/").test { assertEquals("B", awaitItem()?.name) }
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
}
