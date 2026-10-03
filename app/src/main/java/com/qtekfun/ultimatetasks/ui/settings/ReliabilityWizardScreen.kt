// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.reminders.PhoneMaker
import com.qtekfun.ultimatetasks.notify.ReminderReceiver
import com.qtekfun.ultimatetasks.ui.detail.DetailCard

/**
 * Makes reminders arrive on phones that stop apps in the background (RF-01): each step says why
 * it is asked and shows whether it is done. Shown once after signing in; Settings repeats it.
 */
@Composable
fun ReliabilityWizardScreen(onDone: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val maker = PhoneMaker.of(Build.MANUFACTURER)
    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) { padding ->
        Column(
            Modifier.padding(
                padding
            ).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.wizard_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(R.string.wizard_intro),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            WizardSteps(maker)
            DetailCard {
                val hint = if (maker == PhoneMaker.OTHER) {
                    R.string.settings_alarm_mode_hint
                } else {
                    R.string.wizard_alarm_recommended
                }
                SwitchSetting(
                    stringResource(R.string.settings_alarm_mode),
                    stringResource(hint),
                    settings.alarmClock,
                    viewModel::setAlarmClock
                )
            }
            val testTitle = stringResource(R.string.settings_test_title)
            OutlinedButton(onClick = {
                ReminderReceiver.test(context, testTitle)
            }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_test))
            }
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.wizard_done))
            }
        }
    }
}

/** One card per thing to allow, each showing whether it is done. */
@Composable
private fun WizardSteps(maker: PhoneMaker) {
    val context = LocalContext.current
    // Permissions change in other screens: read them again when coming back.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val notifications = remember(refresh) { ReminderPermissions.notificationsAllowed(context) }
    val exact = remember(refresh) { ReminderPermissions.exactAlarmsAllowed(context) }
    val exempt = remember(refresh) { ReminderPermissions.batteryExempt(context) }
    val askNotifications = rememberNotificationRequest { refresh++ }
    // As in UltimateDeck: the system dialog comes up on its own the first time.
    var asked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!asked && !notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            asked = true
            askNotifications()
        }
    }
    WizardStep(
        R.string.settings_notifications,
        R.string.wizard_notifications_why,
        notifications,
        askNotifications
    )
    WizardStep(R.string.wizard_exact, R.string.wizard_exact_why, exact) {
        ReminderPermissions.askExactAlarms(context)
    }
    WizardStep(R.string.settings_battery, R.string.wizard_battery_why, exempt) {
        ReminderPermissions.askBatteryExemption(context)
    }
    MakerStep(maker) { ReminderPermissions.openAppSettings(context) }
}

@Composable
private fun WizardStep(title: Int, why: Int, done: Boolean, onFix: () -> Unit) {
    DetailCard {
        StatusRow(stringResource(title), done, onFix)
        Text(
            stringResource(why),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

/** What to change in the maker's own settings, which no app can do for the user. */
@Composable
private fun MakerStep(maker: PhoneMaker, onOpen: () -> Unit) {
    DetailCard {
        Text(
            stringResource(R.string.wizard_maker),
            Modifier.padding(16.dp),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(makerAdvice(maker)),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        OutlinedButton(onClick = onOpen, modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.wizard_open_app_settings))
        }
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
