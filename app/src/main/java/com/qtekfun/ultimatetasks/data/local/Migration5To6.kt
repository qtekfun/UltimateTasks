// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v6: attachments dropped (Nextcloud Tasks does not show them). Waiting uploads go; the
 * `attachments` column stays, read from the server, so no table has to be rebuilt.
 */
// Database version numbers are the migration itself, not magic numbers.
@Suppress("MagicNumber")
object Migration5To6 : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DELETE FROM `pending_operation` WHERE `type` = 'UPLOAD'")
        connection.execSQL("DROP TABLE IF EXISTS `pending_upload`")
    }
}
