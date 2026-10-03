// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v5 (T26): attachments of tasks, and files waiting to be uploaded. */
// Database version numbers are the migration itself, not magic numbers.
@Suppress("MagicNumber")
object Migration4To5 : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `task` ADD COLUMN `attachments` TEXT NOT NULL DEFAULT '[]'")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_upload` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER NOT NULL, " +
                "`uri` TEXT NOT NULL, `name` TEXT NOT NULL, `mimeType` TEXT, `error` TEXT, " +
                "FOREIGN KEY(`taskId`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pending_upload_taskId` ON `pending_upload` (`taskId`)"
        )
    }
}
