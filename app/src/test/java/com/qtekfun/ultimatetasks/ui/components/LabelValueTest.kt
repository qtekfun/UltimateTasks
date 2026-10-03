// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LabelValueTest {
    @Test
    fun `both keep their width when they fit`() {
        assertEquals(100 to 150, splitWidths(300, 10, 100, 150))
    }

    @Test
    fun `a short label keeps its width and a long value wraps in the rest`() {
        assertEquals(80 to 210, splitWidths(300, 10, 80, 900))
    }

    @Test
    fun `a short value keeps its width and a long label wraps in the rest`() {
        assertEquals(230 to 60, splitWidths(300, 10, 400, 60))
    }

    @Test
    fun `two long texts share the room`() {
        assertEquals(145 to 146, splitWidths(301, 10, 400, 500))
    }

    @Test
    fun `no room gives nothing instead of negative widths`() {
        assertEquals(0 to 0, splitWidths(5, 10, 40, 50))
    }
}
