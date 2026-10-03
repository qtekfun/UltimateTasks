// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.ical

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IcsWriterTest {
    @Test
    fun `new components are written in canonical form`() {
        val todo = IcsComponent(
            "VTODO",
            listOf(
                IcsProperty("UID", value = "1"),
                IcsProperty("DUE", listOf(IcsParameter("VALUE", "DATE")), "20261003"),
                IcsProperty("X-A", listOf(IcsParameter("P", listOf("a;b", "c"))), "v")
            )
        )
        assertEquals(
            "BEGIN:VTODO\r\nUID:1\r\nDUE;VALUE=DATE:20261003\r\nX-A;P=\"a;b\",c:v\r\nEND:VTODO\r\n",
            IcsWriter.write(todo)
        )
    }

    @Test
    fun `long lines fold at 75 octets without splitting characters`() {
        val folded = IcsWriter.fold("SUMMARY:" + "é".repeat(60))
        val lines = folded.removeSuffix("\r\n").split("\r\n")
        assertTrue(lines.all { it.toByteArray().size <= 75 })
        assertTrue(lines.drop(1).all { it.startsWith(" ") })
        assertEquals(
            "SUMMARY:" + "é".repeat(60),
            lines.first() + lines.drop(1).joinToString("") { it.drop(1) }
        )
    }

    @Test
    fun `emoji are never split`() {
        val folded = IcsWriter.fold("X:" + "🎉".repeat(30))
        assertEquals("X:" + "🎉".repeat(30), folded.replace("\r\n ", "").removeSuffix("\r\n"))
    }

    @Test
    fun `withProperty replaces in place, removes duplicates and adds before subcomponents`() {
        val alarm = IcsComponent("VALARM")
        val todo = IcsComponent(
            "VTODO",
            listOf(
                IcsProperty("A", value = "1"),
                IcsProperty("B", value = "1"),
                IcsProperty("A", value = "2"),
                alarm
            )
        )
        val replaced = todo.withProperty("a", IcsProperty("A", value = "3"))
        assertEquals(listOf("A:3", "B:1"), replaced.properties.map { "${it.name}:${it.value}" })
        val added = todo.withProperty("C", IcsProperty("C", value = "x"))
        assertEquals(alarm, added.children.last())
        assertEquals("C", (added.children[3] as IcsProperty).name)
        assertEquals(listOf("B"), todo.withProperty("A", null).properties.map { it.name })
        val noComponents = IcsComponent("VTODO").withProperty("C", IcsProperty("C", value = "x"))
        assertEquals(1, noComponents.children.size)
    }

    @Test
    fun `withComponents replaces in place or appends`() {
        val todo =
            IcsComponent(
                "VTODO",
                listOf(
                    IcsProperty("A", value = "1"),
                    IcsComponent("VALARM"),
                    IcsProperty("B", value = "1")
                )
            )
        val two =
            listOf(
                IcsComponent("VALARM", listOf(IcsProperty("X", value = "1"))),
                IcsComponent("VALARM")
            )
        assertEquals(
            listOf("A", "VALARM", "VALARM", "B"),
            names(todo.withComponents("VALARM", two))
        )
        assertEquals(
            listOf("A", "VALARM", "B", "X-C"),
            names(todo.withComponents("X-C", listOf(IcsComponent("X-C"))))
        )
    }

    private fun names(component: IcsComponent) = component.children.map {
        when (it) {
            is IcsComponent -> it.name
            is IcsProperty -> it.name
        }
    }
}
