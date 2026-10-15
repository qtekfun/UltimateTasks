// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.focus.FocusState

/**
 * As in Apple Reminders, what is typed is kept without pressing Enter: [commit] runs when the
 * field loses focus (a tap elsewhere) and when it leaves the screen with text in it (back, or
 * opening a task). Not when the screen only turns: the text survives that by itself.
 * [text] is read when needed, so a commit that clears it is not repeated on leaving.
 * Returns what to pass to `Modifier.onFocusChanged`.
 */
@Composable
fun rememberCommitOnLeave(text: () -> String, commit: () -> Unit): (FocusState) -> Unit {
    val currentText by rememberUpdatedState(text)
    val currentCommit by rememberUpdatedState(commit)
    val activity = LocalActivity.current
    // The first report is "not focused", before the field ever had focus: not a leave.
    val focused = remember { BooleanArray(1) }
    DisposableEffect(Unit) {
        onDispose {
            if (currentText().isNotBlank() && activity?.isChangingConfigurations != true) {
                currentCommit()
            }
        }
    }
    return { state ->
        if (state.isFocused) {
            focused[0] = true
        } else if (focused[0]) {
            focused[0] = false
            currentCommit()
        }
    }
}
