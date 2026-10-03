// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.task

import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.local.inMemoryDatabase
import com.qtekfun.ultimatetasks.data.remote.Credentials
import com.qtekfun.ultimatetasks.data.remote.caldav.CalDavProvider
import com.qtekfun.ultimatetasks.sync.engine.FakeCalDav
import com.qtekfun.ultimatetasks.sync.engine.SyncScheduler
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ListRepositoryTest {
    @StartStop
    val server = MockWebServer()

    private val fake = FakeCalDav()
    private val db = inMemoryDatabase()
    private val account = MutableStateFlow<AccountEntity?>(null)
    private val session = mockk<AccountSession> { every { activeAccount } returns account }
    private val provider = mockk<CalDavProvider>()
    private val repository =
        ListRepository(db, session, provider, mockk<SyncScheduler>(relaxed = true))
    private val lists = db.taskListDao()

    @AfterEach
    fun close() = db.close()

    private suspend fun signIn(home: String? = fake.home): Long {
        server.dispatcher = fake
        val id = db.accountDao().insert(
            AccountEntity(
                serverUrl = server.url("/").toString(),
                userId = "ana",
                displayName = "Ana",
                calendarHome = home
            )
        )
        val signed = db.accountDao().get(id)!!
        account.value = signed
        val dav = CalDavProvider(OkHttpClient(), {
            Credentials("ana", "pw")
        }, Dispatchers.Unconfined).connect(signed, allowInsecure = true)
        every { provider.connect(any()) } returns dav
        return id
    }

    @Test
    fun `a new list goes to the server and here, last in the order`() = runTest {
        val id = signIn(home = null)
        lists.upsert(listOf(TaskListEntity(id, "/x/", "X", sortOrder = 4)))
        assertEquals(ListChange.DONE, repository.create(ListLook(" Compra ", "#FF9500", "cart")))
        val created = lists.all(id).single { it.name == "Compra" }
        assertEquals(
            Triple("#FF9500", "cart", 5),
            Triple(created.color, created.icon, created.sortOrder)
        )
        assertEquals("Compra", fake.lists[created.href])
        assertTrue(created.href.startsWith(fake.home))
    }

    @Test
    fun `editing renames and recolors on the server, the icon stays here`() = runTest {
        val id = signIn()
        val href = fake.addList("Old")
        lists.upsert(listOf(TaskListEntity(id, href, "Old", "#000000")))
        assertEquals(ListChange.DONE, repository.edit(href, ListLook("New", "#34C759", "home")))
        assertEquals(
            Triple("New", "#34C759", "home"),
            lists.get(id, href)!!.let {
                Triple(it.name, it.color, it.icon)
            }
        )
        assertEquals("New", fake.lists[href])
        fake.proppatches.clear()
        assertEquals(ListChange.DONE, repository.edit(href, ListLook("New", "#34C759", "star")))
        assertEquals("star", lists.get(id, href)!!.icon)
        assertTrue(fake.proppatches.isEmpty())
        assertEquals(ListChange.FAILED, repository.edit("/missing/", ListLook("x", null, null)))
    }

    @Test
    fun `reordering saves only the lists whose place changed`() = runTest {
        val id = signIn()
        val a = fake.addList("A")
        val b = fake.addList("B")
        lists.upsert(
            listOf(
                TaskListEntity(id, a, "A", sortOrder = 0),
                TaskListEntity(id, b, "B", sortOrder = 1)
            )
        )
        assertEquals(ListChange.DONE, repository.reorder(listOf(b, a, "/unknown/")))
        assertEquals(
            listOf(1, 0),
            listOf(lists.get(id, a)!!.sortOrder, lists.get(id, b)!!.sortOrder)
        )
        assertEquals(2, fake.proppatches.size)
        fake.lists.remove(a)
        assertEquals(ListChange.FAILED, repository.reorder(listOf(a, b)))
    }

    @Test
    fun `deleting removes the list on the server and here`() = runTest {
        val id = signIn()
        val href = fake.addList("Gone")
        lists.upsert(listOf(TaskListEntity(id, href, "Gone")))
        assertEquals(ListChange.DONE, repository.delete(href))
        assertNull(lists.get(id, href))
        assertFalse(href in fake.lists)
        assertEquals(ListChange.FAILED, repository.delete(href))
    }

    @Test
    fun `without connection or account nothing changes`() = runTest {
        val id = signIn()
        server.close()
        assertEquals(ListChange.OFFLINE, repository.create(ListLook("X", null, null)))
        account.value = null
        assertEquals(ListChange.FAILED, repository.delete("/a/"))
        repository.setVisible("/a/", false)
        account.value = db.accountDao().get(id)
        lists.upsert(listOf(TaskListEntity(id, "/a/", "A")))
        every { provider.connect(any()) } returns null
        assertEquals(ListChange.FAILED, repository.delete("/a/"))
        repository.setVisible("/a/", false)
        assertFalse(lists.get(id, "/a/")!!.visible)
    }
}
