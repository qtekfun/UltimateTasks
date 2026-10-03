// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.data.local.model.SmartCounts
import com.qtekfun.ultimatetasks.data.task.HomeData
import com.qtekfun.ultimatetasks.data.task.ListSummary
import com.qtekfun.ultimatetasks.domain.task.SmartList
import com.qtekfun.ultimatetasks.domain.task.TaskSource
import com.qtekfun.ultimatetasks.ui.list.smartColor
import com.qtekfun.ultimatetasks.ui.list.smartName
import com.qtekfun.ultimatetasks.ui.theme.ListColors
import java.time.LocalDate

/** The home screen of Apple Reminders (RF-02): four smart lists in color, then "My lists". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    accountName: String,
    onOpen: (TaskSource) -> Unit,
    onLogOut: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val home by viewModel.home.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = {},
                actions = { HomeMenu(onLogOut = { confirmLogout = true }) }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = syncing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            HomeContent(home, onOpen)
        }
    }
    if (confirmLogout) {
        LogoutDialog(
            accountName = accountName,
            onConfirm = {
                confirmLogout = false
                onLogOut()
            },
            onDismiss = { confirmLogout = false }
        )
    }
}

@Composable
private fun HomeContent(data: HomeData?, onOpen: (TaskSource) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        item { SmartGrid(data?.counts ?: SmartCounts(0, 0, 0, 0), onOpen) }
        item {
            Text(
                stringResource(R.string.my_lists),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, start = 4.dp).semantics {
                    heading()
                }
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    val lists = data?.lists.orEmpty()
                    if (lists.isEmpty()) {
                        Text(
                            stringResource(
                                if (data ==
                                    null
                                ) {
                                    R.string.loading
                                } else {
                                    R.string.no_lists
                                }
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    lists.forEachIndexed { index, summary ->
                        ListRow(summary) { onOpen(TaskSource.List(summary.list.href)) }
                        if (index <
                            lists.lastIndex
                        ) {
                            HorizontalDivider(Modifier.padding(start = 60.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartGrid(counts: SmartCounts, onOpen: (TaskSource) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmartTile(SmartList.TODAY, counts.today, onOpen, Modifier.weight(1f))
            SmartTile(SmartList.SCHEDULED, counts.scheduled, onOpen, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmartTile(SmartList.ALL, counts.all, onOpen, Modifier.weight(1f))
            SmartTile(SmartList.COMPLETED, counts.completed, onOpen, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SmartTile(
    kind: SmartList,
    count: Int,
    onOpen: (TaskSource) -> Unit,
    modifier: Modifier
) {
    val color = smartColor(kind)
    val name = stringResource(smartName(kind))
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.heightIn(min = 84.dp).clickable(role = Role.Button) {
            onOpen(TaskSource.Smart(kind))
        }
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                SmartIcon(kind, color)
                Spacer(Modifier.weight(1f))
                if (kind != SmartList.COMPLETED) {
                    Text(
                        "$count",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Today shows the day of the month, like Apple's calendar icon. */
@Composable
private fun SmartIcon(kind: SmartList, color: Color) {
    Box(Modifier.size(32.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
        val icon: ImageVector? = when (kind) {
            SmartList.TODAY -> null
            SmartList.SCHEDULED -> Icons.Default.DateRange
            SmartList.ALL -> Icons.AutoMirrored.Filled.List
            SmartList.COMPLETED -> Icons.Default.Check
        }
        if (icon == null) {
            Text(
                "${LocalDate.now().dayOfMonth}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        } else {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ListRow(summary: ListSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(
            min = 56.dp
        ).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(32.dp).background(ListColors.of(summary.list.color), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            summary.list.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (!summary.list.writable) {
            Icon(
                Icons.Default.Lock,
                contentDescription = stringResource(R.string.read_only_list),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("${summary.open}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HomeMenu(onLogOut: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = {
        open = true
    }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options)) }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.logout)) },
            onClick = {
                open = false
                onLogOut()
            }
        )
    }
}

@Composable
private fun LogoutDialog(accountName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.logout_confirm_title)) },
        text = { Text(stringResource(R.string.logout_confirm_text, accountName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.logout)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
