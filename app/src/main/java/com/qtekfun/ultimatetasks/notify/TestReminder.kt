// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.data.settings.SettingsRepository
import com.qtekfun.ultimatetasks.domain.reminders.Reminder
import com.qtekfun.ultimatetasks.domain.reminders.TestDelivery
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * The test reminder (T33): an alarm a minute ahead, set exactly as real reminders are, so it
 * shows whether this phone delivers them on time. When it arrives is kept to tell the user.
 */
@Singleton
class TestReminder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduler: ReminderScheduler,
    private val settings: SettingsRepository,
    @Named(SettingsRepository.SETTINGS_PREFERENCES) private val preferences: SharedPreferences,
    private val clock: Clock
) {
    suspend fun send(title: String) {
        val at = clock.instant().plus(DELAY)
        preferences.edit {
            putLong(KEY_SCHEDULED, at.toEpochMilli())
            remove(KEY_ARRIVED)
        }
        val reminder =
            Reminder(ID, TASK, title, context.getString(R.string.app_name), at, at, false)
        scheduler.scheduleOne(reminder, settings.settings.first().alarmClock)
    }

    fun arrived() = preferences.edit { putLong(KEY_ARRIVED, clock.instant().toEpochMilli()) }

    fun delivery(): TestDelivery? =
        TestDelivery.of(instant(KEY_SCHEDULED), instant(KEY_ARRIVED), clock.instant())

    private fun instant(key: String): Instant? =
        preferences.getLong(key, 0).takeIf { it > 0 }?.let(Instant::ofEpochMilli)

    companion object {
        /** No planned reminder has a negative id. */
        const val ID = -1L

        /** No task has this id; actions on it do nothing. */
        const val TASK = 0L

        private val DELAY: Duration = Duration.ofMinutes(1)
        private const val KEY_SCHEDULED = "test_reminder_scheduled"
        private const val KEY_ARRIVED = "test_reminder_arrived"
    }
}
