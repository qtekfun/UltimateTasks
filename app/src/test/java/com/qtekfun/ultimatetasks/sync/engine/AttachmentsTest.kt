// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.engine

import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.inMemoryDatabase
import com.qtekfun.ultimatetasks.data.remote.Credentials
import com.qtekfun.ultimatetasks.data.remote.caldav.CalDav
import com.qtekfun.ultimatetasks.data.remote.caldav.CalDavProvider
import com.qtekfun.ultimatetasks.data.task.TaskAttachments
import com.qtekfun.ultimatetasks.data.task.TaskEditor
import com.qtekfun.ultimatetasks.sync.conflict.TaskField
import com.qtekfun.ultimatetasks.sync.queue.ExecutionResult
import com.qtekfun.ultimatetasks.sync.queue.FixedRandom
import com.qtekfun.ultimatetasks.sync.queue.MutableClock
import com.qtekfun.ultimatetasks.sync.queue.OperationQueue
import com.qtekfun.ultimatetasks.sync.queue.ProcessResult
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Attaching files (RF-11): from the picker to Nextcloud Files and the task's ATTACH. */
class AttachmentsTest {
    @StartStop
    val server = MockWebServer()

    private val fake = FakeCalDav()
    private val db = inMemoryDatabase()
    private val clock = MutableClock()
    private val queue = OperationQueue(db, clock, FixedRandom(0.5))
    private val files = MemoryAttachmentFiles()
    private val editor = TaskEditor(db, queue, mockk(relaxed = true), clock)
    private val attachments = TaskAttachments(db, files, queue, mockk(relaxed = true), editor)
    private lateinit var dav: CalDav
    private var taskId = 0L
    private var accountId = 0L

    @BeforeEach
    fun setUp() = runTest {
        server.dispatcher = fake
        accountId =
            db.accountDao().insert(
                AccountEntity(
                    serverUrl = server.url("/").toString(),
                    userId = "ana",
                    displayName = "Ana"
                )
            )
        dav = CalDavProvider(OkHttpClient(), { Credentials("ana", "pw") }, Dispatchers.Unconfined)
            .connect(db.accountDao().get(accountId)!!, allowInsecure = true)!!
        val list = fake.addList("Work")
        PullSync(
            db,
            queue,
            com.qtekfun.ultimatetasks.data.settings.PendingListPrefs(
                com.qtekfun.ultimatetasks.data.settings.FakePreferences()
            )
        )
            .pull(dav, db.accountDao().get(accountId)!!)
        taskId =
            db.taskDao().insert(
                TaskEntity(
                    accountId = accountId,
                    listHref = list,
                    href = "${list}t.ics",
                    uid = "t",
                    summary = "T",
                    etag = null
                )
            )
        files.shared["content://photo"] = "foto.png" to byteArrayOf(1, 2, 3)
    }

    @AfterEach
    fun close() = db.close()

    private suspend fun push() =
        queue.process(accountId, TaskOperationExecutor(dav, db, queue, clock, files))

    @Test
    fun `a chosen file is uploaded, linked and sent with the task`() = runTest {
        queue.enqueue(accountId, taskId, QueuedOperation.CreateTask)
        assertTrue(attachments.attach(taskId, "content://photo"))
        assertEquals(1, attachments.observeUploads(taskId).first().size)
        // Create, upload, and the update that sends the new link, in one run.
        assertEquals(ProcessResult(done = 3), push())
        val task = db.taskDao().get(taskId)!!
        val link = task.attachments.single()
        assertEquals("foto.png" to "image/png", link.name to link.mimeType)
        assertTrue(link.url.endsWith("/f/100"))
        assertTrue(
            fake.uploaded.keys.single().startsWith("/remote.php/dav/files/ana/Tasks/") &&
                fake.folderExists
        )
        assertEquals(emptyList<Any>(), attachments.observeUploads(taskId).first())
        assertTrue(files.stored.isEmpty())
        assertTrue(
            "ATTACH;FMTTYPE=image/png;FILENAME=/foto.png;X-NC-FILE-TYPE=file:" in
                fake.resources["${fake.home}work/t.ics"]!!.ics
        )
        attachments.remove(taskId, link.url)
        assertEquals(emptyList<Any>(), db.taskDao().get(taskId)!!.attachments)
    }

    @Test
    fun `nothing to attach without task or readable file`() = runTest {
        assertFalse(attachments.attach(99, "content://photo"))
        assertFalse(attachments.attach(taskId, "content://missing"))
    }

    @Test
    fun `unreadable files fail for good and can be discarded`() = runTest {
        attachments.attach(taskId, "content://photo")
        val upload = attachments.observeUploads(taskId).first().single()
        files.stored.clear()
        val executor = TaskOperationExecutor(dav, db, queue, clock, files)
        assertEquals(
            ExecutionResult.Failed("File not readable"),
            executor.execute(taskId, QueuedOperation.UploadAttachment(upload.id), false)
        )
        assertEquals("File not readable", attachments.observeUploads(taskId).first().single().error)
        attachments.discard(upload)
        assertEquals(emptyList<Any>(), attachments.observeUploads(taskId).first())
        assertEquals(
            ExecutionResult.Done,
            executor.execute(taskId, QueuedOperation.UploadAttachment(upload.id), false)
        )
    }

    @Test
    fun `server errors retry or fail, a deleted task drops the upload`() = runTest {
        attachments.attach(taskId, "content://photo")
        val upload = attachments.observeUploads(taskId).first().single()
        val executor = TaskOperationExecutor(dav, db, queue, clock, files)
        val operation = QueuedOperation.UploadAttachment(upload.id)
        server.dispatcher = object : mockwebserver3.Dispatcher() {
            override fun dispatch(request: mockwebserver3.RecordedRequest) = MockResponse(503)
        }
        assertEquals(ExecutionResult.Retry("HTTP 503"), executor.execute(taskId, operation, false))
        assertNull(attachments.observeUploads(taskId).first().single().error)
        server.dispatcher = object : mockwebserver3.Dispatcher() {
            override fun dispatch(request: mockwebserver3.RecordedRequest) = MockResponse(403)
        }
        assertEquals(
            ExecutionResult.Failed("Read-only list"),
            executor.execute(taskId, operation, false)
        )
        assertEquals("Read-only list", attachments.observeUploads(taskId).first().single().error)
        server.dispatcher = object : mockwebserver3.Dispatcher() {
            override fun dispatch(request: mockwebserver3.RecordedRequest) = if (request.method ==
                "PROPFIND"
            ) {
                MockResponse(
                    207,
                    okhttp3.Headers.headersOf(),
                    "<d:multistatus xmlns:d=\"DAV:\"/>"
                )
            } else {
                MockResponse(201)
            }
        }
        assertEquals(
            ExecutionResult.Retry("ParseError"),
            executor.execute(taskId, operation, false)
        )
        db.taskDao().update(db.taskDao().get(taskId)!!.copy(deleted = true))
        assertEquals(ExecutionResult.Done, executor.execute(taskId, operation, false))
        assertEquals(emptyList<Any>(), attachments.observeUploads(taskId).first())
    }
}
