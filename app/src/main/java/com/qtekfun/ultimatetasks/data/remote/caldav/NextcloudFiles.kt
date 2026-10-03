// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.remote.caldav

import com.qtekfun.ultimatetasks.data.ical.IcsAttachment
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/**
 * Uploads attachments to the user's Nextcloud Files, in a "Tasks" folder (RF-11), and links
 * them the way Nextcloud Calendar does: `<server>/f/<file id>`, which opens the file in the web.
 */
class NextcloudFiles(
    client: OkHttpClient,
    private val server: HttpUrl,
    io: CoroutineDispatcher,
    userId: String
) {
    private val http = DavHttp(client, server, io)
    private val folder = "remote.php/dav/files/$userId/$FOLDER/"

    suspend fun upload(
        name: String,
        bytes: ByteArray,
        mimeType: String?
    ): DavResult<IcsAttachment> {
        val path = folder + safe(name)
        val put = http.sendBytes(path, bytes, mimeType ?: OCTET_STREAM)
        // The folder does not exist yet the first time: create it and try again.
        val uploaded = if (put == DavResult.HttpError(CONFLICT)) {
            http.send("MKCOL", folder).then {
                http.sendBytes(path, bytes, mimeType ?: OCTET_STREAM)
            }
        } else {
            put
        }
        return uploaded.then {
            http.send(
                "PROPFIND",
                path,
                DavXml.propfind("<oc:fileid xmlns:oc=\"$OWNCLOUD\"/>"),
                mapOf("Depth" to "0")
            )
        }.then { answer ->
            val id = DavXml.multistatus(
                answer.body
            )?.responses?.firstOrNull()?.text(OWNCLOUD, "fileid")
            if (id ==
                null
            ) {
                DavResult.ParseError
            } else {
                DavResult.Success(
                    IcsAttachment(server.resolve("f/$id").toString(), name, mimeType)
                )
            }
        }
    }

    /** No folders in the name, and a prefix so two files with one name do not clash. */
    private fun safe(name: String) =
        System.currentTimeMillis().toString() + "-" + name.replace('/', '_')

    private companion object {
        const val FOLDER = "Tasks"
        const val OWNCLOUD = "http://owncloud.org/ns"
        const val OCTET_STREAM = "application/octet-stream"
        const val CONFLICT = 409
    }
}
