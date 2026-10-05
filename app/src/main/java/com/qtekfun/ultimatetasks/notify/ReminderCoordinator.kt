// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.settings.SettingsRepository
import com.qtekfun.ultimatetasks.domain.reminders.Reminder
import com.qtekfun.ultimatetasks.domain.reminders.ReminderPlanner
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Keeps the alarms in step with the tasks, snoozes and settings (RF-10): any change (sync,
 * edit, done, list hidden, snooze, setting) plans and schedules them again. [refresh] does it
 * after a restart or a change of the clock or time zone.
 */
@Singleton
class ReminderCoordinator @Inject constructor(
    private val session: AccountSession,
    database: UltimateTasksDatabase,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val recovery: MissedReminderRecovery,
    private val clock: Clock
) {
    private val dao = database.reminderDao()
    private val ticks = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        // Whatever the system kept from showing while the app was stopped (T32).
        scope.launch { recovery.recover() }
        scope.launch {
            combine(session.activeAccount, settings.settings, ticks) { account, settings, _ ->
                account to
                    settings
            }
                .flatMapLatest { (account, settings) ->
                    if (account == null) {
                        flowOf(emptyList<Reminder>() to settings.alarmClock)
                    } else {
                        combine(dao.observeDueTasks(account.id), dao.observeSnoozes()) {
                                tasks,
                                snoozes
                            ->
                            ReminderPlanner.plan(
                                tasks,
                                snoozes.associate { it.taskId to it.until },
                                settings.allDayHour,
                                clock.instant(),
                                ZoneId.systemDefault()
                            ) to settings.alarmClock
                        }
                    }
                }
                .collect { (reminders, alarmClock) -> scheduler.schedule(reminders, alarmClock) }
        }
    }

    fun refresh() {
        ticks.value++
    }
}
