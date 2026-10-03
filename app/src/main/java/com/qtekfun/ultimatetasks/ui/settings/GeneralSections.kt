// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.settings.AppSettings
import com.qtekfun.ultimatetasks.data.settings.SettingsRepository
import com.qtekfun.ultimatetasks.data.settings.ThemeMode
import com.qtekfun.ultimatetasks.ui.detail.Choice
import com.qtekfun.ultimatetasks.ui.detail.DetailCard

/** Visible lists, the default list for new tasks and whether lists may be deleted (RF-08, RF-09, RF-14). */
@Composable
fun ListsSection(
    settings: AppSettings,
    lists: List<TaskListEntity>,
    repository: SettingsRepository,
    onVisibleLists: () -> Unit
) {
    val writable = lists.filter { it.writable }
    val default = writable.firstOrNull { it.href == settings.defaultList } ?: writable.firstOrNull()
    DetailCard {
        Row(
            Modifier.fillMaxWidth().heightIn(
                min = 56.dp
            ).clickable(role = Role.Button, onClick = onVisibleLists).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_visible_lists), Modifier.weight(1f))
            Text(
                "${lists.count {
                    it.visible
                }}/${lists.size}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
        HorizontalDivider(Modifier.padding(start = 16.dp))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_default_list), Modifier.weight(1f))
            Choice(default?.name ?: "—", writable, {
                it.name
            }) { repository.setDefaultList(it.href) }
        }
        HorizontalDivider(Modifier.padding(start = 16.dp))
        SwitchSetting(
            stringResource(R.string.settings_allow_deleting_lists),
            stringResource(R.string.settings_allow_deleting_lists_hint),
            settings.allowDeletingLists,
            repository::setAllowDeletingLists
        )
    }
}

/** Theme, pure black, wallpaper colors and language. */
@Composable
fun AppearanceSection(settings: AppSettings, repository: SettingsRepository) {
    DetailCard {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_theme), Modifier.weight(1f))
            Choice(themeName(settings.theme), ThemeMode.entries, {
                themeName(it)
            }, repository::setTheme)
        }
        HorizontalDivider(Modifier.padding(start = 16.dp))
        SwitchSetting(
            stringResource(R.string.settings_amoled),
            stringResource(R.string.settings_amoled_hint),
            settings.amoled,
            repository::setAmoled
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchSetting(
                stringResource(R.string.settings_dynamic_color),
                null,
                settings.dynamicColor,
                repository::setDynamicColor
            )
        }
        if (AppLanguages.supported) {
            HorizontalDivider(Modifier.padding(start = 16.dp))
            LanguageRow()
        }
    }
}

@Composable
private fun LanguageRow() {
    val context = LocalContext.current
    // Kept by the system (Android 13+): read once, and the activity restarts when it changes.
    var language by remember {
        mutableStateOf(
            if (AppLanguages.supported) AppLanguages.current(context) else AppLanguage.SYSTEM
        )
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.settings_language), Modifier.weight(1f))
        Choice(languageName(language), AppLanguage.entries, { languageName(it) }) {
            language = it
            if (AppLanguages.supported) AppLanguages.set(context, it)
        }
    }
}

@Composable
private fun themeName(theme: ThemeMode): String = stringResource(
    when (theme) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }
)

@Composable
private fun languageName(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)

    // Each language is named in itself, as language pickers do.
    AppLanguage.ENGLISH -> "English"

    AppLanguage.SPANISH -> "Español"
}
