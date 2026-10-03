// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import com.qtekfun.ultimatetasks.data.local.dao.AccountDao
import com.qtekfun.ultimatetasks.data.local.dao.CredentialsDao
import com.qtekfun.ultimatetasks.data.local.dao.PendingOperationDao
import com.qtekfun.ultimatetasks.data.local.dao.PendingOperationRetryDao
import com.qtekfun.ultimatetasks.data.local.dao.SmartListDao
import com.qtekfun.ultimatetasks.data.local.dao.TaskDao
import com.qtekfun.ultimatetasks.data.local.dao.TaskListDao
import com.qtekfun.ultimatetasks.data.local.entity.AccountCredentialsEntity
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.PendingOperationEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity

/** Local source of truth. Schemas are exported to app/schemas and versioned. */
@Database(
    entities = [
        AccountEntity::class,
        AccountCredentialsEntity::class,
        TaskListEntity::class,
        TaskEntity::class,
        PendingOperationEntity::class
    ],
    version = UltimateTasksDatabase.VERSION,
    exportSchema = true
)
@ColumnTypeConverters(Converters::class)
abstract class UltimateTasksDatabase : RoomDatabase() {
    companion object {
        const val VERSION = 3

        /**
         * Migrations from each released version to the next. There is no destructive fallback:
         * raising [VERSION] requires adding its migration here (checked by DatabaseSchemaTest).
         */
        val MIGRATIONS: Array<Migration> = arrayOf(Migration1To2, Migration2To3)
    }

    abstract fun accountDao(): AccountDao

    abstract fun credentialsDao(): CredentialsDao

    abstract fun taskListDao(): TaskListDao

    abstract fun taskDao(): TaskDao

    abstract fun pendingOperationDao(): PendingOperationDao

    abstract fun pendingOperationRetryDao(): PendingOperationRetryDao

    abstract fun smartListDao(): SmartListDao
}
