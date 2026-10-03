// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Shows a reminder with Complete and Snooze actions; tapping it opens the task (RF-10). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = TaskLink.from(intent) ?: return
        // Before Android 13 notifications need no permission.
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) show(context, taskId, intent)
    }

    private fun show(context: Context, taskId: Long, intent: Intent) {
        createChannel(context)
        val due = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_DUE, 0))
        val open = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            TaskLink.putInto(Intent(context, MainActivity::class.java), taskId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(intent.getStringExtra(EXTRA_TITLE))
            .setContentText(
                context.getString(
                    R.string.reminder_text,
                    intent.getStringExtra(EXTRA_LIST),
                    formatDue(due)
                )
            )
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

    private fun createChannel(context: Context) {
        val channel =
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.reminder_channel),
                NotificationManager.IMPORTANCE_HIGH
            )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun formatDue(due: Instant) = DateTimeFormatter.ofLocalizedDateTime(
        FormatStyle.SHORT
    ).format(due.atZone(ZoneId.systemDefault()))

    companion object {
        private const val CHANNEL = "reminders"
        private const val GROUP = "reminders"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_LIST = "list"
        private const val EXTRA_DUE = "due"

        /** One notification per task, whichever of its reminders showed it. */
        fun notificationId(taskId: Long): Int = taskId.hashCode()

        /** What the notification needs, carried by the alarm. */
        fun describe(intent: Intent, reminder: Reminder) {
            TaskLink.putInto(intent, reminder.taskId)
                .putExtra(EXTRA_TITLE, reminder.title)
                .putExtra(EXTRA_LIST, reminder.listName)
                .putExtra(EXTRA_DUE, reminder.due.toEpochMilli())
        }

        /** A notification right away, to check that reminders arrive (RF-10). */
        fun test(context: Context, title: String) {
            val intent = Intent(context, ReminderReceiver::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_LIST, context.getString(R.string.app_name))
                .putExtra(EXTRA_DUE, System.currentTimeMillis())
            TaskLink.putInto(intent, TEST_TASK)
            ReminderReceiver().onReceive(context, intent)
        }

        /** No task has this id; actions on it do nothing. */
        private const val TEST_TASK = 0L
    }
}
