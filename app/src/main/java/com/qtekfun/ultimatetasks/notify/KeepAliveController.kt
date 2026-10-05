// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.notify

import android.content.Context
import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Runs [KeepAliveService] exactly while robust mode is on and someone is signed in: it starts
 * when either becomes true and stops on logout or when the mode is turned off.
 */
@Singleton
class KeepAliveController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: AccountSession,
    private val settings: SettingsRepository
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(session.activeAccount, settings.settings) { account, settings ->
                account != null && settings.robustMode
            }
                .distinctUntilChanged()
                .collect { wanted ->
                    if (wanted) KeepAliveService.start(context) else KeepAliveService.stop(context)
                }
        }
    }

    /** Whether the service should run now, for the boot receiver. */
    suspend fun wanted(): Boolean =
        session.activeAccount.first() != null && settings.settings.first().robustMode
}
