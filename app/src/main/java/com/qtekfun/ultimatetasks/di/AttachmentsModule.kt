// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.di

import com.qtekfun.ultimatetasks.data.attachments.AndroidAttachmentFiles
import com.qtekfun.ultimatetasks.data.attachments.AttachmentFiles
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AttachmentsModule {
    @Binds
    abstract fun attachmentFiles(files: AndroidAttachmentFiles): AttachmentFiles
}
