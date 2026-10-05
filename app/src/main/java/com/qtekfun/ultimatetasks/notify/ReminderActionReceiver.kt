// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.SnoozeEntity
import com.qtekfun.ultimatetasks.data.task.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Runs the Complete and Snooze buttons of a reminder without opening the app. */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var repository: TaskRepository

    @Inject
    lateinit var database: UltimateTasksDatabase

    @Inject
    lateinit var clock: Clock

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = TaskLink.from(intent) ?: return
        val action =
            ReminderAction.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_ACTION) }
                ?: return
        NotificationManagerCompat.from(context).cancel(ReminderNotifier.notificationId(taskId))
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snooze = action.snooze
                if (snooze == null) {
                    database.taskDao().get(taskId)?.let { repository.setCompleted(it, true) }
                } else {
                    database.reminderDao().snooze(
                        SnoozeEntity(taskId, clock.instant().plus(snooze))
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val EXTRA_ACTION = "action"

        fun intent(context: Context, taskId: Long, action: ReminderAction): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                (taskId * ReminderAction.entries.size + action.ordinal).hashCode(),
                TaskLink.putInto(Intent(context, ReminderActionReceiver::class.java), taskId)
                    .setAction("reminder-action:${action.name}:$taskId")
                    .putExtra(EXTRA_ACTION, action.name),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }
}
