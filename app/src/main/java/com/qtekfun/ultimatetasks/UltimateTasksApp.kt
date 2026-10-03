// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks

import android.app.Application
import androidx.work.Configuration
import com.qtekfun.ultimatetasks.sync.engine.SyncWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class UltimateTasksApp :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: SyncWorkerFactory

    /** WorkManager starts on demand with this factory; its default initializer is off. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
