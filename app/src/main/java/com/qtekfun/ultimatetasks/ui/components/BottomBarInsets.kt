// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Keeps a custom `Scaffold` bottom bar clear of the system UI: the navigation bar, the taskbar
 * or dock of foldables and tablets, and the display cutout. A `Scaffold` does not pad its
 * `bottomBar` slot by itself, so without this the bar is drawn behind them.
 */
@Composable
fun Modifier.bottomBarInsets(): Modifier = windowInsetsPadding(
    WindowInsets.systemBars.union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
)
