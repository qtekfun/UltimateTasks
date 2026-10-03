// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks

import android.content.Context
import android.content.SharedPreferences
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.settings.SettingsRepository
import com.qtekfun.ultimatetasks.di.DatabaseModule
import com.qtekfun.ultimatetasks.di.SettingsModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Named
import javax.inject.Singleton

/** A fresh in-memory database per test: the app's real data on the device is never touched. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): UltimateTasksDatabase =
        Room.inMemoryDatabaseBuilder<UltimateTasksDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .build()
}

/** Settings of their own, emptied for each test, so the user's settings stay as they are. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SettingsModule::class])
object TestSettingsModule {
    @Provides
    @Named(SettingsRepository.SETTINGS_PREFERENCES)
    fun settingsPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(UI_TEST_SETTINGS, Context.MODE_PRIVATE)
}

const val UI_TEST_SETTINGS = "ui-test-settings"
