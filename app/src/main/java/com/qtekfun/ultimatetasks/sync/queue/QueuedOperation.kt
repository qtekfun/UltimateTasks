// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.sync.queue

import com.qtekfun.ultimatetasks.data.local.model.OperationType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A local change waiting for the server, stored as the JSON payload of a pending operation.
 * Creating and updating send the task as it is when the operation runs, so repeating them is
 * harmless and repeated changes need a single operation.
 */
@Serializable
sealed interface QueuedOperation {
    val type: OperationType

    /** PUT with `If-None-Match: *` to the href the app chose: a lost answer is detectable (412). */
    @Serializable
    @SerialName("create_task")
    object CreateTask : QueuedOperation {
        override val type get() = OperationType.CREATE
    }

    @Serializable
    @SerialName("update_task")
    object UpdateTask : QueuedOperation {
        override val type get() = OperationType.UPDATE
    }

    /** Moves the resource from the list [from] to the list [to] (RF-05). */
    @Serializable
    @SerialName("move_task")
    data class MoveTask(val from: String, val to: String) : QueuedOperation {
        override val type get() = OperationType.MOVE
    }

    /** Carries what the server needs, since the task itself may be gone locally by then. */
    @Serializable
    @SerialName("delete_task")
    data class DeleteTask(val href: String, val etag: String?) : QueuedOperation {
        override val type get() = OperationType.DELETE
    }

    companion object {
        /** Payload JSON; the class name goes in "op" to keep it apart from the fields. */
        val json: Json = Json {
            classDiscriminator = "op"
            ignoreUnknownKeys = true
        }

        fun encode(operation: QueuedOperation): String =
            json.encodeToString(serializer(), operation)

        fun decode(payload: String): QueuedOperation = json.decodeFromString(serializer(), payload)
    }
}
