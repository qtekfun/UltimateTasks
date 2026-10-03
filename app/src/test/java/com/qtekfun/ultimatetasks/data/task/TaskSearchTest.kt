// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.task

import app.cash.turbine.test
import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.local.inMemoryDatabase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TaskSearchTest {
    private val db = inMemoryDatabase()
    private val account = MutableStateFlow<AccountEntity?>(null)
    private val search = TaskSearch(
        db,
        mockk<AccountSession> {
            every { activeAccount } returns
                account
        }
    )

    @AfterEach
    fun close() = db.close()

    private suspend fun setUp() {
        val id = db.accountDao().insert(
            AccountEntity(serverUrl = "https://c.example/", userId = "ana", displayName = "Ana")
        )
        account.value = db.accountDao().get(id)
        db.taskListDao().upsert(
            listOf(
                TaskListEntity(id, "/a/", "A", sortOrder = 1),
                TaskListEntity(id, "/b/", "B", sortOrder = 0),
                TaskListEntity(id, "/hidden/", "Hidden", visible = false)
            )
        )
        fun task(
            list: String,
            uid: String,
            summary: String,
            notes: String = "",
            tags: List<String> = emptyList(),
            done: Boolean = false
        ) = TaskEntity(
            accountId = id,
            listHref = list,
            href = "$list$uid.ics",
            uid = uid,
            summary = summary,
            notes = notes,
            tags = tags,
            completed = done
        )
        listOf(
            task("/a/", "1", "Comprar pan"),
            task("/b/", "2", "Llamar", notes = "por el pan integral"),
            task("/a/", "3", "Otra", tags = listOf("panadería")),
            task("/a/", "4", "Pan hecho", done = true),
            task("/hidden/", "5", "Pan oculto"),
            task("/a/", "6", "100% listo")
        ).forEach { db.taskDao().insert(it) }
    }

    private suspend fun uids(query: String, completed: Boolean = false): List<String> {
        var result = emptyList<String>()
        search.observe(query, completed).test {
            result = awaitItem().map { it.uid }
            cancelAndIgnoreRemainingEvents()
        }
        return result
    }

    @Test
    fun `finds title, notes and tags of visible lists, by list order`() = runTest {
        setUp()
        assertEquals(listOf("2", "1", "3"), uids("PAN"))
        assertEquals(listOf("2", "1", "3", "4"), uids("pan", completed = true))
    }

    @Test
    fun `blank queries find nothing and wildcards are literal`() = runTest {
        setUp()
        assertEquals(emptyList<String>(), uids("   "))
        assertEquals(listOf("6"), uids("100%"))
        assertEquals(emptyList<String>(), uids("_"))
        assertEquals("%a\\%b\\_c\\\\d%", search.pattern("a%b_c\\d"))
    }
}
