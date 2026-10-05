// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.qtekfun.ultimatetasks.notify.ReminderNotifier

/** What the system must allow for reminders to arrive on time (RF-01, RF-10). */
object ReminderPermissions {
    /**
     * Whether a reminder would show: the permission (Android 13+), the app's switch in the system
     * settings and its reminders channel. Some phones block them from the settings, where the
     * permission dialog cannot help.
     */
    fun notificationsAllowed(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        val channel = manager.getNotificationChannel(ReminderNotifier.CHANNEL)
        return notificationPermissionGranted(context) && manager.areNotificationsEnabled() &&
            channel?.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** Whether the permission itself is granted, so only the settings can turn them back on. */
    fun notificationPermissionGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Android 11+ pauses apps not opened for months and takes their permissions, alarms included.
     * Before that there is nothing to exempt.
     */
    fun unusedAppsExempt(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            context.packageManager.isAutoRevokeWhitelisted

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
    fun askBatteryExemption(context: Context) = tryOpen(
        context,
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            "package:${context.packageName}".toUri()
        )
    )

    /** Android 12: the screen where the user allows exact alarms. */
    fun askExactAlarms(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            tryOpen(
                context,
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    "package:${context.packageName}".toUri()
                )
            )
        }
    }

    /** The app's notification settings, for when the system no longer shows its dialog. */
    fun openNotificationSettings(context: Context) = tryOpen(
        context,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    )

    /** The app's page in the system settings, where makers put autostart and background options. */
    fun openAppSettings(context: Context) = tryOpen(
        context,
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${context.packageName}".toUri()
        )
    )

    /** False when the screen is missing or, as on recent ColorOS, closed to other apps. */
    fun tryOpen(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
