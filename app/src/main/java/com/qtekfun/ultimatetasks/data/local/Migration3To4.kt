// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v4 (T21): reminders snoozed from their notification. */
// Database version numbers are the migration itself, not magic numbers.
@Suppress("MagicNumber")
object Migration3To4 : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `snooze` (" +
                "`taskId` INTEGER NOT NULL, `until` INTEGER NOT NULL, " +
                "PRIMARY KEY(`taskId`), FOREIGN KEY(`taskId`) REFERENCES `task`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
    }
}
