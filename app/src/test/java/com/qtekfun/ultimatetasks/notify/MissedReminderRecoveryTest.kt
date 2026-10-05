// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.ShownReminderEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.local.inMemoryDatabase
import com.qtekfun.ultimatetasks.data.settings.FakePreferences
import com.qtekfun.ultimatetasks.data.settings.SettingsRepository
import com.qtekfun.ultimatetasks.domain.reminders.Reminder
import com.qtekfun.ultimatetasks.sync.queue.MutableClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Missed reminders against a real database: what shows, what does not, and only once (T32). */
class MissedReminderRecoveryTest {
    private val db = inMemoryDatabase()
    private val preferences = FakePreferences()
    private val settings = SettingsRepository(preferences)
    private val clock = MutableClock(Instant.parse("2026-10-05T10:00:00Z"))
    private val account = MutableStateFlow<AccountEntity?>(null)
    private val notifier = mockk<ReminderNotifier>(relaxed = true)
    private val shown = mutableListOf<Pair<Reminder, Boolean>>()
    private val recovery = MissedReminderRecovery(
        mockk<AccountSession> { every { activeAccount } returns account },
        db,
        settings,
        notifier,
        preferences,
        clock
    )

    init {
        every { notifier.show(any(), any()) } answers
            { shown += firstArg<Reminder>() to secondArg() }
    }

    @AfterEach
    fun close() = db.close()

    private suspend fun signIn() {
        val id = db.accountDao().insert(
            AccountEntity(serverUrl = "https://c.example/", userId = "ana", displayName = "Ana")
        )
        db.taskListDao().upsert(listOf(TaskListEntity(accountId = id, href = "/l/", name = "Home")))
        account.value = db.accountDao().get(id)
    }

    /** A task due at [due] (UTC), whose main reminder has id `2 * task id`. */
    private suspend fun task(summary: String, due: String): Long = db.taskDao().insert(
        TaskEntity(
            accountId = account.value!!.id,
            listHref = "/l/",
            href = "/l/$summary.ics",
            uid = summary,
            summary = summary,
            due = due,
            dueZone = "UTC"
        )
    )

    @Test
    fun `without an account nothing happens`() = runTest {
        recovery.recover()
        verify(exactly = 0) { notifier.show(any(), any()) }
    }

    @Test
    fun `the first run only records what is past, later runs bring back what was missed`() =
        runTest {
            signIn()
            task("old", "2026-10-05T08:00")
            recovery.recover()
            assertEquals(emptyList<Pair<Reminder, Boolean>>(), shown)

            task("missed", "2026-10-05T09:30")
            clock.now = clock.now.plusSeconds(60)
            recovery.recover()
            recovery.recover()
            assertEquals(listOf("missed" to true), shown.map { it.first.title to it.second })
        }

    @Test
    fun `a reminder that showed on time is not brought back`() = runTest {
        signIn()
        recovery.recover()
        val id = task("on time", "2026-10-05T09:59")
        recovery.markShown(id * 2, Instant.parse("2026-10-05T09:59:00Z"))
        recovery.recover()
        assertEquals(emptyList<Pair<Reminder, Boolean>>(), shown)
    }

    @Test
    fun `only the chosen window counts, and never when turned off`() = runTest {
        signIn()
        recovery.recover()
        task("yesterday morning", "2026-10-04T08:00")
        task("this morning", "2026-10-05T08:00")
        settings.setMissedWindowHours(6)
        recovery.recover()
        assertEquals(listOf("this morning"), shown.map { it.first.title })

        task("just now", "2026-10-05T09:50")
        settings.setMissedWindowHours(0)
        recovery.recover()
        assertEquals(listOf("this morning"), shown.map { it.first.title })
    }

    @Test
    fun `records older than any window are forgotten`() = runTest {
        signIn()
        val dao = db.shownReminderDao()
        dao.markShown(
            listOf(
                ShownReminderEntity(2, clock.now.minus(Duration.ofHours(49)), clock.now),
                ShownReminderEntity(4, clock.now.minus(Duration.ofHours(47)), clock.now)
            )
        )
        recovery.recover()
        assertEquals(listOf(4L), dao.shown().map { it.reminderId })
    }
}
