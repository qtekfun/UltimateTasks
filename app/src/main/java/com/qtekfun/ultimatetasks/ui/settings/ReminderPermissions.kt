// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/** What the system must allow for reminders to arrive on time (RF-01, RF-10). */
object ReminderPermissions {
    fun notificationsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Granted at install on Android 13+ (USE_EXACT_ALARM); on 12 the user allows it. */
    fun exactAlarmsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun batteryExempt(context: Context): Boolean = context.getSystemService(
        PowerManager::class.java
    ).isIgnoringBatteryOptimizations(context.packageName)

    /**
     * The system dialog that exempts the app from battery optimisation. Play restricts it to some
     * apps; F-Droid does not, and reminders are exactly the case it exists for (T02b, PRIVACY.md).
     */
    @SuppressLint("BatteryLife")
    fun askBatteryExemption(context: Context) = open(
        context,
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            "package:${context.packageName}".toUri()
        )
    )

    /** Android 12: the screen where the user allows exact alarms. */
    fun askExactAlarms(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            open(
                context,
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    "package:${context.packageName}".toUri()
                )
            )
        }
    }

    /** The app's notification settings, for when the system no longer shows its dialog. */
    fun openNotificationSettings(context: Context) = open(
        context,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    )

    /** The app's page in the system settings, where makers put autostart and background options. */
    fun openAppSettings(context: Context) = open(
        context,
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${context.packageName}".toUri()
        )
    )

    private fun open(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Some makers remove these screens: nothing else to open.
        }
    }
}
