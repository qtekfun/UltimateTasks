// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.engine

import com.qtekfun.ultimatetasks.data.attachments.AttachmentFiles
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.remote.caldav.CalDav
import com.qtekfun.ultimatetasks.data.remote.caldav.DavResult
import com.qtekfun.ultimatetasks.sync.conflict.TaskField
import com.qtekfun.ultimatetasks.sync.queue.ExecutionResult
import com.qtekfun.ultimatetasks.sync.queue.OperationQueue
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation

/**
 * Uploads a chosen file to Nextcloud Files, then links it in the task with ATTACH and queues
 * that change (RF-11). The local copy goes once uploaded.
 */
class AttachmentUploader(
    private val dav: CalDav,
    database: UltimateTasksDatabase,
    private val queue: OperationQueue,
    private val files: AttachmentFiles
) {
    private val tasks = database.taskDao()
    private val uploads = database.uploadDao()

    suspend fun upload(
        uploadId: Long,
        onFailure: (DavResult<*>) -> ExecutionResult
    ): ExecutionResult {
        val upload = uploads.get(uploadId) ?: return ExecutionResult.Done
        val task = tasks.get(upload.taskId)
        val bytes = files.read(upload.uri)
        return when {
            task == null || task.deleted -> {
                forget(uploadId, upload.uri)
                ExecutionResult.Done
            }

            bytes == null -> {
                uploads.fail(uploadId, UNREADABLE)
                ExecutionResult.Failed(UNREADABLE)
            }

            else -> when (val result = dav.files.upload(upload.name, bytes, upload.mimeType)) {
                is DavResult.Success -> {
                    val current = tasks.get(task.id) ?: task
                    tasks.update(
                        current.copy(
                            attachments = current.attachments + result.value,
                            dirtyFields = current.dirtyFields or TaskField.ATTACHMENTS.bit
                        )
                    )
                    queue.enqueue(task.accountId, task.id, QueuedOperation.UpdateTask)
                    forget(uploadId, upload.uri)
                    ExecutionResult.Done
                }

                else -> onFailure(result).also {
                    if (it is ExecutionResult.Failed) uploads.fail(uploadId, it.reason)
                }
            }
        }
    }

    private suspend fun forget(uploadId: Long, path: String) {
        uploads.delete(uploadId)
        files.delete(path)
    }

    private companion object {
        const val UNREADABLE = "File not readable"
    }
}
