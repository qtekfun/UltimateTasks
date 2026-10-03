// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PanesTest {
    @Test
    fun `phones get one pane, tablets two upright and three lying down`() {
        assertEquals(
            listOf(Panes.ONE, Panes.ONE, Panes.TWO, Panes.TWO, Panes.THREE),
            listOf(360, 599, 600, 1199, 1280).map(Panes::forWidth)
        )
    }
}
