// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.entity

import androidx.room3.Entity
import java.time.Instant

/**
 * A reminder that was shown (T32), by its id and the time it was for: ids are reused when a
 * task's date changes. Kept only as long as missed reminders are looked for.
 */
@Entity(tableName = "shown_reminder", primaryKeys = ["reminderId", "at"])
data class ShownReminderEntity(val reminderId: Long, val at: Instant, val shownAt: Instant)
