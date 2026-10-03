// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.queue

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.random.Random

/** A clock tests move by hand. */
class MutableClock(var now: Instant = Instant.parse("2026-10-01T10:00:00Z")) : Clock() {
    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = this

    fun advance(duration: Duration) {
        now = now.plus(duration)
    }
}

/** A Random whose nextDouble is fixed: 0.5 means no jitter, 0 and 1 are the extremes. */
class FixedRandom(private val value: Double) : Random() {
    override fun nextBits(bitCount: Int): Int = 0

    override fun nextDouble(): Double = value
}

/** Records every call and answers from a script per task; unscripted calls succeed. */
class ScriptedExecutor : OperationExecutor {
    val calls = mutableListOf<Pair<Long, QueuedOperation>>()
    private val scripts = mutableMapOf<Long, ArrayDeque<() -> ExecutionResult>>()

    fun on(taskId: Long, vararg results: () -> ExecutionResult) {
        scripts.getOrPut(taskId) { ArrayDeque() }.addAll(results)
    }

    /** Whether each call was told the operation may already have been sent. */
    val maybeSent = mutableListOf<Boolean>()

    override suspend fun execute(
        taskId: Long,
        operation: QueuedOperation,
        maybeSent: Boolean
    ): ExecutionResult {
        calls += taskId to operation
        this.maybeSent += maybeSent
        return scripts[taskId]?.removeFirstOrNull()?.invoke() ?: ExecutionResult.Done
    }
}
