// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v2 (T06): encrypted credentials of each account, removed together with it. */
object Migration1To2 : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `account_credentials` (" +
                "`accountId` INTEGER NOT NULL, `loginName` TEXT NOT NULL, " +
                "`ciphertext` BLOB NOT NULL, `iv` BLOB NOT NULL, PRIMARY KEY(`accountId`), " +
                "FOREIGN KEY(`accountId`) REFERENCES `account`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
    }
}
