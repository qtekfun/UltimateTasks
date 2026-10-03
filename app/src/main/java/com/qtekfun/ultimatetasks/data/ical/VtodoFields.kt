// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.ical

import java.time.Instant

/** What the app reads from and writes to a `VTODO` (SPEC RF-05). Everything else is kept as is. */
data class VtodoFields(
    val uid: String,
    val summary: String = "",
    val notes: String = "",
    val url: String? = null,
    val due: IcsDate? = null,
    val start: IcsDate? = null,
    val completed: Boolean = false,
    val completedAt: Instant? = null,
    /** 0 none, 1–9 as in `PRIORITY` (1 highest). */
    val priority: Int = 0,
    val tags: List<String> = emptyList(),
    val parentUid: String? = null,
    val sortOrder: Long? = null,
    /** The `RRULE` value. */
    val recurrence: String? = null,
    /** The first alarm relative to the due date, in seconds before it. */
    val reminderBefore: Long? = null,
    val modifiedAt: Instant? = null
)
