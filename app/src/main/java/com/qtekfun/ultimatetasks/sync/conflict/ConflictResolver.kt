// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.conflict

import com.qtekfun.ultimatetasks.data.ical.VtodoFields
import java.time.Instant

/** The two versions of a text field changed on both sides, for the user to choose (rule 1). */
data class TextConflict(val field: TaskField, val local: String, val server: String)

/** What to do with a task after comparing it with the server. */
sealed interface Resolution {
    /**
     * Keep [fields] locally. [push] are the fields where the local value won and still has to
     * reach the server; [conflicts] are text fields waiting for the user, kept with the local
     * text meanwhile and not sent.
     */
    data class Merge(
        val fields: VtodoFields,
        val push: Set<TaskField>,
        val conflicts: List<TextConflict>
    ) : Resolution

    /** Deleted on the server and unchanged here: delete it locally too. */
    data object DeleteLocally : Resolution

    /** Deleted on the server but changed here: ask to keep a copy or discard it (rule 3). */
    data class DeletedOnServer(val local: VtodoFields) : Resolution
}

/** The local task: its fields, which of them changed here and when the last change was made. */
data class LocalVersion(val fields: VtodoFields, val dirty: Set<TaskField>, val changedAt: Instant?)

/** The server task: its fields, or null when it was deleted, and its LAST-MODIFIED. */
data class ServerVersion(val fields: VtodoFields?, val changedAt: Instant?)

/**
 * Three-way merge of a task (SPEC §5). [base] is the version both sides last agreed on (the
 * last one read from the server). Pure: it only decides.
 */
object ConflictResolver {
    fun resolve(base: VtodoFields?, local: LocalVersion, server: ServerVersion): Resolution {
        val fields = server.fields
        return when {
            fields == null && local.dirty.isEmpty() -> Resolution.DeleteLocally
            fields == null -> Resolution.DeletedOnServer(local.fields)
            else -> merge(base, local, fields, server.changedAt)
        }
    }

    private fun merge(
        base: VtodoFields?,
        local: LocalVersion,
        server: VtodoFields,
        serverChangedAt: Instant?
    ): Resolution.Merge {
        var merged = server
        val push = mutableSetOf<TaskField>()
        val conflicts = mutableListOf<TextConflict>()
        for (field in local.dirty) {
            val serverChanged = base == null || field.read(server) != field.read(base)
            val same = field.read(server) == field.read(local.fields)
            val localWins = !serverChanged || localIsNewer(local.changedAt, serverChangedAt)
            when {
                same -> Unit

                serverChanged && field.isText -> {
                    merged = field.write(merged, local.fields)
                    conflicts +=
                        TextConflict(
                            field,
                            field.read(local.fields) as String,
                            field.read(server) as String
                        )
                }

                localWins -> {
                    merged = field.write(merged, local.fields)
                    push += field
                }

                else -> Unit
            }
        }
        return Resolution.Merge(merged, push, conflicts)
    }

    /** Last change wins (rule 2); a tie or an unknown server time keeps the local change. */
    private fun localIsNewer(local: Instant?, server: Instant?): Boolean =
        server == null || (local != null && !local.isBefore(server))
}
