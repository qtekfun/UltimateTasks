// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.attachments

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.qtekfun.ultimatetasks.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Keeps chosen files in the app's private storage until uploaded, so the upload does not
 * depend on the picker's temporary permission (RF-11). Files over [MAX_BYTES] are refused.
 */
class AndroidAttachmentFiles @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher
) : AttachmentFiles {
    private val folder get() = File(context.filesDir, "uploads").apply { mkdirs() }

    override suspend fun import(uri: String): ImportedFile? = withContext(io) {
        val source = uri.toUri()
        val resolver = context.contentResolver
        val name =
            resolver.query(
                source,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: source.lastPathSegment ?: DEFAULT_NAME
        val target = File(folder, UUID.randomUUID().toString())
        try {
            val copied = resolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            if (copied == null || copied > MAX_BYTES) {
                target.delete()
                null
            } else {
                ImportedFile(target.path, name, resolver.getType(source))
            }
        } catch (_: IOException) {
            target.delete()
            null
        } catch (_: SecurityException) {
            target.delete()
            null
        }
    }

    override suspend fun read(path: String): ByteArray? = withContext(io) {
        File(path).takeIf { it.isFile }?.readBytes()
    }

    override fun delete(path: String) {
        File(path).delete()
    }

    private companion object {
        /** Large enough for photos and documents, small enough for a phone's upload. */
        const val MAX_BYTES = 50L * 1024 * 1024
        const val DEFAULT_NAME = "attachment"
    }
}
