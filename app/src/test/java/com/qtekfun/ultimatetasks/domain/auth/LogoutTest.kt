// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.auth

import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.auth.CredentialStore
import com.qtekfun.ultimatetasks.data.auth.FakeCipher
import com.qtekfun.ultimatetasks.data.auth.LoginFlowApiFactory
import com.qtekfun.ultimatetasks.data.local.entity.PendingOperationEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.local.inMemoryDatabase
import com.qtekfun.ultimatetasks.data.local.model.OperationType
import com.qtekfun.ultimatetasks.data.remote.Credentials
import com.qtekfun.ultimatetasks.data.remote.NextcloudJson
import com.qtekfun.ultimatetasks.data.remote.ServerUrl
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LogoutTest {
    @StartStop
    val server = MockWebServer()

    private val db = inMemoryDatabase()
    private val store = CredentialStore(db, FakeCipher())
    private val session = AccountSession(db, store)
    private val logout =
        Logout(LoginFlowApiFactory(OkHttpClient(), NextcloudJson), session, store, db)

    private var taskId = 0L

    @AfterEach
    fun close() = db.close()

    /** Signs in an account whose server is the local MockWebServer, with a list, a task and a queued operation. */
    private suspend fun signedIn(): Long {
        val url = (
            ServerUrl.parse(
                server.url("/nextcloud/").toString(),
                allowInsecure = true
            ) as ServerUrl.ParseResult.Valid
            )
        val accountId = session.signIn(url.url, Credentials("ana", "app-password"))
        db.taskListDao().upsert(listOf(TaskListEntity(accountId, "/l/", "List")))
        taskId = db.taskDao().insert(
            TaskEntity(
                accountId = accountId,
                listHref = "/l/",
                href = "/l/t.ics",
                uid = "t",
                summary = "T"
            )
        )
        db.pendingOperationDao().insert(
            PendingOperationEntity(
                accountId = accountId,
                type = OperationType.UPDATE,
                taskId = taskId,
                payload = "{}",
                createdAt = Instant.EPOCH,
                nextAttemptAt = Instant.EPOCH
            )
        )
        return accountId
    }

    @Test
    fun `revokes the app password and deletes the account with all its data`() = runBlocking {
        server.enqueue(MockResponse(200))
        val accountId = signedIn()

        logout.run(allowInsecure = true)

        val request = server.takeRequest()
        assertEquals(
            "DELETE /nextcloud/ocs/v2.php/core/apppassword",
            "${request.method} ${request.target}"
        )
        val basic = request.headers["Authorization"].orEmpty().removePrefix("Basic ")
        assertEquals("ana:app-password", String(Base64.getDecoder().decode(basic)))
        assertNull(session.activeAccount.first())
        assertNull(session.credentials())
        assertNull(store.load(accountId))
        assertNull(db.taskDao().get(taskId))
        assertNull(db.taskListDao().get(accountId, "/l/"))
        assertEquals(emptyList<PendingOperationEntity>(), db.pendingOperationDao().all(accountId))
    }

    @Test
    fun `still logs out when the server cannot be reached`() = runBlocking {
        val accountId = signedIn()
        server.close()

        logout.run(allowInsecure = true)

        assertNull(session.activeAccount.first())
        assertNull(store.load(accountId))
    }

    @Test
    fun `uses the stored credentials after an app restart`() = runBlocking {
        server.enqueue(MockResponse(200))
        signedIn()
        session.forget()

        logout.run(allowInsecure = true)

        assertEquals(true, server.takeRequest().headers["Authorization"]?.startsWith("Basic "))
    }

    @Test
    fun `does nothing without an account`() = runBlocking {
        logout()

        assertNull(session.activeAccount.first())
    }
}
