// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.data.settings.AppSettings
import com.qtekfun.ultimatetasks.domain.reminders.PhoneMaker
import com.qtekfun.ultimatetasks.notify.ReminderReceiver
import com.qtekfun.ultimatetasks.ui.detail.Choice
import com.qtekfun.ultimatetasks.ui.detail.DetailCard
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Everything that makes reminders arrive (RF-10, T02b): notifications allowed, the battery
 * exemption, the aggressive alarm-clock mode, the hour of all-day tasks and a test.
 */
@Composable
fun RemindersSection(settings: AppSettings, viewModel: SettingsViewModel, onWizard: () -> Unit) {
    val context = LocalContext.current
    // Permissions change outside the app: read them again whenever it comes back.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val notifications = remember(refresh) { ReminderPermissions.notificationsAllowed(context) }
    val exempt = remember(refresh) { ReminderPermissions.batteryExempt(context) }
    val unused = remember(refresh) { ReminderPermissions.unusedAppsExempt(context) }
    val askNotifications = rememberNotificationRequest { refresh++ }
    DetailCard {
        StatusRow(stringResource(R.string.settings_notifications), notifications, askNotifications)
        HorizontalDivider(Modifier.padding(start = 16.dp))
        StatusRow(stringResource(R.string.settings_battery), exempt) {
            ReminderPermissions.askBatteryExemption(context)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HorizontalDivider(Modifier.padding(start = 16.dp))
            UnusedAppsRow(unused)
        }
        HorizontalDivider(Modifier.padding(start = 16.dp))
        SwitchSetting(
            stringResource(R.string.settings_alarm_mode),
            stringResource(R.string.settings_alarm_mode_hint),
            settings.alarmClock,
            viewModel::setAlarmClock
        )
        HorizontalDivider(Modifier.padding(start = 16.dp))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_all_day_hour), Modifier.weight(1f))
            Choice(hourLabel(settings.allDayHour), (0..LAST_HOUR).toList(), {
                hourLabel(it)
            }, viewModel::setAllDayHour)
        }
        val maker = PhoneMaker.of(Build.MANUFACTURER)
        if (maker != PhoneMaker.OTHER) {
            HorizontalDivider(Modifier.padding(start = 16.dp))
            MakerAdvice(maker)
        }
        HorizontalDivider(Modifier.padding(start = 16.dp))
        TextButton(onClick = onWizard, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(stringResource(R.string.settings_wizard))
        }
        HorizontalDivider(Modifier.padding(start = 16.dp))
        val testTitle = stringResource(R.string.settings_test_title)
        OutlinedButton(onClick = {
            ReminderReceiver.test(context, testTitle)
        }, modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.settings_test))
        }
    }
}

/** Android 11+ pauses unused apps; the switch is on the app's info page, which explains nothing. */
@Composable
private fun UnusedAppsRow(exempt: Boolean) {
    val context = LocalContext.current
    StatusRow(stringResource(R.string.settings_unused_apps), exempt) {
        PhoneSettings.askUnusedAppsExemption(context)
    }
    if (!exempt) {
        Text(
            stringResource(R.string.wizard_unused_why),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

@Composable
internal fun StatusRow(label: String, ok: Boolean, onFix: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f))
        if (ok) {
            Text(stringResource(R.string.settings_ok), color = MaterialTheme.colorScheme.primary)
        } else {
            OutlinedButton(onClick = onFix) { Text(stringResource(R.string.settings_allow)) }
        }
    }
}

@Composable
fun SwitchSetting(label: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label)
            hint?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private const val LAST_HOUR = 23

private fun hourLabel(hour: Int): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(LocalTime.of(hour, 0))
