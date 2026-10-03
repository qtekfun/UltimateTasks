// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.ical

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class IcsParserTest {
    @Test
    fun `reads name, parameters and value`() {
        val property = IcsParser.parseLine("DUE;VALUE=DATE;X-A=1,2:20261003")
        assertEquals("DUE", property.name)
        assertEquals(
            listOf(IcsParameter("VALUE", "DATE"), IcsParameter("X-A", listOf("1", "2"))),
            property.parameters
        )
        assertEquals("20261003", property.value)
        assertEquals("DATE", property.parameter("value")?.value)
        assertNull(property.parameter("TZID"))
    }

    @Test
    fun `quoted parameter values may hold separators and the value may hold colons`() {
        val property = IcsParser.parseLine(
            "ATTACH;FILENAME=\"a;b:c,d\";FMTTYPE=text/plain:https://x/y"
        )
        assertEquals("a;b:c,d", property.parameter("FILENAME")?.value)
        assertEquals("https://x/y", property.value)
    }

    @Test
    fun `a parameter without value is kept empty`() {
        val property = IcsParser.parseLine("X-A;FLAG:v")
        assertEquals(listOf(IcsParameter("FLAG", emptyList())), property.parameters)
        assertEquals("v", property.value)
    }

    @Test
    fun `an unterminated quote ends the line`() {
        val property = IcsParser.parseLine("X-A;P=\"open")
        assertEquals("", property.name)
        assertEquals("X-A;P=\"open", property.value)
    }

    @Test
    fun `lines without a colon are kept as raw text`() {
        val property = IcsParser.parseLine("not a content line", raw = "not a content line\n")
        assertEquals("", property.name)
        assertEquals("not a content line\n", property.raw)
    }

    @Test
    fun `folded lines are joined`() {
        val calendar = IcsParser.parse(
            "BEGIN:VTODO\r\nSUMMARY:Hel\r\n lo\r\n\tworld\r\nEND:VTODO\r\n"
        ).single()
        assertEquals("Helloworld", calendar.property("SUMMARY")?.value)
    }

    @Test
    fun `components nest and a missing END is closed at the end`() {
        val text = "BEGIN:VCALENDAR\nBEGIN:VTODO\nBEGIN:VALARM\nACTION:DISPLAY\nEND:VALARM\nUID:1"
        val calendar = IcsParser.parse(text).single()
        val todo = calendar.components("VTODO").single()
        assertEquals("1", todo.property("UID")?.value)
        assertEquals("DISPLAY", todo.components("VALARM").single().property("ACTION")?.value)
        assertNull(todo.endRaw)
        assertEquals(text, IcsWriter.write(calendar).removeSuffix("END:VTODO\r\nEND:VCALENDAR\r\n"))
    }

    @Test
    fun `a stray END is kept as an ordinary line`() {
        val calendar = IcsParser.parse("BEGIN:VTODO\nEND:VEVENT\nEND:VTODO\n").single()
        assertEquals("VEVENT", calendar.property("END")?.value)
    }

    @Test
    fun `text outside components is ignored`() {
        assertEquals(emptyList<IcsComponent>(), IcsParser.parse("X-A:1\n"))
    }
}
