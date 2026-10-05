// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.reminders.Reminder
import com.qtekfun.ultimatetasks.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shows a reminder with Complete and Snooze actions; tapping it opens the task (RF-10). A
 * reminder brought back after the system kept it from showing says when it was for (T32).
 */
@Singleton
class ReminderNotifier @Inject constructor(@ApplicationContext private val context: Context) {
    fun show(reminder: Reminder, missed: Boolean) {
        // Before Android 13 notifications need no permission.
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        createChannel()
        val taskId = reminder.taskId
        val open = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            TaskLink.putInto(Intent(context, MainActivity::class.java), taskId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (missed) {
            context.getString(
                R.string.reminder_missed_text,
                reminder.listName,
                formatTime(reminder.at)
            )
        } else {
            context.getString(R.string.reminder_text, reminder.listName, formatDue(reminder.due))
        }
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminder.title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(GROUP)
        ReminderAction.entries.forEach { action ->
            builder.addAction(
                0,
                context.getString(action.label),
                ReminderActionReceiver.intent(context, taskId, action)
            )
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId(taskId), builder.build())
        } catch (_: SecurityException) {
            // The permission was revoked between the check and now: nothing to show.
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.reminder_channel),
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun formatDue(due: Instant) = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .format(due.atZone(ZoneId.systemDefault()))

    private fun formatTime(at: Instant) = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .format(at.atZone(ZoneId.systemDefault()))

    companion object {
        const val CHANNEL = "reminders"
        private const val GROUP = "reminders"

        /** One notification per task, whichever of its reminders showed it. */
        fun notificationId(taskId: Long): Int = taskId.hashCode()
    }
}
