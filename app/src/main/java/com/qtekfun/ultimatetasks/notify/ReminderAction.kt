// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

import com.qtekfun.ultimatetasks.R
import java.time.Duration

/** The buttons of a reminder (RF-10): done, or ring again later (only on this device). */
enum class ReminderAction(val label: Int, val snooze: Duration?) {
    COMPLETE(R.string.reminder_complete, null),
    SNOOZE_15(R.string.reminder_snooze_15, Duration.ofMinutes(SHORT_SNOOZE_MINUTES)),
    SNOOZE_HOUR(R.string.reminder_snooze_hour, Duration.ofHours(1)),
    TOMORROW(R.string.reminder_tomorrow, Duration.ofDays(1))
}

private const val SHORT_SNOOZE_MINUTES = 15L
