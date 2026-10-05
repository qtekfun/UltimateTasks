// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v7 (T32): reminders already shown, to bring back those the system kept from showing. */
// Database version numbers are the migration itself, not magic numbers.
@Suppress("MagicNumber")
object Migration6To7 : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `shown_reminder` (" +
                "`reminderId` INTEGER NOT NULL, `at` INTEGER NOT NULL, " +
                "`shownAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`reminderId`, `at`))"
        )
    }
}
