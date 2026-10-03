// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v3 (T09): conflicts waiting for the user, kept on the task until resolved. */
// Database version numbers are the migration itself, not magic numbers.
@Suppress("MagicNumber")
object Migration2To3 : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `task` ADD COLUMN `conflictSummary` TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE `task` ADD COLUMN `conflictNotes` TEXT DEFAULT NULL")
        connection.execSQL(
            "ALTER TABLE `task` ADD COLUMN `deletedOnServer` INTEGER NOT NULL DEFAULT 0"
        )
    }
}
