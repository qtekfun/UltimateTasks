// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.domain.reminders.Reminder
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * An alarm of a reminder (RF-10): shows it, records that it showed and, since the app is
 * running now, brings back any the system kept from showing (T32).
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject
    lateinit var notifier: ReminderNotifier

    @Inject
    lateinit var recovery: MissedReminderRecovery

    override fun onReceive(context: Context, intent: Intent) {
        val reminder = read(intent) ?: return
        notifier.show(reminder, missed = false)
        val pending = goAsync()
        scope.launch {
            try {
                recovery.markShown(reminder.id, reminder.at)
                recovery.recover()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private const val EXTRA_ID = "id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_LIST = "list"
        private const val EXTRA_DUE = "due"
        private const val EXTRA_AT = "at"
        private const val EXTRA_EARLY = "early"

        /** No task has this id; actions on it do nothing. */
        private const val TEST_TASK = 0L

        /** What the notification needs, carried by the alarm. */
        fun describe(intent: Intent, reminder: Reminder) {
            TaskLink.putInto(intent, reminder.taskId)
                .putExtra(EXTRA_ID, reminder.id)
                .putExtra(EXTRA_TITLE, reminder.title)
                .putExtra(EXTRA_LIST, reminder.listName)
                .putExtra(EXTRA_DUE, reminder.due.toEpochMilli())
                .putExtra(EXTRA_AT, reminder.at.toEpochMilli())
                .putExtra(EXTRA_EARLY, reminder.early)
        }

        private fun read(intent: Intent): Reminder? {
            // Alarms set by an older version carry no id; the app sets them again when it starts.
            val taskId = TaskLink.from(intent)?.takeIf { intent.hasExtra(EXTRA_ID) } ?: return null
            return Reminder(
                id = intent.getLongExtra(EXTRA_ID, 0),
                taskId = taskId,
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                listName = intent.getStringExtra(EXTRA_LIST).orEmpty(),
                due = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_DUE, 0)),
                at = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_AT, 0)),
                early = intent.getBooleanExtra(EXTRA_EARLY, false)
            )
        }

        /** A notification right away, to check that reminders arrive (RF-10). */
        fun test(context: Context, title: String) {
            val now = Instant.now()
            ReminderNotifier(context.applicationContext).show(
                Reminder(
                    -1,
                    TEST_TASK,
                    title,
                    context.getString(R.string.app_name),
                    now,
                    now,
                    false
                ),
                missed = false
            )
        }
    }
}
