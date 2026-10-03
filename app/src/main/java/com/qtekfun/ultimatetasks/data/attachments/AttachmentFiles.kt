// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.attachments

/** A file chosen by the user, copied into the app until it is uploaded (RF-11). */
data class ImportedFile(val path: String, val name: String, val mimeType: String?)

/** Where chosen files wait for their upload; Android's lives in [AndroidAttachmentFiles]. */
interface AttachmentFiles {
    /** Copies the content at [uri] into the app; null when it cannot be read or is too large. */
    suspend fun import(uri: String): ImportedFile?

    suspend fun read(path: String): ByteArray?

    fun delete(path: String)
}
