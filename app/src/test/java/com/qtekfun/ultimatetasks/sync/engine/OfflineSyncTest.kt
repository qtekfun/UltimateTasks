// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.engine

import com.qtekfun.ultimatetasks.data.ical.IcsWriter
import com.qtekfun.ultimatetasks.data.ical.VtodoFields
import com.qtekfun.ultimatetasks.data.ical.VtodoMapper
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.inMemoryDatabase
import com.qtekfun.ultimatetasks.data.remote.Credentials
import com.qtekfun.ultimatetasks.data.remote.caldav.CalDav
import com.qtekfun.ultimatetasks.data.remote.caldav.CalDavProvider
import com.qtekfun.ultimatetasks.data.remote.caldav.DavResult
import com.qtekfun.ultimatetasks.data.settings.FakePreferences
import com.qtekfun.ultimatetasks.data.settings.PendingListPrefs
import com.qtekfun.ultimatetasks.sync.conflict.TaskField
import com.qtekfun.ultimatetasks.sync.queue.FixedRandom
import com.qtekfun.ultimatetasks.sync.queue.MutableClock
import com.qtekfun.ultimatetasks.sync.queue.OperationQueue
import com.qtekfun.ultimatetasks.sync.queue.ProcessResult
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Working without a connection, then coming back (T12): nothing is lost or sent twice. */
class OfflineSyncTest {
    @StartStop
    val server = MockWebServer()

    private val fake = FakeCalDav()
    private val db = inMemoryDatabase()
    private val clock = MutableClock()
    private val queue = OperationQueue(db, clock, FixedRandom(0.5))
    private val pull = PullSync(db, queue, PendingListPrefs(FakePreferences()))
    private val tasks = db.taskDao()
    private lateinit var account: AccountEntity
    private lateinit var online: CalDav
    private lateinit var offline: CalDav
    private lateinit var work: String

    @BeforeEach
    fun setUp() = runTest {
        server.dispatcher = fake
        val id = db.accountDao().insert(
            AccountEntity(
                serverUrl = server.url("/").toString(),
                userId = "ana",
                displayName = "Ana"
            )
        )
        account = db.accountDao().get(id)!!
        online = connect(account)
        // Nothing listens on port 1: every request fails as with no network.
        offline = connect(account.copy(serverUrl = "http://127.0.0.1:1/"))
        work = fake.addList("Work")
    }

    @AfterEach
    fun close() = db.close()

    private fun connect(account: AccountEntity) =
        CalDavProvider(OkHttpClient(), { Credentials("ana", "secret") }, Dispatchers.Unconfined)
            .connect(account, allowInsecure = true)!!

    private fun ics(uid: String, summary: String) =
        IcsWriter.write(VtodoMapper.write(null, VtodoFields(uid, summary, ""), clock.now))

    private suspend fun pullWith(dav: CalDav) = pull.pull(dav, db.accountDao().get(account.id)!!)

    private suspend fun pushWith(dav: CalDav) =
        queue.process(account.id, TaskOperationExecutor(dav, db, queue, clock))

    private suspend fun edit(id: Long, summary: String) {
        val task = tasks.get(id)!!
        tasks.update(
            task.copy(
                summary = summary,
                dirtyFields =
                    task.dirtyFields or TaskField.SUMMARY.bit
            )
        )
        queue.enqueue(account.id, id, QueuedOperation.UpdateTask)
    }

    private fun puts() = fake.requests.count { it.startsWith("PUT") }

    @Test
    fun `without network a pull fails and keeps the local changes`() = runTest {
        fake.put("${work}a.ics", ics("a", "A"))
        pullWith(online)
        val id = tasks.byHref(account.id, "${work}a.ics")!!.id
        edit(id, "Offline")
        assertTrue(pullWith(offline) is DavResult.NetworkError)
        val task = tasks.get(id)!!
        assertEquals("Offline" to TaskField.SUMMARY.bit, task.summary to task.dirtyFields)
        assertEquals(1, db.pendingOperationDao().all(account.id).size)
    }

