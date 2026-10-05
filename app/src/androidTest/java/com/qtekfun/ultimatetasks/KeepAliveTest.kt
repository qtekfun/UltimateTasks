// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.settings.OnboardingPrefs
import com.qtekfun.ultimatetasks.data.settings.SettingFlag
import com.qtekfun.ultimatetasks.data.settings.SettingsRepository
import com.qtekfun.ultimatetasks.notify.KeepAliveController
import com.qtekfun.ultimatetasks.notify.KeepAliveService
import com.qtekfun.ultimatetasks.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robust mode (T34): the foreground service runs exactly while the mode is on for a signed-in
 * account. The app is opened first, since Android 12+ only starts such services from the front.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class KeepAliveTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    lateinit var database: UltimateTasksDatabase

    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var controller: KeepAliveController

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        hilt.inject()
        val preferences = context.getSharedPreferences(UI_TEST_SETTINGS, Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        OnboardingPrefs(preferences).markWizardShown()
        runBlocking {
            database.accountDao().insert(AccountEntity(1, "https://cloud.example/", "ana", "Ana"))
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        controller.start(scope)
    }

    @After
    fun tearDown() {
        settings.setFlag(SettingFlag.ROBUST_MODE, false)
        scope.cancel()
        KeepAliveService.stop(context)
        scenario?.close()
    }

    @Test
    fun turningTheModeOnStartsTheServiceAndOffStopsIt() = runBlocking {
        settings.setFlag(SettingFlag.ROBUST_MODE, true)
        withTimeout(TIMEOUT) { KeepAliveService.running.first { it } }

        settings.setFlag(SettingFlag.ROBUST_MODE, false)
        withTimeout(TIMEOUT) { KeepAliveService.running.first { !it } }
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
