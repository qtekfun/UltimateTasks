// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatetasks.data.auth.AccountSession
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.domain.auth.Logout
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Who is signed in. [ready] stays false until stored credentials are loaded, to avoid flashing
 * the login screen.
 */
data class SessionState(val ready: Boolean = false, val account: AccountEntity? = null)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val session: AccountSession,
    private val logout: Logout
) : ViewModel() {
    private val mutableState = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            session.restore()
            session.activeAccount.collect { account ->
                mutableState.value = SessionState(ready = true, account = account)
            }
        }
    }

    fun logOut() {
        viewModelScope.launch { logout() }
    }
}
