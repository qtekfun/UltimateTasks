// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.qtekfun.ultimatetasks.data.local.entity.AccountCredentialsEntity
import com.qtekfun.ultimatetasks.data.local.entity.SnoozeEntity
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Opens a database created with each old exported schema and lets Room migrate and validate it. */
class MigrationTest {
    @TempDir
    lateinit var dir: File

    private val schemas = File("schemas/${UltimateTasksDatabase::class.qualifiedName}")

    /** Creates [file] exactly as version [version] of the exported schema describes it. */
    private fun createFromSchema(file: File, version: Int, extraSql: List<String> = emptyList()) {
        val schema = Json.parseToJsonElement(
            File(schemas, "$version.json").readText()
        ).jsonObject["database"]!!.jsonObject
        val statements = schema["entities"]!!.jsonArray.flatMap { entity ->
            val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
            val create = entity.jsonObject["createSql"]!!.jsonPrimitive.content.replace(
                "\${TABLE_NAME}",
                table
            )
            val indices = entity.jsonObject["indices"]?.jsonArray.orEmpty().map {
                it.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table)
            }
            listOf(create) + indices
        } + schema["setupQueries"]!!.jsonArray.map { it.jsonPrimitive.content }
        val connection = BundledSQLiteDriver().open(file.path)
        (statements + extraSql + "PRAGMA user_version = $version").forEach(connection::execSQL)
        connection.close()
    }

    private fun open(file: File): UltimateTasksDatabase {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        every { context.getDatabasePath(any()) } returns file
        return Room.databaseBuilder<UltimateTasksDatabase>(context, file.name)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*UltimateTasksDatabase.MIGRATIONS)
            .build()
    }

    @Test
    fun `migrates version 1 to the latest, keeps the data and stores credentials`() = runTest {
        val file = File(dir, "v1.db")
        createFromSchema(
            file,
            version = 1,
            extraSql = listOf(
                "INSERT INTO account (id, serverUrl, userId, displayName) " +
                    "VALUES (1, 'https://c.example/', 'ana', 'Ana')",
                "INSERT INTO task_list (accountId, href, name, visible, writable) " +
                    "VALUES (1, '/l/', 'List', 1, 1)",
                "INSERT INTO task (id, accountId, listHref, href, uid, summary, notes, " +
                    "completed, priority, tags, dirtyFields, deleted) " +
                    "VALUES (100, 1, '/l/', '/l/t.ics', 't', 'Task', '', 0, 0, '', 0, 0)"
            )
        )

        val db = open(file)
        val task = db.taskDao().get(100)
        db.credentialsDao().put(
            AccountCredentialsEntity(1, "ana", byteArrayOf(1, 2), byteArrayOf(3))
        )
        val stored = db.credentialsDao().get(1)
        db.accountDao().delete(1)
        val afterDelete = db.credentialsDao().get(1)
        db.close()

        assertEquals("Task", task?.summary)
        assertEquals("ana", stored?.loginName)
        assertNull(afterDelete)
    }

    @Test
    fun `migrates version 2 to the latest with no conflicts pending`() = runTest {
        val file = File(dir, "v2.db")
        createFromSchema(
            file,
            version = 2,
            extraSql = listOf(
                "INSERT INTO account (id, serverUrl, userId, displayName) " +
                    "VALUES (1, 'https://c.example/', 'ana', 'Ana')",
                "INSERT INTO task_list (accountId, href, name, visible, writable) " +
                    "VALUES (1, '/l/', 'List', 1, 1)",
                "INSERT INTO task (id, accountId, listHref, href, uid, summary, notes, " +
                    "completed, priority, tags, dirtyFields, deleted) " +
                    "VALUES (100, 1, '/l/', '/l/t.ics', 't', 'Task', '', 0, 0, '', 1, 0)"
            )
        )

        val db = open(file)
        val task = db.taskDao().get(100)
        db.close()

        assertEquals("Task", task?.summary)
        assertEquals(1, task?.dirtyFields)
        assertNull(task?.conflictSummary)
        assertNull(task?.conflictNotes)
        assertEquals(false, task?.deletedOnServer)
    }

    @Test
    fun `migrates version 3 to the latest with snoozes that go with their task`() = runTest {
        val file = File(dir, "v3.db")
        createFromSchema(
            file,
            version = 3,
            extraSql = listOf(
                "INSERT INTO account (id, serverUrl, userId, displayName) " +
                    "VALUES (1, 'https://c.example/', 'ana', 'Ana')",
                "INSERT INTO task_list (accountId, href, name, visible, writable) " +
                    "VALUES (1, '/l/', 'List', 1, 1)",
                "INSERT INTO task (id, accountId, listHref, href, uid, summary, notes, " +
                    "completed, priority, tags, dirtyFields, deleted, deletedOnServer) " +
                    "VALUES (100, 1, '/l/', '/l/t.ics', 't', 'Task', '', 0, 0, '', 0, 0, 0)"
            )
        )

        val db = open(file)
        db.reminderDao().snooze(SnoozeEntity(100, Instant.parse("2026-10-03T10:00:00Z")))
        val snoozes = db.reminderDao().observeSnoozes().first()
        db.taskDao().delete(100)
        val afterDelete = db.reminderDao().observeSnoozes().first()
        db.close()

        assertEquals(listOf(100L), snoozes.map { it.taskId })
        assertEquals(emptyList<SnoozeEntity>(), afterDelete)
    }
}
