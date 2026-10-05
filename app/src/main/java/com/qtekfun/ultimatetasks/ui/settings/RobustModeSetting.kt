// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.settings

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.reminders.PhoneMaker

/**
 * The robust mode switch (RF-10): a fixed notification keeps the app alive. Recommended on the
 * makers that kill apps in the background.
 */
@Composable
fun RobustModeSetting(on: Boolean, onChange: (Boolean) -> Unit) {
    val hint = stringResource(R.string.robust_mode_hint)
    val recommended = PhoneMaker.of(Build.MANUFACTURER) != PhoneMaker.OTHER
    SwitchSetting(
        stringResource(R.string.robust_mode),
        if (recommended) hint + "\n" + stringResource(R.string.robust_mode_recommended) else hint,
        on,
        onChange
    )
}
