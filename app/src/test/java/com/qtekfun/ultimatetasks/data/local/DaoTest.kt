// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local

import app.cash.turbine.test
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.PendingOperationEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.local.model.OperationType
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DaoTest {
    private val database = inMemoryDatabase()
    private val accounts = database.accountDao()
    private val lists = database.taskListDao()
    private val tasks = database.taskDao()
    private val operations = database.pendingOperationDao()

    @AfterEach
    fun close() = database.close()

    private suspend fun account(): Long = accounts.insert(
        AccountEntity(
            serverUrl = "https://cloud.example.com",
            userId = "ana",
            displayName = "Ana"
        )
    )

    private fun list(accountId: Long, href: String, name: String, order: Int? = null) =
        TaskListEntity(accountId = accountId, href = href, name = name, sortOrder = order)

    private fun task(accountId: Long, listHref: String, uid: String, order: Long? = null) =
        TaskEntity(
            accountId = accountId,
            listHref = listHref,
            href = "$listHref$uid.ics",
            uid = uid,
            summary = uid,
            sortOrder = order
        )

    @Test
    fun `accounts are stored, updated and observed`() = runTest {
        val id = account()
        accounts.update(accounts.get(id)!!.copy(calendarHome = "/remote.php/dav/calendars/ana/"))
        accounts.observeAll().test {
            assertEquals("/remote.php/dav/calendars/ana/", awaitItem().single().calendarHome)
        }
    }

    @Test
    fun `lists come in server order, then without order, then by name`() = runTest {
        val id = account()
        lists.upsert(
            listOf(
                list(id, "/b/", "beta"),
                list(id, "/a/", "Alpha"),
                list(id, "/c/", "Gamma", order = 2),
                list(id, "/d/", "Delta", order = 1)
            )
        )
        lists.observeAll(id).test {
            assertEquals(listOf("Delta", "Gamma", "Alpha", "beta"), awaitItem().map { it.name })
        }
    }

    @Test
    fun `lists are visible by default and can be hidden`() = runTest {
        val id = account()
        lists.upsert(listOf(list(id, "/a/", "A")))
        assertTrue(lists.get(id, "/a/")!!.visible)
        lists.setVisible(id, "/a/", false)
        assertEquals(false, lists.get(id, "/a/")!!.visible)
    }

    @Test
    fun `upserting a list keeps one row per href`() = runTest {
        val id = account()
        lists.upsert(listOf(list(id, "/a/", "A")))
        lists.upsert(listOf(list(id, "/a/", "Renamed")))
        lists.observeAll(id).test { assertEquals(listOf("Renamed"), awaitItem().map { it.name }) }
    }

    @Test
    fun `a list shows its tasks in manual order, without deleted ones`() = runTest {
        val id = account()
        lists.upsert(listOf(list(id, "/a/", "A")))
        tasks.insert(task(id, "/a/", "late"))
        tasks.insert(task(id, "/a/", "second", order = 20))
        tasks.insert(task(id, "/a/", "first", order = 10))
        val gone = tasks.insert(task(id, "/a/", "gone"))
        tasks.update(tasks.get(gone)!!.copy(deleted = true))
        tasks.observeList(id, "/a/").test {
            assertEquals(listOf("first", "second", "late"), awaitItem().map { it.uid })
        }
    }

    @Test
    fun `tasks keep every field, tags and instants included`() = runTest {
        val id = account()
        lists.upsert(listOf(list(id, "/a/", "A")))
        val done = Instant.parse("2026-10-03T10:15:30.123Z")
        val stored = task(id, "/a/", "full").copy(
            tags = listOf("casa", "con, coma"),
            completed = true,
            completedAt = done,
            due = "2026-10-05T09:30",
            dueZone = "Europe/Madrid",
            priority = 1,
            recurrence = "FREQ=WEEKLY",
            reminderBefore = 900
        )
        val read = tasks.get(tasks.insert(stored))!!
        assertEquals(stored.copy(id = read.id), read)
        assertEquals(listOf(read), tasks.byUid(id, "full"))
        tasks.delete(read.id)
        assertNull(tasks.get(read.id))
    }

    @Test
    fun `an empty tag list survives`() = runTest {
        val id = account()
        lists.upsert(listOf(list(id, "/a/", "A")))
        assertEquals(emptyList<String>(), tasks.get(tasks.insert(task(id, "/a/", "t")))!!.tags)
    }

    @Test
    fun `two tasks cannot share a resource`() = runTest {
        val id = account()
        lists.upsert(listOf(list(id, "/a/", "A")))
        tasks.insert(task(id, "/a/", "same"))
        assertThrows<Exception> { tasks.insert(task(id, "/a/", "same")) }
    }

    @Test
    fun `a task needs its list`() = runTest {
        val id = account()
        assertThrows<Exception> { tasks.insert(task(id, "/missing/", "t")) }
    }

    @Test
    fun `deleting a list deletes its tasks, deleting the account deletes everything`() = runTest {
        val id = account()
        lists.upsert(listOf(list(id, "/a/", "A"), list(id, "/b/", "B")))
        val inA = tasks.insert(task(id, "/a/", "a"))
        val inB = tasks.insert(task(id, "/b/", "b"))
        operations.insert(
            PendingOperationEntity(
                accountId = id,
                type = OperationType.CREATE,
                taskId = inB,
                createdAt = Instant.EPOCH
            )
        )
        lists.delete(id, "/a/")
        assertNull(tasks.get(inA))
        accounts.delete(id)
        assertNull(tasks.get(inB))
        assertNull(lists.get(id, "/b/"))
        assertEquals(emptyList<PendingOperationEntity>(), operations.all(id))
    }

    @Test
    fun `queued operations come back in the order they were made`() = runTest {
        val id = account()
        val first = operations.insert(
            PendingOperationEntity(
                accountId = id,
                type = OperationType.CREATE,
                taskId = 1,
                createdAt = Instant.EPOCH
            )
        )
        operations.insert(
            PendingOperationEntity(
                accountId = id,
                type = OperationType.UPDATE,
                taskId = 1,
                createdAt = Instant.EPOCH
            )
        )
        assertEquals(
            listOf(OperationType.CREATE, OperationType.UPDATE),
            operations.all(id).map {
                it.type
            }
        )
        operations.delete(first)
        assertEquals(listOf(OperationType.UPDATE), operations.all(id).map { it.type })
    }
}
