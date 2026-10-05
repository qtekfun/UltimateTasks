// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.reminders.PhoneMaker

/**
 * What to change in the maker's own settings, which no app can do for the user: exact steps,
 * and a button to the maker's screen or, where the system closes it to apps, the app's info page.
 */
@Composable
fun MakerAdvice(maker: PhoneMaker) {
    val context = LocalContext.current
    Text(
        stringResource(R.string.wizard_maker),
        Modifier.padding(16.dp).semantics { heading() },
        fontWeight = FontWeight.SemiBold
    )
    Text(
        stringResource(makerAdvice(maker)),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    OutlinedButton(
        onClick = { PhoneSettings.openMakerSettings(context, maker) },
        modifier = Modifier.padding(16.dp)
    ) {
        Text(stringResource(R.string.wizard_open_maker_settings))
    }
}

private fun makerAdvice(maker: PhoneMaker): Int = when (maker) {
    PhoneMaker.COLOROS -> R.string.wizard_maker_coloros
    PhoneMaker.XIAOMI -> R.string.wizard_maker_xiaomi
    PhoneMaker.HUAWEI -> R.string.wizard_maker_huawei
    PhoneMaker.SAMSUNG -> R.string.wizard_maker_samsung
    PhoneMaker.VIVO -> R.string.wizard_maker_vivo
    PhoneMaker.OTHER -> R.string.wizard_maker_other
}
