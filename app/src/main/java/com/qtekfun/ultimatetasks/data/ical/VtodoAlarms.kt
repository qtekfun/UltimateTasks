// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.ical

/** The reminder before the due date (RF-10), one `VALARM` among any others the task has. */
object VtodoAlarms {
    /** A DISPLAY alarm triggered before the due date: `TRIGGER;RELATED=END:-PT15M`. */
    fun secondsBeforeDue(alarm: IcsComponent): Long? = alarm.property("TRIGGER")
        ?.takeIf { it.parameter("RELATED")?.value.equals("END", ignoreCase = true) }
        ?.let { IcsDuration.parse(it.value) }
        ?.takeIf { it <= 0 }
        ?.let { -it }

    fun withReminder(todo: IcsComponent, seconds: Long?): IcsComponent {
        val alarms = todo.components("VALARM")
        val index = alarms.indexOfFirst { secondsBeforeDue(it) != null }
        val replacement = seconds?.let {
            IcsComponent(
                "VALARM",
                listOf(
                    IcsProperty("ACTION", value = "DISPLAY"),
                    IcsProperty(
                        "TRIGGER",
                        listOf(IcsParameter("RELATED", "END")),
                        IcsDuration.format(-it)
                    ),
                    IcsProperty("DESCRIPTION", value = "Reminder")
                )
            )
        }
        val updated = alarms.toMutableList()
        when {
            index >= 0 && replacement != null -> updated[index] = replacement
            index >= 0 -> updated.removeAt(index)
            replacement != null -> updated.add(replacement)
        }
        return todo.withComponents("VALARM", updated)
    }
}
