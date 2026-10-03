// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

import android.content.Intent

/** The task a notification opens (RF-10), carried in an intent. */
object TaskLink {
    private const val EXTRA_TASK = "task_id"

    fun putInto(intent: Intent, taskId: Long): Intent = intent.putExtra(EXTRA_TASK, taskId)

    fun from(intent: Intent?): Long? = intent?.getLongExtra(EXTRA_TASK, -1)?.takeIf { it >= 0 }
}
