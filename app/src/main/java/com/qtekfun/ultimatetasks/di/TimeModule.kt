// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.di

import com.qtekfun.ultimatetasks.data.time.LocalDay
import com.qtekfun.ultimatetasks.data.time.SystemLocalDay
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import kotlin.random.Random

/** Time and randomness, injected so tests can fix them. */
@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {
    @Binds
    abstract fun localDay(impl: SystemLocalDay): LocalDay

    companion object {
        @Provides
        fun clock(): Clock = Clock.systemUTC()

        @Provides
        fun random(): Random = Random.Default
    }
}