    @Test
    fun `edits offline are merged and retried with backoff until back online`() = runTest {
        fake.put("${work}a.ics", ics("a", "A"))
        pullWith(online)
        val id = tasks.byHref(account.id, "${work}a.ics")!!.id
        edit(id, "One")
        assertEquals(ProcessResult(retried = 1), pushWith(offline))
        // The attempted update may have been sent, so later edits queue one more, merged.
        edit(id, "Two")
        edit(id, "Three")
        assertEquals(2, db.pendingOperationDao().all(account.id).size)
        // Back online, but the retry is not due yet: nothing goes.
        assertEquals(ProcessResult(), pushWith(online))
        assertEquals(0, puts())
        clock.advance(Duration.ofMinutes(1))
        assertEquals(ProcessResult(done = 2), pushWith(online))
        assertEquals(2, puts())
        assertTrue("SUMMARY:Three" in fake.resources["${work}a.ics"]!!.ics)
        assertEquals(0, tasks.get(id)!!.dirtyFields)
        assertTrue(db.pendingOperationDao().all(account.id).isEmpty())
    }

    @Test
    fun `a task created offline is uploaded once when the network comes back`() = runTest {
        pullWith(online)
        val id = tasks.insert(
            TaskEntity(
                accountId = account.id,
                listHref = work,
                href = "${work}n.ics",
                uid = "n",
                summary = "New",
                dirtyFields = -1
            )
        )
        queue.enqueue(account.id, id, QueuedOperation.CreateTask)
        edit(id, "New, edited")
        assertEquals(ProcessResult(retried = 1), pushWith(offline))
        clock.advance(Duration.ofMinutes(1))
        assertEquals(ProcessResult(done = 1), pushWith(online))
        assertEquals(1, puts())
        assertTrue("SUMMARY:New\\, edited" in fake.resources["${work}n.ics"]!!.ics)
    }

    @Test
    fun `an upload that reached the server before the app was closed ends clean`() = runTest {
        fake.put("${work}a.ics", ics("a", "A"))
        pullWith(online)
        val id = tasks.byHref(account.id, "${work}a.ics")!!.id
        edit(id, "Sent")
        val operation = db.pendingOperationDao().all(account.id).single()
        // The PUT arrived, but the app died before saving the new ETag.
        val sent = tasks.get(id)!!
        fake.put(
            "${work}a.ics",
            IcsWriter.write(VtodoMapper.write(null, VtodoFields("a", "Sent", ""), clock.now))
        )
        db.pendingOperationRetryDao().markStarted(operation.id, clock.now)
        pushWith(online)
        clock.advance(Duration.ofMinutes(1))
        pushWith(online)
        val task = tasks.get(id)!!
        assertEquals("Sent", task.summary)
        assertNull(task.conflictSummary)
        assertEquals(0, task.dirtyFields)
        assertEquals(fake.resources["${work}a.ics"]!!.etag, task.etag)
        assertTrue(sent.etag != task.etag)
        assertTrue(db.pendingOperationDao().all(account.id).isEmpty())
    }

    @Test
    fun `a delete made offline reaches the server later and nothing else is sent`() = runTest {
        fake.put("${work}a.ics", ics("a", "A"))
        pullWith(online)
        val task = tasks.byHref(account.id, "${work}a.ics")!!
        edit(task.id, "Edited, then deleted")
        tasks.update(tasks.get(task.id)!!.copy(deleted = true))
        queue.enqueue(account.id, task.id, QueuedOperation.DeleteTask(task.href, task.etag))
        assertEquals(ProcessResult(retried = 1), pushWith(offline))
        clock.advance(Duration.ofMinutes(1))
        pushWith(online)
        assertEquals(0, puts())
        assertNull(fake.resources["${work}a.ics"])
        assertTrue(db.pendingOperationDao().all(account.id).isEmpty())
    }
}
