// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatetasks.ui.login.LoginScreen
import com.qtekfun.ultimatetasks.ui.navigation.AppNavigation
import com.qtekfun.ultimatetasks.ui.session.SessionViewModel
import com.qtekfun.ultimatetasks.ui.theme.UltimateTasksTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UltimateTasksTheme {
                val sessionViewModel: SessionViewModel = viewModel()
                val session by sessionViewModel.state.collectAsStateWithLifecycle()
                val account = session.account
                when {
                    // Nothing is drawn until stored credentials are loaded (a few milliseconds).
                    !session.ready -> Unit

                    account == null -> LoginScreen()

                    else -> AppNavigation(
                        accountName = account.displayName,
                        onLogOut = sessionViewModel::logOut
                    )
                }
            }
        }
    }
}
