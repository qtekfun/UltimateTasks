// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.reminders

import java.time.Duration
import java.time.Instant

/** A reminder that was shown: its id and the time it was for, since ids are reused. */
data class ShownReminder(val reminderId: Long, val at: Instant)

/**
 * Reminders whose time passed without showing, because the system stopped the app and its
 * alarms (T32). Only those within [window] count: an older one would be noise, not a reminder.
 */
object MissedReminders {
    fun pick(
        planned: List<Reminder>,
        shown: Set<ShownReminder>,
        now: Instant,
        window: Duration
    ): List<Reminder> {
        val since = now.minus(window)
        return planned.filter { reminder ->
            !reminder.at.isAfter(now) && reminder.at.isAfter(since) &&
                ShownReminder(reminder.id, reminder.at) !in shown
        }.sortedBy { it.at }
    }

    /** Records older than this can go: no reminder that old is picked any more. */
    fun keepAfter(now: Instant, window: Duration): Instant = now.minus(window)
}
