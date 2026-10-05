// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.engine

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.qtekfun.ultimatetasks.notify.MissedReminderRecovery
import javax.inject.Inject
import javax.inject.Provider

/**
 * Runs a sync in the background; WorkManager retries it with backoff when it fails. [afterSync]
 * then runs whatever the outcome: tasks may have changed (T32, missed reminders).
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val engine: SyncEngine,
    private val afterSync: suspend () -> Unit = {}
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (engine.sync().also(::log).also { afterSync() }) {
        is SyncOutcome.Ok, SyncOutcome.NoAccount -> Result.success()

        SyncOutcome.Offline, is SyncOutcome.Error -> Result.retry()

        // Retrying cannot help until the user signs in again.
        SyncOutcome.Unauthorized -> Result.failure()
    }

    /** Only the outcome kind and HTTP codes are logged, never task data or credentials. */
    private fun log(outcome: SyncOutcome) {
        Log.i(TAG, "Sync finished: $outcome")
    }

    private companion object {
        const val TAG = "UltimateTasksSync"
    }
}

/** Creates workers with their dependencies, without an extra Hilt-WorkManager library. */
class SyncWorkerFactory @Inject constructor(
    private val engine: Provider<SyncEngine>,
    private val recovery: Provider<MissedReminderRecovery>
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? = if (workerClassName == SyncWorker::class.java.name) {
        SyncWorker(appContext, workerParameters, engine.get()) { recovery.get().recover() }
    } else {
        null
    }
}
