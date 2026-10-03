// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.model

/** The numbers on the four buttons of the home screen (RF-02). */
data class SmartCounts(val today: Int, val scheduled: Int, val all: Int, val completed: Int)

/** Open tasks of one list, for "My lists". */
data class ListCount(val listHref: String, val open: Int)
