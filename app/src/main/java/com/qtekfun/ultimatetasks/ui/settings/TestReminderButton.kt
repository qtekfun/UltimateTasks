// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.reminders.TestDelivery
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

/**
 * A real reminder in a minute, set like any other (T33), and how it went: on time, late by how
 * much, or not arrived. While waiting it checks every few seconds.
 */
@Composable
fun TestReminderButton(
    modifier: Modifier = Modifier,
    viewModel: TestReminderViewModel = viewModel()
) {
    val delivery by viewModel.delivery.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.check() }
    LaunchedEffect(delivery) {
        if (delivery is TestDelivery.Waiting) {
            delay(CHECK_EVERY_MS)
            viewModel.check()
        }
    }
    val title = stringResource(R.string.settings_test_title)
    Column(modifier) {
        OutlinedButton(onClick = { viewModel.send(title) }) {
            Text(stringResource(R.string.settings_test))
        }
        delivery?.let {
            Text(
                deliveryText(it),
                style = MaterialTheme.typography.bodySmall,
                color = if (it is TestDelivery.Late || it is TestDelivery.Missing) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(top = 8.dp).semantics {
                    liveRegion =
                        LiveRegionMode.Polite
                }
            )
        }
    }
}

@Composable
private fun deliveryText(delivery: TestDelivery): String = when (delivery) {
    is TestDelivery.Waiting -> stringResource(R.string.test_waiting, time(delivery.scheduledAt))

    is TestDelivery.OnTime -> stringResource(R.string.test_on_time, time(delivery.arrivedAt))

    is TestDelivery.Late -> pluralStringResource(
        R.plurals.test_late,
        delivery.minutes.toInt(),
        time(delivery.arrivedAt),
        delivery.minutes.toInt()
    )

    is TestDelivery.Missing -> stringResource(R.string.test_missing, time(delivery.scheduledAt))
}

private fun time(at: Instant): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(at.atZone(ZoneId.systemDefault()))

private const val CHECK_EVERY_MS = 5_000L
