// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Asks to show notifications with the system dialog. Once the user has refused it twice the
 * system no longer shows it, so the app's notification settings open instead. Before Android 13
 * there is no dialog, and notifications blocked in the settings are switched on there too.
 */
@Composable
fun rememberNotificationRequest(onResult: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val blocked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                activity?.shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS
                ) ==
                false
            if (!granted && blocked) ReminderPermissions.openNotificationSettings(context)
            onResult()
        }
    return {
        // Granted but switched off in the settings: only the settings can turn them back on.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ReminderPermissions.notificationPermissionGranted(context)
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ReminderPermissions.openNotificationSettings(context)
        }
    }
}
