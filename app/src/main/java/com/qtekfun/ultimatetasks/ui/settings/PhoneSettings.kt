// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.IntentCompat
import com.qtekfun.ultimatetasks.domain.reminders.MakerScreens
import com.qtekfun.ultimatetasks.domain.reminders.PhoneMaker

/** Settings only the user can change, outside the permission dialogs. */
object PhoneSettings {
    /** The system screen with "Pause app activity if unused" for this app. */
    fun askUnusedAppsExemption(context: Context) {
        ReminderPermissions.tryOpen(
            context,
            IntentCompat.createManageUnusedAppRestrictionsIntent(context, context.packageName)
        )
    }

    /**
     * The maker's own auto-start screen when the system lets apps open it, otherwise the app's
     * info page, where recent systems keep the same switches.
     */
    fun openMakerSettings(context: Context, maker: PhoneMaker) {
        val opened = MakerScreens.of(maker).any { screen ->
            ReminderPermissions.tryOpen(
                context,
                Intent().setClassName(screen.packageName, screen.className)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        if (!opened) ReminderPermissions.openAppSettings(context)
    }
}
