// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.ical

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VtodoAttachmentsTest {
    private val todo = IcsParser.parse(
        "BEGIN:VTODO\r\nUID:t\r\n" +
            "ATTACH;FMTTYPE=image/png;FILENAME=/Tasks/foto.png;X-NC-FILE-TYPE=file:" +
            "https://c.example/f/42\r\n" +
            "ATTACH:https://example.com/docs/plan.pdf\r\n" +
            "ATTACH;ENCODING=BASE64;VALUE=BINARY:AAAA\r\n" +
            "BEGIN:VALARM\r\nEND:VALARM\r\nEND:VTODO\r\n"
    ).single()

    @Test
    fun `reads linked files, not inline data`() {
        assertEquals(
            listOf(
                IcsAttachment("https://c.example/f/42", "foto.png", "image/png"),
                IcsAttachment("https://example.com/docs/plan.pdf", "plan.pdf", null)
            ),
            VtodoAttachments.read(todo)
        )
    }

    @Test
    fun `keeps the lines still there, drops removed ones and adds new ones before subcomponents`() {
        val kept =
            VtodoAttachments.read(todo).take(1) +
                IcsAttachment("https://c.example/f/7", "nota.txt", "text/plain")
        val written = IcsWriter.write(VtodoAttachments.write(todo, kept)).replace("\r\n ", "")
        assertEquals(
            listOf(
                "ATTACH;FMTTYPE=image/png;FILENAME=/Tasks/foto.png;X-NC-FILE-TYPE=file:https://c.example/f/42",
                "ATTACH;ENCODING=BASE64;VALUE=BINARY:AAAA",
                "ATTACH;FMTTYPE=text/plain;FILENAME=/nota.txt;X-NC-FILE-TYPE=file:https://c.example/f/7"
            ),
            written.split("\r\n").filter { it.startsWith("ATTACH") }
        )
        assertEquals(true, written.indexOf("f/7") < written.indexOf("BEGIN:VALARM"))
        val noComponents = VtodoAttachments.write(
            IcsComponent("VTODO"),
            listOf(IcsAttachment("https://x/f/1", "a", null))
        )
        assertEquals(
            "ATTACH;FILENAME=/a;X-NC-FILE-TYPE=file:https://x/f/1",
            IcsWriter.contentLine(noComponents.properties.single())
        )
    }
}
