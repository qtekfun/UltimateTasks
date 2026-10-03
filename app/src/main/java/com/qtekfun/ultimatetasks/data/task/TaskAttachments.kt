// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.task

import com.qtekfun.ultimatetasks.data.attachments.AttachmentFiles
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.PendingUploadEntity
import com.qtekfun.ultimatetasks.sync.engine.SyncScheduler
import com.qtekfun.ultimatetasks.sync.queue.OperationQueue
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Attaching files to a task and taking them off (RF-11). Uploads wait in the queue. */
class TaskAttachments @Inject constructor(
    database: UltimateTasksDatabase,
    private val files: AttachmentFiles,
    private val queue: OperationQueue,
    private val scheduler: SyncScheduler,
    private val editor: TaskEditor
) {
    private val tasks = database.taskDao()
    private val uploads = database.uploadDao()

    fun observeUploads(taskId: Long): Flow<List<PendingUploadEntity>> =
        uploads.observeForTask(taskId)

    /** Copies the chosen file and queues its upload; false when it could not be read. */
    suspend fun attach(taskId: Long, uri: String): Boolean {
        val task = tasks.get(taskId)
        val file = task?.let { files.import(uri) }
        if (task == null || file == null) return false
        val id = uploads.insert(
            PendingUploadEntity(
                taskId = taskId,
                uri = file.path,
                name = file.name,
                mimeType = file.mimeType
            )
        )
        queue.enqueue(task.accountId, taskId, QueuedOperation.UploadAttachment(id))
        scheduler.requestSync()
        return true
    }

    /** Unlinks the file from the task; the file itself stays in Nextcloud Files. */
    suspend fun remove(taskId: Long, url: String) = editor.update(taskId) { task ->
        task.copy(attachments = task.attachments.filterNot { it.url == url })
    }

    /** Drops an upload that failed for good, and its local copy. */
    suspend fun discard(upload: PendingUploadEntity) {
        uploads.delete(upload.id)
        files.delete(upload.uri)
    }
}
