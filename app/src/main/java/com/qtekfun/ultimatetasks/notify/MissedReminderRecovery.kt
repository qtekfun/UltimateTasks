// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

import android.content.SharedPreferences
import androidx.core.content.edit
import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.ShownReminderEntity
import com.qtekfun.ultimatetasks.data.settings.SettingsRepository
import com.qtekfun.ultimatetasks.domain.reminders.MissedReminders
import com.qtekfun.ultimatetasks.domain.reminders.ReminderPlanner
import com.qtekfun.ultimatetasks.domain.reminders.ShownReminder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Brings back reminders the system kept from showing (T32): some phones stop the app and drop
 * its alarms. Runs when the app starts, after each sync and whenever a reminder shows.
 */
@Singleton
class MissedReminderRecovery @Inject constructor(
    private val session: AccountSession,
    database: UltimateTasksDatabase,
    private val settings: SettingsRepository,
    private val notifier: ReminderNotifier,
    @Named(SettingsRepository.SETTINGS_PREFERENCES) private val preferences: SharedPreferences,
    private val clock: Clock
) {
    private val dao = database.shownReminderDao()

    // The alarm, the sync and the start can all run it at once: one at a time shows each once.
    private val running = Mutex()

    /** Records that a reminder showed at its time, so it is never brought back. */
    suspend fun markShown(reminderId: Long, at: Instant) {
        dao.markShown(listOf(ShownReminderEntity(reminderId, at, clock.instant())))
    }

    suspend fun recover() = running.withLock {
        val account = session.activeAccount.first() ?: return@withLock
        val current = settings.settings.first()
        val now = clock.instant()
        val window = Duration.ofHours(current.missedWindowHours.toLong())
        val planned = ReminderPlanner.planAll(
            dao.dueTasks(account.id),
            dao.snoozes().associate { it.taskId to it.until },
            current.allDayHour,
            ZoneId.systemDefault()
        )
        val shown = dao.shown().map { ShownReminder(it.reminderId, it.at) }.toSet()
        val missed = MissedReminders.pick(planned, shown, now, window)
        // Before this version nothing was recorded: what is past already showed, or was missed
        // long enough ago. Bringing it all back on the first run would be a flood.
        val firstRun = !preferences.getBoolean(KEY_BASELINE, false)
        if (!firstRun) missed.forEach { notifier.show(it, missed = true) }
        dao.markShown(missed.map { ShownReminderEntity(it.id, it.at, now) })
        if (firstRun) preferences.edit { putBoolean(KEY_BASELINE, true) }
        // Kept for the longest window offered, so widening it later repeats nothing.
        dao.forgetBefore(MissedReminders.keepAfter(now, maxOf(window, LONGEST_WINDOW)))
    }

    private companion object {
        const val KEY_BASELINE = "missed_reminders_baseline"
        val LONGEST_WINDOW: Duration = Duration.ofHours(48)
    }
}
