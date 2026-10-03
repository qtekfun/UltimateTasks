// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.queue

/** Sends one queued operation to the server; implemented by the sync engine (T09). */
fun interface OperationExecutor {
    /**
     * [maybeSent] is true when an earlier run was started and may have reached the server
     * without its answer arriving (timeout, app closed): a create then expects a 412.
     */
    suspend fun execute(
        taskId: Long,
        operation: QueuedOperation,
        maybeSent: Boolean
    ): ExecutionResult
}

/** What happened when an operation ran. */
sealed interface ExecutionResult {
    data object Done : ExecutionResult

    /** Temporary problem (network, 5xx): try again later with backoff. */
    data class Retry(val reason: String?) : ExecutionResult

    /** The server refused it for good (e.g. read-only list): wait for the user. */
    data class Failed(val reason: String?) : ExecutionResult
}
