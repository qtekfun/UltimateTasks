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
import com.qtekfun.ultimatetasks.sync.conflict.TaskField
import com.qtekfun.ultimatetasks.sync.queue.ExecutionResult
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Pull and push against [FakeCalDav], with the real queue, merger and resolver. */
class SyncTest {
    @StartStop
    val server = MockWebServer()

    private val fake = FakeCalDav()
    private val db = inMemoryDatabase()
    private val clock = MutableClock()
    private val queue = OperationQueue(db, clock, FixedRandom(0.5))
    private val pull = PullSync(db, queue)
    private val tasks = db.taskDao()
    private lateinit var account: AccountEntity
    private lateinit var dav: CalDav
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
        dav =
            CalDavProvider(OkHttpClient(), { Credentials("ana", "secret") }, Dispatchers.Unconfined)
                .connect(account, allowInsecure = true)!!
        work = fake.addList("Work")
    }

    @AfterEach
    fun close() = db.close()

    private fun ics(uid: String, summary: String, notes: String = "") =
        IcsWriter.write(VtodoMapper.write(null, VtodoFields(uid, summary, notes), clock.now))

    private suspend fun pullAll(): DavResult<*>? = pull.pull(dav, db.accountDao().get(account.id)!!)

    private suspend fun push() =
        queue.process(account.id, TaskOperationExecutor(dav, db, queue, clock))

    private suspend fun local(href: String) = tasks.byHref(account.id, href)

    @Test
    fun `the first pull finds the home, the lists and their tasks`() = runTest {
        fake.put("${work}a.ics", ics("a", "Informe", "Para el lunes"))
        assertNull(pullAll())
        assertEquals(fake.home, db.accountDao().get(account.id)!!.calendarHome)
        assertEquals(listOf("Work"), db.taskListDao().all(account.id).map { it.name })
        val task = local("${work}a.ics")!!
        assertEquals("Informe" to "Para el lunes", task.summary to task.notes)
        assertEquals(fake.resources["${work}a.ics"]!!.etag, task.etag)
        assertNotNull(db.taskListDao().get(account.id, work)!!.syncToken)
        assertEquals(
            okhttp3.Credentials.basic("ana", "secret"),
            server.takeRequest().headers["Authorization"]
        )
    }

    @Test
    fun `later pulls only fetch what changed`() = runTest {
        fake.put("${work}a.ics", ics("a", "A"))
        fake.put("${work}b.ics", ics("b", "B"))
        pullAll()
        fake.put("${work}a.ics", ics("a", "A2"))
        fake.remove("${work}b.ics")
        fake.put("${work}c.ics", ics("c", "C"))
        fake.requests.clear()
        assertNull(pullAll())
        assertEquals("A2", local("${work}a.ics")!!.summary)
        assertNull(local("${work}b.ics"))
        assertEquals("C", local("${work}c.ics")!!.summary)
        assertEquals(1, fake.requests.count { it == "REPORT $work" && true } - 1)
    }

    @Test
    fun `an expired token pulls everything again and drops what the server no longer has`() =
        runTest {
            fake.put("${work}a.ics", ics("a", "A"))
            pullAll()
            db.taskListDao().setSyncToken(account.id, work, "http://sabre.io/ns/sync/unknown")
            fake.resources.remove("${work}a.ics")
            fake.put("${work}b.ics", ics("b", "B"))
            assertNull(pullAll())
            assertNull(local("${work}a.ics"))
            assertEquals("B", local("${work}b.ics")!!.summary)
        }

    @Test
    fun `lists keep their local settings, and go when the server drops them`() = runTest {
        val home = fake.addList("Home")
        pullAll()
        db.taskListDao().setVisible(account.id, home, false)
        pullAll()
        assertFalse(db.taskListDao().get(account.id, home)!!.visible)
        fake.lists.remove(home)
        fake.lists.remove(work)
        tasks.insert(
            TaskEntity(
                accountId = account.id,
                listHref = work,
                href = "${work}n.ics",
                uid = "n",
                summary = "Unsent"
            )
        )
        pullAll()
        assertNull(db.taskListDao().get(account.id, home))
        assertNotNull(db.taskListDao().get(account.id, work))
    }

    @Test
    fun `lists without sync tokens are listed whole, only when their ctag changed`() = runTest {
        val deck = fake.addList("Deck")
        fake.withoutSync += deck
        fake.put("${deck}card-1.ics", ics("card-1", "Tarjeta"))
        assertNull(pullAll())
        assertEquals("Tarjeta", local("${deck}card-1.ics")!!.summary)
        fake.requests.clear()
        assertNull(pullAll())
        assertFalse(fake.requests.any { it == "REPORT $deck" })
        fake.remove("${deck}card-1.ics")
        fake.put("${deck}card-2.ics", ics("card-2", "Otra"))
        assertNull(pullAll())
        assertNull(local("${deck}card-1.ics"))
        assertEquals("Otra", local("${deck}card-2.ics")!!.summary)
    }

    @Test
    fun `resources that are not tasks are ignored`() = runTest {
        fake.put(
            "${work}e.ics",
            "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nUID:e\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"
        )
        assertNull(pullAll())
        assertNull(local("${work}e.ics"))
    }

    @Test
    fun `a task created here is uploaded and becomes clean`() = runTest {
        pullAll()
        val id = tasks.insert(
            TaskEntity(
                accountId = account.id,
                listHref = work,
                href = "${work}new.ics",
                uid = "new",
                summary = "Nueva",
                dirtyFields = -1
            )
        )
        queue.enqueue(account.id, id, QueuedOperation.CreateTask)
        assertEquals(ProcessResult(done = 1), push())
        val uploaded = fake.resources["${work}new.ics"]!!
        assertTrue("SUMMARY:Nueva" in uploaded.ics)
        val task = tasks.get(id)!!
        assertEquals(uploaded.etag, task.etag)
        assertEquals(0, task.dirtyFields)
        assertEquals(uploaded.ics, task.ics)
    }

    @Test
    fun `a create whose answer was lost adopts the copy that arrived`() = runTest {
        pullAll()
        val id = tasks.insert(
            TaskEntity(
                accountId = account.id,
                listHref = work,
                href = "${work}n.ics",
                uid = "n",
                summary = "N",
                dirtyFields = 1
            )
        )
        queue.enqueue(account.id, id, QueuedOperation.CreateTask)
        fake.put("${work}n.ics", ics("n", "N"))
        db.pendingOperationRetryDao().markStarted(
            db.pendingOperationDao().all(account.id).single().id,
            clock.now
        )
        assertEquals(ProcessResult(done = 1), push())
        assertEquals(fake.resources["${work}n.ics"]!!.etag, tasks.get(id)!!.etag)
    }

    @Test
    fun `edits made while uploading stay dirty`() = runTest {
        fake.put("${work}a.ics", ics("a", "A"))
        pullAll()
        val task = local("${work}a.ics")!!
        tasks.update(task.copy(summary = "Edit 1", dirtyFields = TaskField.SUMMARY.bit))
        queue.enqueue(account.id, task.id, QueuedOperation.UpdateTask)
        val executor = TaskOperationExecutor(dav, db, queue, clock)
        fake.failures["${work}a.ics"] = ArrayDeque()
        server.dispatcher = object : mockwebserver3.Dispatcher() {
            override fun dispatch(
                request: mockwebserver3.RecordedRequest
            ): mockwebserver3.MockResponse {
                if (request.method == "PUT") {
                    kotlinx.coroutines.runBlocking {
                        tasks.update(tasks.get(task.id)!!.copy(summary = "Edit 2"))
                    }
                }
                return fake.dispatch(request)
            }
        }
        assertEquals(
            ExecutionResult.Done,
            executor.execute(task.id, QueuedOperation.UpdateTask, false)
        )
        assertEquals(TaskField.SUMMARY.bit, tasks.get(task.id)!!.dirtyFields)
    }

    @Test
    fun `title edited on both sides waits for the user and the server text is kept there`() =
        runTest {
            fake.put("${work}a.ics", ics("a", "Original"))
            pullAll()
            val task = local("${work}a.ics")!!
            tasks.update(
                task.copy(
                    summary = "Mío",
                    priority = 1,
                    dirtyFields =
                        TaskField.SUMMARY.bit or TaskField.PRIORITY.bit,
                    modifiedAt = clock.now
                )
            )
            queue.enqueue(account.id, task.id, QueuedOperation.UpdateTask)
            fake.put("${work}a.ics", ics("a", "Suyo"))
            // The update meets a newer server copy: it is merged and sent again on the next run.
            assertEquals(ProcessResult(retried = 1), push())
            val merged = tasks.get(task.id)!!
            assertEquals("Mío" to "Suyo", merged.summary to merged.conflictSummary)
            clock.advance(Duration.ofMinutes(1))
            // The retried update and the one queued by the merge both go; the second changes nothing.
            assertEquals(ProcessResult(done = 2), push())
            val sent = fake.resources["${work}a.ics"]!!.ics
            assertTrue("SUMMARY:Suyo" in sent && "PRIORITY:1" in sent)
            assertEquals(TaskField.SUMMARY.bit, tasks.get(task.id)!!.dirtyFields)
        }

    @Test
    fun `notes in conflict found by a pull`() = runTest {
        fake.put("${work}a.ics", ics("a", "A", "Notas"))
        pullAll()
        val task = local("${work}a.ics")!!
        tasks.update(task.copy(notes = "Mías", dirtyFields = TaskField.NOTES.bit))
        fake.put("${work}a.ics", ics("a", "A", "Suyas"))
        pullAll()
        val merged = tasks.get(task.id)!!
        assertEquals("Mías" to "Suyas", merged.notes to merged.conflictNotes)
        queue.enqueue(account.id, task.id, QueuedOperation.UpdateTask)
        push()
        assertTrue("DESCRIPTION:Suyas" in fake.resources["${work}a.ics"]!!.ics)
    }

    @Test
    fun `a local change the server did not touch survives a pull and is queued`() = runTest {
        fake.put("${work}a.ics", ics("a", "A"))
        pullAll()
        val task = local("${work}a.ics")!!
        tasks.update(task.copy(priority = 5, dirtyFields = TaskField.PRIORITY.bit))
        fake.put("${work}a.ics", ics("a", "A", "otra nota"))
        pullAll()
        val merged = tasks.get(task.id)!!
        assertEquals(5 to "otra nota", merged.priority to merged.notes)
        assertEquals(
            listOf(QueuedOperation.UpdateTask),
            db.pendingOperationDao().all(account.id).map {
                QueuedOperation.decode(it.payload)
            }
        )
    }

    @Test
    fun `deleted on the server, gone if untouched, kept and flagged if changed here`() = runTest {
        fake.put("${work}a.ics", ics("a", "A"))
        fake.put("${work}b.ics", ics("b", "B"))
        pullAll()
        val changed = local("${work}b.ics")!!
        tasks.update(changed.copy(summary = "B2", dirtyFields = TaskField.SUMMARY.bit))
        queue.enqueue(account.id, changed.id, QueuedOperation.UpdateTask)
        fake.remove("${work}a.ics")
        fake.remove("${work}b.ics")
        pullAll()
        assertNull(local("${work}a.ics"))
        val kept = tasks.get(changed.id)!!
        assertTrue(kept.deletedOnServer)
        assertNull(kept.etag)
        assertEquals(emptyList<Any>(), db.pendingOperationDao().all(account.id))
    }

    @Test
    fun `an update of a task the server deleted flags it`() = runTest {
        fake.put("${work}a.ics", ics("a", "A"))
        pullAll()
        val task = local("${work}a.ics")!!
        tasks.update(task.copy(summary = "A2", dirtyFields = TaskField.SUMMARY.bit))
        fake.resources.remove("${work}a.ics")
        val executor = TaskOperationExecutor(dav, db, queue, clock)
        assertEquals(
            ExecutionResult.Done,
            executor.execute(task.id, QueuedOperation.UpdateTask, false)
        )
        assertTrue(tasks.get(task.id)!!.deletedOnServer)
        assertEquals(
            ExecutionResult.Done,
            executor.execute(task.id, QueuedOperation.UpdateTask, false)
        )
    }

    @Test
    fun `a 412 for a task now gone flags it too`() = runTest {
        fake.put("${work}a.ics", ics("a", "A"))
        pullAll()
        val task = local("${work}a.ics")!!
        tasks.update(task.copy(summary = "A2", dirtyFields = TaskField.SUMMARY.bit))
        fake.failures["${work}a.ics"] = ArrayDeque(listOf(412))
        fake.resources.remove("${work}a.ics")
        val executor = TaskOperationExecutor(dav, db, queue, clock)
        assertEquals(
            ExecutionResult.Done,
            executor.execute(task.id, QueuedOperation.UpdateTask, false)
        )
        assertTrue(tasks.get(task.id)!!.deletedOnServer)
    }

    @Test
    fun `deletes and moves reach the server`() = runTest {
        val home = fake.addList("Home")
        fake.put("${work}a.ics", ics("a", "A"))
        fake.put("${work}b.ics", ics("b", "B"))
        pullAll()
        val a = local("${work}a.ics")!!
        tasks.update(a.copy(deleted = true))
        queue.enqueue(account.id, a.id, QueuedOperation.DeleteTask(a.href, a.etag))
        val b = local("${work}b.ics")!!
        tasks.update(b.copy(listHref = home, href = "${home}b.ics"))
        queue.enqueue(account.id, b.id, QueuedOperation.MoveTask(work, home))
        assertEquals(ProcessResult(done = 2), push())
        assertNull(fake.resources["${work}a.ics"])
        assertNull(tasks.get(a.id))
        assertNotNull(fake.resources["${home}b.ics"])
        val executor = TaskOperationExecutor(dav, db, queue, clock)
        assertEquals(
            ExecutionResult.Done,
            executor.execute(a.id, QueuedOperation.DeleteTask(a.href, null), true)
        )
        assertEquals(
            ExecutionResult.Done,
            executor.execute(b.id, QueuedOperation.MoveTask(work, home), true)
        )
    }

    @Test
    fun `operations on tasks that are gone or waiting for the user send nothing`() = runTest {
        pullAll()
        val executor = TaskOperationExecutor(dav, db, queue, clock)
        assertEquals(ExecutionResult.Done, executor.execute(99, QueuedOperation.UpdateTask, false))
        val id = tasks.insert(
            TaskEntity(
                accountId = account.id,
                listHref = work,
                href = "${work}x.ics",
                uid = "x",
                summary = "X",
                deleted = true
            )
        )
        assertEquals(ExecutionResult.Done, executor.execute(id, QueuedOperation.UpdateTask, false))
        tasks.update(tasks.get(id)!!.copy(deleted = false, deletedOnServer = true))
        assertEquals(
            ExecutionResult.Done,
            executor.execute(id, QueuedOperation.MoveTask(work, work), false)
        )
        fake.requests.clear()
        assertEquals(emptyList<String>(), fake.requests)
    }

    @Test
    fun `server refusals and errors become queue results`() = runTest {
        pullAll()
        val id = tasks.insert(
            TaskEntity(
                accountId = account.id,
                listHref = work,
                href = "${work}x.ics",
                uid = "x",
                summary = "X"
            )
        )
        val executor = TaskOperationExecutor(dav, db, queue, clock)
        fun fail(vararg codes: Int) =
            fake.failures.getOrPut("${work}x.ics") { ArrayDeque() }.addAll(codes.toList())
        fail(403, 400, 503, 401)
        assertEquals(
            ExecutionResult.Failed("Read-only list"),
            executor.execute(id, QueuedOperation.CreateTask, false)
        )
        assertEquals(
            ExecutionResult.Failed("HTTP 400"),
            executor.execute(id, QueuedOperation.CreateTask, false)
        )
        assertEquals(
            ExecutionResult.Retry("HTTP 503"),
            executor.execute(id, QueuedOperation.UpdateTask, false)
        )
        assertEquals(
            ExecutionResult.Retry("Unauthorized"),
            executor.execute(id, QueuedOperation.DeleteTask("${work}x.ics", null), false)
        )
        fake.failures["$work"] = ArrayDeque(listOf(500))
        fail(412)
        assertEquals(
            ExecutionResult.Retry("HTTP 500"),
            executor.execute(id, QueuedOperation.UpdateTask, false)
        )
        fake.failures["${work}x.ics"] = ArrayDeque(listOf(500))
        assertEquals(
            ExecutionResult.Retry("HTTP 500"),
            executor.execute(id, QueuedOperation.MoveTask(work, "/b/"), false)
        )
    }

    @Test
    fun `pull failures are reported`() = runTest {
        fake.failures[fake.home] = ArrayDeque(listOf(500))
        assertEquals(DavResult.HttpError(500), pullAll())
        fake.failures[work] = ArrayDeque(listOf(503))
        assertEquals(DavResult.HttpError(503), pullAll())
        fake.put("${work}a.ics", ics("a", "A"))
        fake.failures[work] = ArrayDeque(listOf(207, 401))
        // The sync report succeeds with an empty body only once, then the multiget fails.
        fake.failures[work] = ArrayDeque()
        server.dispatcher = object : mockwebserver3.Dispatcher() {
            override fun dispatch(request: mockwebserver3.RecordedRequest) =
                if ("calendar-multiget" in
                    request.body?.utf8().orEmpty()
                ) {
                    mockwebserver3.MockResponse(401)
                } else {
                    fake.dispatch(request)
                }
        }
        assertEquals(DavResult.Unauthorized, pullAll())
    }
}
