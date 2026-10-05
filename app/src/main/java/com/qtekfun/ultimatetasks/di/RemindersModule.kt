// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.di

import com.qtekfun.ultimatetasks.notify.ReminderBeat
import com.qtekfun.ultimatetasks.notify.ReminderHeartbeat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RemindersModule {
    @Binds
    abstract fun reminderBeat(beat: ReminderHeartbeat): ReminderBeat
}
