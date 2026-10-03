// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.settings

import com.qtekfun.ultimatetasks.domain.task.TaskSort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ListSortPrefsTest {
    @Test
    fun `each list keeps its own order, manual by default`() {
        val preferences = FakePreferences()
        val prefs = ListSortPrefs(preferences)
        assertEquals(TaskSort.MANUAL, prefs.get("/a/"))
        prefs.set("/a/", TaskSort.DUE)
        assertEquals(TaskSort.DUE to TaskSort.MANUAL, prefs.get("/a/") to prefs.get("/b/"))
        preferences.edit().putString("sort:/c/", "GONE").apply()
        assertEquals(TaskSort.MANUAL, prefs.get("/c/"))
    }
}
