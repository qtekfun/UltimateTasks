// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

/**
 * The periodic work that keeps reminders deliverable (RF-10): the heartbeat's alarm (T33) and
 * the robust mode's service (T34) both run it. It never touches the network.
 */
fun interface ReminderBeat {
    suspend fun beat()
}
