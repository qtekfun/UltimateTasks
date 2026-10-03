// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.ical

import java.io.File
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class VtodoMapperTest {
    private val now = Instant.parse("2026-10-03T12:00:00Z")
    private val stamp = "20261003T120000Z"

    private fun corpus(name: String) =
        File(requireNotNull(javaClass.getResource("/ics-corpus/$name")).toURI()).readText()

    private fun calendar(text: String) = IcsParser.parse(text).single()

    /** Writes [change] applied to [text] and returns the unfolded lines of the result. */
    private fun edit(text: String, change: (VtodoFields) -> VtodoFields): List<String> {
        val calendar = calendar(text)
        val written = IcsWriter.write(
            VtodoMapper.write(calendar, change(VtodoMapper.read(calendar)!!), now)
        )
        return lines(written)
    }

    /** A calendar with one task made of [body] lines. */
    private fun todo(vararg body: String) =
        (listOf("BEGIN:VCALENDAR", "BEGIN:VTODO") + body + listOf("END:VTODO", "END:VCALENDAR"))
            .joinToString("\r\n", postfix = "\r\n")

    private fun lines(text: String) =
        text.replace("\r\n", "\n").replace("\n ", "").split("\n").filter {
            it.isNotEmpty()
        }

    @Test
    fun `reads what Nextcloud-style clients write`() {
        val fields = VtodoMapper.read(calendar(corpus("edge-cases/crlf-folded-alarm.ics")))!!
        assertEquals("edge-1@ultimatetasks", fields.uid)
        assertTrue(fields.summary.startsWith("Llamar a María, revisar el presupuesto; y enviar"))
        assertTrue(fields.summary.endsWith("se pliegue a mitad"))
        assertEquals("Primera línea\nSegunda línea", fields.notes)
        assertEquals(IcsDate("2026-10-05T09:30", "Europe/Madrid"), fields.due)
        assertEquals(listOf("Casa", "Urgente"), fields.tags)
        assertEquals(123456L, fields.sortOrder)
        assertEquals(900L, fields.reminderBefore)
        assertFalse(fields.completed)
    }

    @Test
    fun `reads the RFC sample fields`() {
        val fields = VtodoMapper.read(calendar(corpus("synctools/most-fields1.ics")))!!
        assertEquals(1, fields.priority)
        assertEquals(listOf("Test", "Sample"), fields.tags)
        assertEquals("FREQ=YEARLY;INTERVAL=2", fields.recurrence)
        assertEquals(IcsDate("2010-10-01"), fields.due)
        assertEquals(IcsDate("2010-01-01"), fields.start)
        assertEquals("http://example.com/pub/calendars/jsmith/mytime.ics", fields.url)
        assertNull(fields.parentUid)
        assertEquals(Instant.parse("1996-08-17T13:30:00Z"), fields.modifiedAt)
        assertFalse(fields.completed)
    }

    @TestFactory
    fun `writing the fields read leaves the file byte for byte`() =
        File(requireNotNull(javaClass.getResource("/ics-corpus")).toURI()).walk()
            .filter { it.extension == "ics" }
            .map { file ->
                DynamicTest.dynamicTest(file.name) {
                    val calendar = calendar(file.readText())
                    val fields = VtodoMapper.read(calendar) ?: return@dynamicTest
                    assertEquals(calendar, VtodoMapper.write(calendar, fields, now))
                }
            }.toList()

    @Test
    fun `a change rewrites its line and the stamps, nothing else`() {
        val text = corpus("synctools/most-fields1.ics")
        val before = lines(text)
        val after = edit(text) { it.copy(summary = "Nuevo, título") }
        val changed = after - before.toSet()
        assertEquals(
            setOf(
                "SUMMARY:Nuevo\\, título",
                "LAST-MODIFIED:$stamp",
                "DTSTAMP:$stamp",
                "SEQUENCE:2"
            ),
            changed.toSet()
        )
        assertEquals(
            before.filterNot {
                it.startsWith("LAST-MODIFIED") || it.startsWith("SEQUENCE")
            },
            after.filter {
                it in
                    before
            }
        )
    }

    @Test
    fun `completing and reopening`() {
        val text = corpus("synctools/most-fields1.ics")
        val done = edit(text) { it.copy(completed = true, completedAt = now) }
        assertTrue("STATUS:COMPLETED" in done)
        assertTrue("COMPLETED:$stamp" in done)
        assertTrue("PERCENT-COMPLETE:100" in done)
        val reopened = lines(
            IcsWriter.write(
                VtodoMapper.write(
                    calendar(done.joinToString("\r\n", postfix = "\r\n")),
                    VtodoMapper.read(calendar(text))!!,
                    now
                )
            )
        )
        assertTrue("STATUS:NEEDS-ACTION" in reopened)
        assertFalse(
            reopened.any {
                it.startsWith("COMPLETED") || it.startsWith("PERCENT-COMPLETE")
            }
        )
        val plain =
            edit(newTask(VtodoFields("u", completed = true, completedAt = now))) {
                it.copy(completed = false, completedAt = null)
            }
        assertTrue("STATUS:NEEDS-ACTION" in plain)
    }

    @Test
    fun `a completed status without date, or a completion date without status`() {
        val noDate = VtodoMapper.fields(
            IcsComponent("VTODO", listOf(IcsProperty("STATUS", value = "COMPLETED")))
        )
        assertTrue(noDate.completed)
        assertNull(noDate.completedAt)
        val noStatus = VtodoMapper.fields(
            IcsComponent("VTODO", listOf(IcsProperty("COMPLETED", value = stamp)))
        )
        assertTrue(noStatus.completed)
        assertEquals(now, noStatus.completedAt)
        val done = edit(newTask(VtodoFields("u"))) { it.copy(completed = true) }
        assertFalse(done.any { it.startsWith("COMPLETED:") })
    }

    private fun newTask(fields: VtodoFields) = IcsWriter.write(VtodoMapper.write(null, fields, now))

    @Test
    fun `a new task is a complete calendar with its time zone`() {
        val fields = VtodoFields(
            uid = "new-1",
            summary = "Comprar pan",
            notes = "Integral",
            url = "https://example.com",
            due = IcsDate("2026-10-05T09:30", "Europe/Madrid"),
            start = IcsDate("2026-10-04"),
            priority = 5,
            tags = listOf("casa", "a,b"),
            parentUid = "parent-1",
            sortOrder = 7,
            recurrence = "FREQ=WEEKLY;BYDAY=MO",
            reminderBefore = 600
        )
        val text = newTask(fields)
        val lines = lines(text)
        assertEquals(
            listOf(
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:${VtodoMapper.PRODUCT_ID}",
                "BEGIN:VTIMEZONE"
            ),
            lines.take(4)
        )
        assertTrue(lines.indexOf("BEGIN:VTIMEZONE") < lines.indexOf("BEGIN:VTODO"))
        assertTrue("CREATED:$stamp" in lines)
        assertTrue("DUE;TZID=Europe/Madrid:20261005T093000" in lines)
        assertTrue("CATEGORIES:casa,a\\,b" in lines)
        assertTrue("RELATED-TO;RELTYPE=PARENT:parent-1" in lines)
        assertTrue("TRIGGER;RELATED=END:-PT10M" in lines)
        assertFalse(lines.any { it.startsWith("SEQUENCE") })
        assertEquals(fields.copy(modifiedAt = now), VtodoMapper.read(calendar(text)))
    }

    @Test
    fun `UTC and floating dates need no time zone, a known one is not repeated`() {
        assertFalse(
            "BEGIN:VTIMEZONE" in
                lines(newTask(VtodoFields("u", due = IcsDate("2026-10-05T09:30", "UTC"))))
        )
        assertFalse(
            "BEGIN:VTIMEZONE" in lines(newTask(VtodoFields("u", due = IcsDate("2026-10-05T09:30"))))
        )
        val again =
            edit(corpus("edge-cases/crlf-folded-alarm.ics")) {
                it.copy(due = IcsDate("2026-10-06T09:30", "Europe/Madrid"))
            }
        assertEquals(1, again.count { it == "BEGIN:VTIMEZONE" })
        val unknown = newTask(VtodoFields("u", due = IcsDate("2026-10-05T09:30", "Mars/Olympus")))
        assertFalse("BEGIN:VTIMEZONE" in lines(unknown))
    }

    @Test
    fun `clearing fields removes their properties`() {
        val text = newTask(
            VtodoFields(
                "u", summary = "s", notes = "n", url = "https://x",
                due = IcsDate(
                    "2026-10-05"
                ),
                priority = 1,
                tags = listOf(
                    "t"
                ),
                parentUid = "p", sortOrder = 1, recurrence = "FREQ=DAILY", reminderBefore = 60
            )
        )
        val cleared = edit(text) { VtodoFields(uid = it.uid) }
        listOf(
            "SUMMARY", "DESCRIPTION", "URL", "DUE", "PRIORITY", "CATEGORIES", "RELATED-TO",
            "X-APPLE-SORT-ORDER", "RRULE", "BEGIN:VALARM"
        )
            .forEach { name -> assertFalse(cleared.any { it.startsWith(name) }, name) }
    }

    @Test
    fun `tags from several CATEGORIES lines are merged, empty ones dropped`() {
        val todo = IcsParser.parse(
            "BEGIN:VTODO\nCATEGORIES:a,b\\,c\nCATEGORIES: ,d\nEND:VTODO\n"
        ).single()
        assertEquals(listOf("a", "b,c", "d"), VtodoMapper.fields(todo).tags)
    }

    @Test
    fun `odd priorities and empty values read as nothing`() {
        val todo = IcsParser.parse(
            "BEGIN:VTODO\nPRIORITY:12\nURL: \nRRULE:\nRELATED-TO;RELTYPE=CHILD:c\nEND:VTODO\n"
        ).single()
        val fields = VtodoMapper.fields(todo)
        assertEquals(0, fields.priority)
        assertNull(fields.url)
        assertNull(fields.recurrence)
        assertNull(fields.parentUid)
        assertEquals("", fields.uid)
    }

    @Test
    fun `changing the parent keeps other links in place`() {
        val text = todo("UID:u", "RELATED-TO:old", "RELATED-TO;RELTYPE=SIBLING:s")
        assertEquals("old", VtodoMapper.read(calendar(text))!!.parentUid)
        val moved = edit(text) { it.copy(parentUid = "new") }
        assertEquals(
            listOf("RELATED-TO;RELTYPE=PARENT:new", "RELATED-TO;RELTYPE=SIBLING:s"),
            moved.filter {
                it.startsWith("RELATED-TO")
            }
        )
        val removed = edit(text) { it.copy(parentUid = null) }
        assertEquals(
            listOf("RELATED-TO;RELTYPE=SIBLING:s"),
            removed.filter {
                it.startsWith("RELATED-TO")
            }
        )
        val alarmFirst = todo("UID:u", "BEGIN:VALARM", "END:VALARM")
        val added = edit(alarmFirst) { it.copy(parentUid = "p") }
        assertTrue(added.indexOf("RELATED-TO;RELTYPE=PARENT:p") < added.indexOf("BEGIN:VALARM"))
    }

    @Test
    fun `the due reminder is replaced or removed, other alarms stay`() {
        val text = "BEGIN:VCALENDAR\r\nBEGIN:VTODO\r\nUID:u\r\n" +
            "BEGIN:VALARM\r\nTRIGGER:-PT5M\r\nEND:VALARM\r\n" +
            "BEGIN:VALARM\r\nTRIGGER;RELATED=END:-P1D\r\nEND:VALARM\r\n" +
            "BEGIN:VALARM\r\nEND:VALARM\r\n" +
            "END:VTODO\r\nEND:VCALENDAR\r\n"
        assertEquals(86_400L, VtodoMapper.read(calendar(text))!!.reminderBefore)
        val replaced = edit(text) { it.copy(reminderBefore = 3_600) }
        assertEquals(
            listOf("TRIGGER:-PT5M", "TRIGGER;RELATED=END:-PT1H"),
            replaced.filter {
                it.startsWith("TRIGGER")
            }
        )
        val removed = edit(text) { it.copy(reminderBefore = null) }
        assertEquals(listOf("TRIGGER:-PT5M"), removed.filter { it.startsWith("TRIGGER") })
        val after = todo("UID:u", "BEGIN:VALARM", "TRIGGER;RELATED=END:PT5M", "END:VALARM")
        assertNull(VtodoMapper.read(calendar(after))!!.reminderBefore)
    }

    @Test
    fun `a calendar without tasks reads as none, only the first task is written`() {
        assertNull(VtodoMapper.read(IcsComponent("VCALENDAR")))
        val two = todo("UID:a", "END:VTODO", "BEGIN:VTODO", "UID:b", "RECURRENCE-ID:20261005")
        val written = edit(two) { it.copy(summary = "x") }
        assertEquals(listOf("UID:a", "SUMMARY:x"), written.subList(2, 4))
        assertTrue("RECURRENCE-ID:20261005" in written)
    }
}
