// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.ical

/** Escaping of TEXT values (RFC 5545 §3.3.11): backslash, semicolon, comma and line breaks. */
object IcsText {
    fun escape(text: String): String = buildString {
        for (char in text.replace("\r\n", "\n")) {
            when (char) {
                '\\' -> append("\\\\")
                ';' -> append("\\;")
                ',' -> append("\\,")
                '\n' -> append("\\n")
                else -> append(char)
            }
        }
    }

    fun unescape(value: String): String = buildString {
        var index = 0
        while (index < value.length) {
            val char = value[index]
            val next = value.getOrNull(index + 1)
            if (char == '\\' && next != null) {
                append(if (next == 'n' || next == 'N') '\n' else next)
                index += 2
            } else {
                append(char)
                index++
            }
        }
    }
}
