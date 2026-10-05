// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qtekfun.ultimatetasks.domain.reminders.Reminder
import com.qtekfun.ultimatetasks.notify.HeartbeatScheduler
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import javax.inject.Inject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The heartbeat's alarm is set on the real AlarmManager only while a reminder is to come (T33). */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HeartbeatTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    lateinit var heartbeat: HeartbeatScheduler

    @Before
    fun setUp() {
        hilt.inject()
    }

    @After
    fun tearDown() {
        heartbeat.update(emptyList())
    }

    private fun reminderIn(seconds: Long): Reminder {
        val at = Instant.now().plusSeconds(seconds)
        return Reminder(2, 1, "Task", "List", at, at, early = false)
    }

    @Test
    fun beatsWhileAReminderIsToCome() {
        heartbeat.update(listOf(reminderIn(3600)))
        assertTrue(heartbeat.isScheduled())
    }

    @Test
    fun stopsWhenNoneIsLeft() {
        heartbeat.update(listOf(reminderIn(3600)))
        heartbeat.update(listOf(reminderIn(-60)))
        assertFalse(heartbeat.isScheduled())
    }
}
