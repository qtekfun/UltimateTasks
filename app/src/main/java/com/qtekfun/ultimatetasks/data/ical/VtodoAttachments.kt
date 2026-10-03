// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.ical

import kotlinx.serialization.Serializable

/** A file linked to a task (RF-11): its URL, the name to show and its type. */
@Serializable
data class IcsAttachment(val url: String, val name: String, val mimeType: String? = null)

/**
 * `ATTACH` properties that link to files (RF-11). Inline attachments (`ENCODING=BASE64`) are
 * not files of the server: they are kept in the task untouched and not shown.
 */
object VtodoAttachments {
    fun read(todo: IcsComponent): List<IcsAttachment> = todo.properties
        .filter {
            it.name.equals("ATTACH", true) && it.parameter("ENCODING") == null &&
                it.value.isNotBlank()
        }
        .map { property ->
            val url = property.value.trim()
            val name =
                property.parameter("FILENAME")?.value?.substringAfterLast('/')?.takeIf {
                    it.isNotBlank()
                }
                    ?: url.substringAfterLast('/')
            IcsAttachment(url, name, property.parameter("FMTTYPE")?.value)
        }

    /**
     * Keeps the ATTACH lines of attachments still there as they were, drops the removed ones
     * and adds the new ones, as Nextcloud Calendar writes them.
     */
    fun write(todo: IcsComponent, attachments: List<IcsAttachment>): IcsComponent {
        val wanted = attachments.map { it.url }.toSet()
        val kept = todo.children.filterNot { node ->
            node is IcsProperty && node.name.equals("ATTACH", true) &&
                node.parameter("ENCODING") == null &&
                node.value.trim() !in wanted
        }
        val present = kept.filterIsInstance<IcsProperty>().filter {
            it.name.equals("ATTACH", true)
        }.map { it.value.trim() }.toSet()
        val added = attachments.filter { it.url !in present }.map(::property)
        val firstComponent = kept.indexOfFirst { it is IcsComponent }.let {
            if (it <
                0
            ) {
                kept.size
            } else {
                it
            }
        }
        return todo.copy(children = kept.toMutableList().apply { addAll(firstComponent, added) })
    }

    private fun property(attachment: IcsAttachment) = IcsProperty(
        "ATTACH",
        listOfNotNull(
            attachment.mimeType?.let { IcsParameter("FMTTYPE", it) },
            IcsParameter("FILENAME", "/" + attachment.name),
            IcsParameter("X-NC-FILE-TYPE", "file")
        ),
        attachment.url
    )
}
