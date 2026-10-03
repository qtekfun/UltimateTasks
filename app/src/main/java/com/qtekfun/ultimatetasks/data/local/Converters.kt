// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local

import androidx.room3.ColumnTypeConverter
import com.qtekfun.ultimatetasks.data.ical.IcsAttachment
import java.time.Instant
import kotlinx.serialization.json.Json

/** Room type converters: instants as epoch milliseconds, lists as separator-joined text, attachments as JSON. */
class Converters {
    @ColumnTypeConverter
    fun instantToMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @ColumnTypeConverter
    fun millisToInstant(millis: Long?): Instant? = millis?.let(Instant::ofEpochMilli)

    @ColumnTypeConverter
    fun stringsToText(values: List<String>): String = values.joinToString(SEPARATOR)

    @ColumnTypeConverter
    fun textToStrings(text: String): List<String> =
        if (text.isEmpty()) emptyList() else text.split(SEPARATOR)

    @ColumnTypeConverter
    fun attachmentsToText(values: List<IcsAttachment>): String = Json.encodeToString(values)

    @ColumnTypeConverter
    fun textToAttachments(text: String): List<IcsAttachment> =
        runCatching { Json.decodeFromString<List<IcsAttachment>>(text) }.getOrDefault(emptyList())

    private companion object {
        /** ASCII unit separator: it never appears in tags typed by people. */
        const val SEPARATOR = "\u001F"
    }
}
