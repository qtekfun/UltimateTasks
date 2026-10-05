// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.settings.backup

import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.auth.CredentialStore
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.remote.Credentials
import com.qtekfun.ultimatetasks.data.remote.ServerUrl
import com.qtekfun.ultimatetasks.data.settings.AppSettings
import com.qtekfun.ultimatetasks.data.settings.ListPrefs
import com.qtekfun.ultimatetasks.data.settings.PendingListPrefs
import com.qtekfun.ultimatetasks.data.settings.SettingsRepository
import com.qtekfun.ultimatetasks.data.settings.ThemeMode
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Current backup format; older ones would be upgraded when read. */
private const val FORMAT = 1

/** The backup file: settings, list preferences and, optionally, the session sealed with a password. */
@Serializable
data class BackupFile(
    val format: Int = FORMAT,
    val settings: BackupSettings,
    val lists: List<ListPrefs> = emptyList(),
    val sessions: Sealed? = null
)

@Serializable
data class BackupSettings(
    val theme: String,
    val amoled: Boolean,
    val dynamicColor: Boolean,
    val defaultList: String? = null,
    val allowDeletingLists: Boolean,
    val alarmClock: Boolean,
    val allDayHour: Int,
    // Older backups have none: the default applies.
    val missedWindowHours: Int = AppSettings.DEFAULT_MISSED_WINDOW_HOURS
)

@Serializable
data class BackupSession(val serverUrl: String, val loginName: String, val appPassword: String)

/** How a restore went. */
sealed interface RestoreResult {
    /** Settings applied; [sessions] signed in (0 when there were none or one was already). */
    data class Restored(val sessions: Int) : RestoreResult

    data object WrongPassword : RestoreResult

    data object Invalid : RestoreResult
}

/**
 * Exports and restores the app's settings (T23), as UltimateDeck does, to move to a new phone.
 * The session (app password) is only included on request, sealed with the user's password.
 * Visibility and icons of the lists travel too: applied now to lists already here, and to the
 * others when the first sync brings them.
 */
class SettingsBackup @Inject constructor(
    private val settings: SettingsRepository,
    private val pending: PendingListPrefs,
    private val session: AccountSession,
    database: UltimateTasksDatabase,
    private val credentials: CredentialStore
) {
    private val accounts = database.accountDao()
    private val lists = database.taskListDao()
    private val json = Json {
        ignoreUnknownKeys = true
        // The format must be in the file, so a newer one is recognised and refused.
        encodeDefaults = true
    }

    /** The backup as JSON; with a [password], the signed-in session goes inside, sealed. */
    suspend fun export(password: CharArray?): String {
        val current = settings.settings.first()
        val all = accounts.observeAll().first()
        val listPrefs = all.flatMap {
            lists.all(it.id)
        }.map { ListPrefs(it.href, it.visible, it.icon) }
        val sealed = password?.let { secret ->
            val sessions = all.mapNotNull { account ->
                credentials.load(account.id)?.let {
                    BackupSession(account.serverUrl, it.loginName, it.appPassword)
                }
            }
            BackupCrypto.seal(json.encodeToString(sessions).toByteArray(), secret)
        }
        return json.encodeToString(
            BackupFile(settings = current.toBackup(), lists = listPrefs, sessions = sealed)
        )
    }

    /** Whether [backup] carries a session, so a password must be asked for; null if unreadable. */
    fun hasSessions(backup: String): Boolean? = parse(backup)?.let { it.sessions != null }

    /**
     * Applies the settings and list preferences and, when there is no session on this phone
     * yet, signs in with the one in the backup. A [password] is needed only for that.
     */
    suspend fun restore(backup: String, password: CharArray?): RestoreResult {
        val file = parse(backup) ?: return RestoreResult.Invalid
        return sessionsToSignIn(file, password)?.let { apply(file, it) }
            ?: RestoreResult.WrongPassword
    }

    private suspend fun apply(file: BackupFile, sessions: List<BackupSession>): RestoreResult {
        settings.restore(file.settings.toSettings())
        val count = sessions.count { saved ->
            val url = ServerUrl.parse(saved.serverUrl) as? ServerUrl.ParseResult.Valid
            url?.let { session.signIn(it.url, Credentials(saved.loginName, saved.appPassword)) } !=
                null
        }
        applyLists(file.lists)
        return RestoreResult.Restored(count)
    }

    /** Lists already here get their preferences now; the rest wait for the first sync. */
    private suspend fun applyLists(prefs: List<ListPrefs>) {
        val account = session.activeAccount.first()
        val here = account?.let { lists.all(it.id) }.orEmpty().associateBy { it.href }
        val (known, waiting) = prefs.partition { it.href in here }
        lists.update(
            known.map { pref ->
                here.getValue(pref.href).copy(visible = pref.visible, icon = pref.icon)
            }
        )
        pending.save(waiting)
    }

    /** The sessions to sign in with: none if not asked or already signed in; null on a wrong password. */
    private suspend fun sessionsToSignIn(
        file: BackupFile,
        password: CharArray?
    ): List<BackupSession>? {
        val sealed = file.sessions
        if (sealed == null || password == null ||
            session.activeAccount.first() != null
        ) {
            return emptyList()
        }
        return BackupCrypto.open(sealed, password)?.let {
            json.decodeFromString<List<BackupSession>>(it.decodeToString())
        }
    }

    private fun parse(backup: String): BackupFile? = try {
        json.decodeFromString<BackupFile>(backup).takeIf { it.format <= FORMAT }
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun AppSettings.toBackup() = BackupSettings(
    theme = theme.name,
    amoled = amoled,
    dynamicColor = dynamicColor,
    defaultList = defaultList,
    allowDeletingLists = allowDeletingLists,
    alarmClock = alarmClock,
    allDayHour = allDayHour,
    missedWindowHours = missedWindowHours
)

private fun BackupSettings.toSettings(): AppSettings = AppSettings(
    theme = ThemeMode.entries.firstOrNull { it.name == theme } ?: AppSettings().theme,
    amoled = amoled,
    dynamicColor = dynamicColor,
    defaultList = defaultList,
    allowDeletingLists = allowDeletingLists,
    alarmClock = alarmClock,
    allDayHour = allDayHour,
    missedWindowHours = missedWindowHours
)
