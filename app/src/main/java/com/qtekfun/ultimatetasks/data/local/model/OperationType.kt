// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local.model

/** What a queued operation does to its task. The queue logic itself comes with T07. */
enum class OperationType {
    CREATE,
    UPDATE,
    MOVE,
    DELETE,
    UPLOAD
}
