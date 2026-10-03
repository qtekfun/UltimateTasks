// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.detail

import com.qtekfun.ultimatetasks.data.local.entity.PendingUploadEntity
import com.qtekfun.ultimatetasks.data.task.TaskAttachments

/** File actions of the detail screen (RF-11); [run] executes them on the task shown. */
class DetailFiles(
    private val attachments: TaskAttachments,
    private val run: (suspend (Long) -> Unit) -> Unit
) {
    fun attach(uri: String) = run { attachments.attach(it, uri) }

    fun remove(url: String) = run { attachments.remove(it, url) }

    fun discard(upload: PendingUploadEntity) = run { attachments.discard(upload) }
}
