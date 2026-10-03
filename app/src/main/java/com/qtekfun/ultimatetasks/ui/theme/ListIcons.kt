// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.ui.graphics.vector.ImageVector

/** The icons a list can have (RF-08), stored by name on this device only. */
object ListIcons {
    val all: Map<String, ImageVector> = linkedMapOf(
        "list" to Icons.AutoMirrored.Filled.List,
        "home" to Icons.Default.Home,
        "cart" to Icons.Default.ShoppingCart,
        "star" to Icons.Default.Star,
        "heart" to Icons.Default.Favorite,
        "work" to Icons.Default.Build,
        "calendar" to Icons.Default.DateRange,
        "mail" to Icons.Default.Email,
        "phone" to Icons.Default.Phone,
        "person" to Icons.Default.AccountCircle,
        "family" to Icons.Default.Face,
        "place" to Icons.Default.Place,
        "bell" to Icons.Default.Notifications,
        "settings" to Icons.Default.Settings,
        "idea" to Icons.Default.Info,
        "like" to Icons.Default.ThumbUp
    )

    fun of(name: String?): ImageVector = all[name] ?: Icons.AutoMirrored.Filled.List
}
