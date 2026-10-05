// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

import javax.inject.Inject
import javax.inject.Singleton

/**
 * One beat (T33): brings back reminders that did not show and sets every alarm again, in case
 * the phone dropped them while the app was frozen. Nothing is shown and nothing goes online.
 */
@Singleton
class ReminderHeartbeat @Inject constructor(
    private val coordinator: ReminderCoordinator,
    private val recovery: MissedReminderRecovery
) : ReminderBeat {
    override suspend fun beat() {
        recovery.recover()
        coordinator.replan()
    }
}
