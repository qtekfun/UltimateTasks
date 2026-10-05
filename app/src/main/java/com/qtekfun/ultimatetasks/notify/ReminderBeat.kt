// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

import javax.inject.Inject

/**
 * The periodic work that keeps reminders deliverable while the robust mode's service runs
 * (RF-10). It never touches the network.
 */
fun interface ReminderBeat {
    fun beat()
}

/** Plans and schedules every reminder again, in case the system dropped some alarms. */
class RescheduleBeat @Inject constructor(private val coordinator: ReminderCoordinator) :
    ReminderBeat {
    override fun beat() = coordinator.refresh()
}
