// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import com.qtekfun.ultimatetasks.domain.task.SmartList
import com.qtekfun.ultimatetasks.domain.task.TaskSource

/** What is on screen, saved as plain values so it survives rotation and process death. */
class NavState(wizard: Boolean) {
    var opened by mutableStateOf<TaskSource?>(null)
    var task by mutableStateOf<Long?>(null)
    var settings by mutableStateOf(false)
    var reorder by mutableStateOf(false)
    var wizard by mutableStateOf(wizard)

    /** The list was opened with "New task": ready to type. */
    var adding by mutableStateOf(false)

    fun closeList() {
        opened = null
        adding = false
    }

    companion object {
        val Saver: Saver<NavState, Any> = listSaver(
            save = {
                listOf(
                    it.opened?.let(::encode),
                    it.task,
                    it.settings,
                    it.reorder,
                    it.wizard,
                    it.adding
                )
            },
            restore = { values ->
                NavState(values[4] as Boolean).apply {
                    opened = (values[0] as String?)?.let(::decode)
                    task = values[1] as Long?
                    settings = values[2] as Boolean
                    reorder = values[3] as Boolean
                    adding = values[5] as Boolean
                }
            }
        )

        private fun encode(source: TaskSource): String = when (source) {
            is TaskSource.List -> "list:${source.href}"
            is TaskSource.Smart -> "smart:${source.kind.name}"
        }

        private fun decode(text: String): TaskSource? = when {
            text.startsWith("list:") -> TaskSource.List(text.removePrefix("list:"))

            else -> SmartList.entries.firstOrNull {
                "smart:${it.name}" == text
            }?.let { TaskSource.Smart(it) }
        }
    }
}
