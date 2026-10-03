// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

/** A task made from text shared by another app (RF-13). */
data class SharedTask(val title: String, val notes: String, val url: String?)

object SharedText {
    private val URL = Regex("""https?://\S+""")
    private const val MAX_TITLE = 200

    /** Punctuation that ends a sentence, not the link. */
    private fun trimLink(text: String) = text.trimEnd('.', ',', ')', ']')

    /**
     * The subject (or else the first line) is the title, the first link goes to the URL field
     * and the rest of the text to the notes. A bare link becomes the title too, so the task is
     * never empty.
     */
    fun parse(subject: String?, text: String?): SharedTask {
        val body = text.orEmpty().trim()
        val url = URL.find(body)?.value?.let(::trimLink)
        // A line that is only the link is the link, not text.
        val lines = body.lines().map(String::trim).filter { it.isNotEmpty() && trimLink(it) != url }
        val title =
            subject?.trim()?.takeIf { it.isNotEmpty() } ?: lines.firstOrNull() ?: url.orEmpty()
        val notes = lines.filter { it != title }.joinToString("\n")
        return SharedTask(title.take(MAX_TITLE), notes, url)
    }
}
