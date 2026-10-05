// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatetasks.domain.reminders.TestDelivery
import com.qtekfun.ultimatetasks.notify.TestReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Sends the test reminder and tells how it went (T33). */
@HiltViewModel
class TestReminderViewModel @Inject constructor(private val test: TestReminder) : ViewModel() {
    private val mutableDelivery = MutableStateFlow(test.delivery())
    val delivery: StateFlow<TestDelivery?> = mutableDelivery.asStateFlow()

    fun send(title: String) {
        viewModelScope.launch {
            test.send(title)
            check()
        }
    }

    /** Reads it again: it arrives through an alarm, outside the screen. */
    fun check() {
        mutableDelivery.value = test.delivery()
    }
}
