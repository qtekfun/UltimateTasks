// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.settings.backup

import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.auth.CredentialStore
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.local.inMemoryDatabase
import com.qtekfun.ultimatetasks.data.remote.Credentials
import com.qtekfun.ultimatetasks.data.settings.FakePreferences
import com.qtekfun.ultimatetasks.data.settings.PendingListPrefs
import com.qtekfun.ultimatetasks.data.settings.SettingsRepository
import com.qtekfun.ultimatetasks.data.settings.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsBackupTest {
    private val oldDb = inMemoryDatabase()
    private val newDb = inMemoryDatabase()
    private val oldPhone = SettingsRepository(FakePreferences())
    private val newPrefs = FakePreferences()
    private val newPhone = SettingsRepository(newPrefs)
    private val pending = PendingListPrefs(newPrefs)
    private val store = mockk<CredentialStore>()
    private val password = "correct horse".toCharArray()

    @AfterEach
    fun close() {
        oldDb.close()
        newDb.close()
    }

    private suspend fun exported(withSession: Boolean): String {
        oldDb.accountDao().insert(AccountEntity(1, "https://cloud.example/", "ana", "Ana"))
        oldDb.taskListDao().upsert(
            listOf(
                TaskListEntity(1, "/a/", "A", icon = "cart"),
                TaskListEntity(1, "/deck/", "Deck: P", visible = false)
            )
        )
        coEvery { store.load(1) } returns Credentials("ana", "app-password")
        oldPhone.setTheme(ThemeMode.DARK)
        oldPhone.setAmoled(true)
        oldPhone.setAllDayHour(8)
        oldPhone.setDefaultList("/a/")
        oldPhone.setAllowDeletingLists(true)
        oldPhone.setAlarmClock(true)
        val session = mockk<AccountSession>(relaxed = true)
        return SettingsBackup(
            oldPhone,
            PendingListPrefs(FakePreferences()),
            session,
            oldDb,
            store
        ).export(if (withSession) password else null)
    }

    private fun restorer(account: AccountEntity?): Pair<SettingsBackup, AccountSession> {
        val session = mockk<AccountSession>(relaxed = true) {
            every { activeAccount } returns
                MutableStateFlow(account)
        }
        return SettingsBackup(newPhone, pending, session, newDb, store) to session
    }

    @Test
    fun `settings travel to a new phone`() = runTest {
        val backup = exported(withSession = false)
        val (restorer, _) = restorer(null)
        assertEquals(false, restorer.hasSessions(backup))
        assertEquals(RestoreResult.Restored(0), restorer.restore(backup, null))
        val restored = newPhone.settings.first()
        assertEquals(ThemeMode.DARK, restored.theme)
        assertTrue(restored.amoled && restored.alarmClock && restored.allowDeletingLists)
        assertEquals(8 to "/a/", restored.allDayHour to restored.defaultList)
    }

    @Test
    fun `list preferences apply now to known lists and later to the rest`() = runTest {
        val backup = exported(withSession = false)
        val id = newDb.accountDao().insert(
            AccountEntity(serverUrl = "https://cloud.example/", userId = "ana", displayName = "Ana")
        )
        newDb.taskListDao().upsert(listOf(TaskListEntity(id, "/a/", "A")))
        val (restorer, _) = restorer(newDb.accountDao().get(id))
        restorer.restore(backup, null)
        assertEquals("cart", newDb.taskListDao().get(id, "/a/")!!.icon)
        val waiting = pending.take("/deck/")!!
        assertFalse(waiting.visible)
        assertNull(pending.take("/deck/"))
        assertNull(pending.take("/a/"))
    }

    @Test
    fun `the session needs the password and signs in only when nobody is`() = runTest {
        val backup = exported(withSession = true)
        assertEquals(true, restorer(null).first.hasSessions(backup))
        assertEquals(
            RestoreResult.WrongPassword,
            restorer(null).first.restore(backup, "wrong".toCharArray())
        )
        val (fresh, session) = restorer(null)
        coEvery { session.signIn(any(), any()) } returns 7
        assertEquals(RestoreResult.Restored(1), fresh.restore(backup, password))
        coVerify {
            session.signIn(
                any(),
                match {
                    it.loginName == "ana" &&
                        it.appPassword == "app-password"
                }
            )
        }
        val (signedIn, other) = restorer(AccountEntity(2, "https://x/", "bob", "Bob"))
        assertEquals(RestoreResult.Restored(0), signedIn.restore(backup, password))
        coVerify(exactly = 0) { other.signIn(any(), any()) }
    }

    @Test
    fun `unreadable or future backups are refused`() = runTest {
        val (restorer, _) = restorer(null)
        assertEquals(RestoreResult.Invalid, restorer.restore("not json", null))
        assertEquals(RestoreResult.Invalid, restorer.restore("""{"settings":{}}""", null))
        assertNull(restorer.hasSessions("{}"))
        val future = exported(withSession = false).replace("\"format\":1", "\"format\":99")
        assertEquals(RestoreResult.Invalid, restorer.restore(future, null))
    }
}
