// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** The device's current date and zone: what "today" and every shown time depend on. */
data class DayZone(val date: LocalDate, val zone: ZoneId)

/**
 * Emits the current [DayZone] and again whenever the day, the clock or the device's zone
 * changes, so screens built on "today" do not stay stale after midnight or a trip.
 */
interface LocalDay {
    val changes: Flow<DayZone>
}

@Singleton
class SystemLocalDay @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock
) : LocalDay {
    override val changes: Flow<DayZone> = callbackFlow {
        val send = {
            val zone = ZoneId.systemDefault()
            trySend(DayZone(LocalDate.now(clock.withZone(zone)), zone))
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                send()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        send()
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}
