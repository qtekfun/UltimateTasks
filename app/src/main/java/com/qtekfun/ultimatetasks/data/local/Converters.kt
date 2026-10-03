// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.local

import androidx.room3.ColumnTypeConverter
import java.time.Instant

/** Room type converters: instants as epoch milliseconds, lists as separator-joined text. */
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

    private companion object {
        /** ASCII unit separator: it never appears in tags typed by people. */
        const val SEPARATOR = "\u001F"
    }
}
