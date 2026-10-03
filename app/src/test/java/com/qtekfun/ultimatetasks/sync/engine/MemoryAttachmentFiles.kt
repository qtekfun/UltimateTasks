// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.engine

import com.qtekfun.ultimatetasks.data.attachments.AttachmentFiles
import com.qtekfun.ultimatetasks.data.attachments.ImportedFile

/** Chosen files kept in memory: [shared] are the uris a picker would give. */
class MemoryAttachmentFiles : AttachmentFiles {
    val shared = mutableMapOf<String, Pair<String, ByteArray>>()
    val stored = mutableMapOf<String, ByteArray>()

    override suspend fun import(uri: String): ImportedFile? = shared[uri]?.let { (name, bytes) ->
        val path = "/files/${stored.size}"
        stored[path] = bytes
        ImportedFile(path, name, "image/png")
    }

    override suspend fun read(path: String): ByteArray? = stored[path]

    override fun delete(path: String) {
        stored.remove(path)
    }
}
