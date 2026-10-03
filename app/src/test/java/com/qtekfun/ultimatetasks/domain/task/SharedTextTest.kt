// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SharedTextTest {
    @Test
    fun `the subject is the title and the link goes to the URL`() {
        assertEquals(
            SharedTask("Receta de pan", "Mira esto", "https://example.com/pan"),
            SharedText.parse("Receta de pan", "Mira esto\nhttps://example.com/pan")
        )
    }

    @Test
    fun `without subject the first line is the title`() {
        assertEquals(
            SharedTask("Comprar leche", "y huevos", null),
            SharedText.parse(null, "  Comprar leche \n\n y huevos ")
        )
    }

    @Test
    fun `a bare link is title and URL, trailing punctuation dropped`() {
        assertEquals(
            SharedTask("https://example.com/a", "", "https://example.com/a"),
            SharedText.parse(" ", "https://example.com/a.")
        )
    }

    @Test
    fun `nothing shared gives an empty title, long titles are cut`() {
        assertEquals(SharedTask("", "", null), SharedText.parse(null, null))
        assertEquals(200, SharedText.parse("x".repeat(500), null).title.length)
    }
}
