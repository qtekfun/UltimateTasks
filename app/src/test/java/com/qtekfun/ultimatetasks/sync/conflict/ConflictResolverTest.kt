// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.conflict

import com.qtekfun.ultimatetasks.data.ical.IcsDate
import com.qtekfun.ultimatetasks.data.ical.VtodoFields
import com.qtekfun.ultimatetasks.sync.conflict.TaskField.COMPLETION
import com.qtekfun.ultimatetasks.sync.conflict.TaskField.DUE
import com.qtekfun.ultimatetasks.sync.conflict.TaskField.NOTES
import com.qtekfun.ultimatetasks.sync.conflict.TaskField.PRIORITY
import com.qtekfun.ultimatetasks.sync.conflict.TaskField.SUMMARY
import com.qtekfun.ultimatetasks.sync.conflict.TaskField.TAGS
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ConflictResolverTest {
    private val base =
        VtodoFields(uid = "u", summary = "Comprar pan", notes = "Integral", priority = 5)
    private val earlier = Instant.parse("2026-10-03T10:00:00Z")
    private val later = Instant.parse("2026-10-03T11:00:00Z")

    private fun resolve(
        local: VtodoFields,
        dirty: Set<TaskField>,
        server: VtodoFields?,
        localAt: Instant? = later,
        serverAt: Instant? = earlier,
        from: VtodoFields? = base
    ) = ConflictResolver.resolve(
        from,
        LocalVersion(local, dirty, localAt),
        ServerVersion(server, serverAt)
    )

    @Nested
    inner class Text {
        @Test
        fun `both sides changing the title asks the user and keeps the local text meanwhile`() {
            val local = base.copy(summary = "Comprar pan y leche")
            val server = base.copy(summary = "Comprar pan integral", priority = 1)
            assertEquals(
                Resolution.Merge(
                    server.copy(summary = "Comprar pan y leche"),
                    push = emptySet(),
                    conflicts = listOf(
                        TextConflict(SUMMARY, "Comprar pan y leche", "Comprar pan integral")
                    )
                ),
                resolve(local, setOf(SUMMARY), server)
            )
        }

        @Test
        fun `notes too, even when the server change is newer`() {
            val local = base.copy(notes = "Mía")
            val server = base.copy(notes = "Suya")
            val result = resolve(
                local,
                setOf(NOTES),
                server,
                localAt = earlier,
                serverAt = later
            ) as Resolution.Merge
            assertEquals(listOf(TextConflict(NOTES, "Mía", "Suya")), result.conflicts)
            assertEquals("Mía", result.fields.notes)
        }

        @Test
        fun `the same edit on both sides is no conflict`() {
            val both = base.copy(summary = "Igual")
            assertEquals(
                Resolution.Merge(both, emptySet(), emptyList()),
                resolve(both, setOf(SUMMARY), both)
            )
        }

        @Test
        fun `a local edit the server did not touch is pushed`() {
            val local = base.copy(summary = "Nuevo")
            val server = base.copy(priority = 1)
            assertEquals(
                Resolution.Merge(server.copy(summary = "Nuevo"), setOf(SUMMARY), emptyList()),
                resolve(local, setOf(SUMMARY), server, localAt = earlier, serverAt = later)
            )
        }
    }

    @Nested
    inner class OtherFields {
        private val local = base.copy(priority = 1, due = IcsDate("2026-10-05"))
        private val server = base.copy(
            priority = 9,
            due = IcsDate("2026-10-06"),
            tags = listOf("casa")
        )

        @Test
        fun `the newer change wins field by field`() {
            assertEquals(
                Resolution.Merge(
                    server.copy(priority = 1, due = IcsDate("2026-10-05")),
                    setOf(PRIORITY, DUE),
                    emptyList()
                ),
                resolve(local, setOf(PRIORITY, DUE), server, localAt = later, serverAt = earlier)
            )
            assertEquals(
                Resolution.Merge(server, emptySet(), emptyList()),
                resolve(local, setOf(PRIORITY, DUE), server, localAt = earlier, serverAt = later)
            )
        }

        @Test
        fun `ties and unknown server times keep the local change, unknown local times do not`() {
            val keep = Resolution.Merge(server.copy(priority = 1), setOf(PRIORITY), emptyList())
            assertEquals(
                keep,
                resolve(local, setOf(PRIORITY), server, localAt = later, serverAt = later)
            )
            assertEquals(
                keep,
                resolve(local, setOf(PRIORITY), server, localAt = null, serverAt = null)
            )
            assertEquals(
                Resolution.Merge(server, emptySet(), emptyList()),
                resolve(local, setOf(PRIORITY), server, localAt = null, serverAt = later)
            )
        }

        @Test
        fun `completing here survives a newer date change on the server`() {
            val done = base.copy(completed = true, completedAt = earlier)
            val moved = base.copy(due = IcsDate("2026-10-09"))
            assertEquals(
                Resolution.Merge(
                    moved.copy(completed = true, completedAt = earlier),
                    setOf(COMPLETION),
                    emptyList()
                ),
                resolve(done, setOf(COMPLETION), moved, localAt = earlier, serverAt = later)
            )
        }

        @Test
        fun `fields not changed here always take the server value`() {
            assertEquals(
                Resolution.Merge(server, emptySet(), emptyList()),
                resolve(base.copy(tags = listOf("viejo")), emptySet(), server)
            )
        }
    }

    @Nested
    inner class Deleted {
        @Test
        fun `deleted on the server and untouched here is deleted here`() {
            assertEquals(Resolution.DeleteLocally, resolve(base, emptySet(), null))
        }

        @Test
        fun `deleted on the server but changed here asks what to do`() {
            val local = base.copy(summary = "Cambiada")
            assertEquals(Resolution.DeletedOnServer(local), resolve(local, setOf(SUMMARY), null))
        }
    }

    @Test
    fun `without a common base every difference counts as changed on the server`() {
        val local = base.copy(summary = "Local", priority = 1)
        val server = base.copy(summary = "Servidor", priority = 9)
        val result = resolve(
            local,
            setOf(SUMMARY, PRIORITY),
            server,
            from = null
        ) as Resolution.Merge
        assertEquals(listOf(TextConflict(SUMMARY, "Local", "Servidor")), result.conflicts)
        assertEquals(setOf(PRIORITY), result.push)
    }

    @Test
    fun `every field reads and writes its own value`() {
        val full = VtodoFields(
            uid = "u", summary = "s", notes = "n", url = "https://x", due = IcsDate("2026-10-05"),
            start = IcsDate("2026-10-04"), completed = true, completedAt = earlier, priority = 1,
            tags = listOf(
                "t"
            ),
            parentUid = "p", sortOrder = 3, recurrence = "FREQ=DAILY", reminderBefore = 60
        )
        val empty = VtodoFields(uid = "u")
        TaskField.entries.forEach { field ->
            val written = field.write(empty, full)
            assertEquals(field.read(full), field.read(written), field.name)
            TaskField.entries.filter { it != field }.forEach { other ->
                assertEquals(
                    other.read(empty),
                    other.read(written),
                    "${field.name} touched ${other.name}"
                )
            }
        }
    }

    @Test
    fun `dirty bits round-trip`() {
        assertEquals(
            setOf(SUMMARY, TAGS),
            TaskField.fromBits(TaskField.toBits(setOf(SUMMARY, TAGS)))
        )
        assertEquals(0, TaskField.toBits(emptySet()))
        assertEquals(TaskField.entries.toSet(), TaskField.fromBits(-1))
    }
}
