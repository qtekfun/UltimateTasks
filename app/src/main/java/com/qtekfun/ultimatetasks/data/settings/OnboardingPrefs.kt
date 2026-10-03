// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Named

/** Whether the reminders wizard was already shown after signing in (RF-01). */
class OnboardingPrefs @Inject constructor(
    @Named(SettingsRepository.SETTINGS_PREFERENCES) private val preferences: SharedPreferences
) {
    fun wizardShown(): Boolean = preferences.getBoolean(KEY, false)

    fun markWizardShown() = preferences.edit { putBoolean(KEY, true) }

    private companion object {
        const val KEY = "reminders_wizard_shown"
    }
}
