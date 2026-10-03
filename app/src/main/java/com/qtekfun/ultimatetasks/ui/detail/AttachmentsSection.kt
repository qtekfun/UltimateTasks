// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtekfun.ultimatetasks.R
import com.qtekfun.ultimatetasks.data.ical.IcsAttachment
import com.qtekfun.ultimatetasks.data.local.entity.PendingUploadEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity

/** Files of the task (RF-11): open in Nextcloud, unlink, and add from the gallery or files. */
@Composable
fun AttachmentsSection(task: TaskEntity, editable: Boolean, viewModel: TaskDetailViewModel) {
    val uploads by viewModel.uploads.collectAsStateWithLifecycle()
    if (task.attachments.isEmpty() && uploads.isEmpty() && !editable) return
    DetailCard {
        Text(
            stringResource(R.string.field_attachments),
            Modifier.padding(start = 16.dp, top = 12.dp)
        )
        task.attachments.forEach { attachment ->
            AttachmentRow(
                attachment,
                onRemove = {
                    viewModel.files.remove(attachment.url)
                }.takeIf { editable }
            )
            HorizontalDivider(Modifier.padding(start = 16.dp))
        }
        uploads.forEach { upload -> UploadRow(upload) { viewModel.files.discard(upload) } }
        if (editable) AddButtons(viewModel.files::attach)
    }
}

@Composable
private fun AttachmentRow(attachment: IcsAttachment, onRemove: (() -> Unit)?) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) {
            open(context, attachment.url)
        }
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(attachment.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        onRemove?.let {
            IconButton(onClick = it) {
                Icon(
                    Icons.Default.Close,
                    stringResource(R.string.attachment_remove, attachment.name)
                )
            }
        }
    }
}

/** A file still uploading, or one that failed and can be dropped. */
@Composable
private fun UploadRow(upload: PendingUploadEntity, onDiscard: () -> Unit) {
    val failed = upload.error != null
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(upload.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            stringResource(
                if (failed) R.string.attachment_failed else R.string.attachment_uploading
            ),
            style = MaterialTheme.typography.bodySmall,
            color = with(MaterialTheme.colorScheme) { if (failed) error else onSurfaceVariant }
        )
        if (failed) {
            IconButton(onClick = onDiscard) {
                Icon(Icons.Default.Close, stringResource(R.string.attachment_remove, upload.name))
            }
        }
    }
}

@Composable
private fun AddButtons(onPicked: (String) -> Unit) {
    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { onPicked(it.toString()) }
        }
    val file =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { onPicked(it.toString()) }
        }
    Row {
        TextButton(onClick = {
            gallery.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
        }) {
            Text(stringResource(R.string.attachment_from_gallery))
        }
        TextButton(onClick = {
            file.launch(arrayOf("*/*"))
        }) { Text(stringResource(R.string.attachment_from_files)) }
    }
}

/** Opens the Nextcloud link in the browser; without one, nothing happens. */
private fun open(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        // No app can open links.
    }
}
