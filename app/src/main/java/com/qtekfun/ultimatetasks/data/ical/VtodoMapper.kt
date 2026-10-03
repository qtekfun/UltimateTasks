// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.ical

import java.time.Instant

/**
 * Reads [VtodoFields] from a `VCALENDAR` and writes them back into it. Writing compares each
 * field with what the file already says and only rewrites the properties that changed, so the
 * rest of the file, including what the app does not understand, stays byte for byte.
 */
object VtodoMapper {
    const val PRODUCT_ID = "-//UltimateTasks//UltimateTasks//EN"

    /** The first `VTODO` of [calendar], or null when it has none. */
    fun read(calendar: IcsComponent): VtodoFields? =
        calendar.components("VTODO").firstOrNull()?.let(::fields)

    fun fields(todo: IcsComponent): VtodoFields {
        val status = todo.property("STATUS")?.value?.trim()
        return VtodoFields(
            uid = todo.property("UID")?.value.orEmpty(),
            summary = todo.property("SUMMARY")?.value?.let(IcsText::unescape).orEmpty(),
            notes = todo.property("DESCRIPTION")?.value?.let(IcsText::unescape).orEmpty(),
            url = todo.property("URL")?.value?.takeIf { it.isNotBlank() },
            due = todo.property("DUE")?.let(IcsDate::from),
            start = todo.property("DTSTART")?.let(IcsDate::from),
            completed = status.equals("COMPLETED", ignoreCase = true) ||
                (status == null && todo.property("COMPLETED") != null),
            completedAt = IcsDate.parseUtc(todo.property("COMPLETED")),
            priority =
                todo.property("PRIORITY")?.value?.trim()?.toIntOrNull()?.takeIf {
                    it in
                        0..MAX_PRIORITY
                }
                    ?: 0,
            tags = todo.properties.filter {
                it.name.equals("CATEGORIES", true)
            }.flatMap { IcsText.splitList(it.value) },
            parentUid = VtodoRelations.parent(todo),
            sortOrder = todo.property("X-APPLE-SORT-ORDER")?.value?.trim()?.toLongOrNull(),
            recurrence = todo.property("RRULE")?.value?.trim()?.takeIf { it.isNotEmpty() },
            reminderBefore = todo.components(
                "VALARM"
            ).firstNotNullOfOrNull(VtodoAlarms::secondsBeforeDue),
            attachments = VtodoAttachments.read(todo),
            modifiedAt = IcsDate.parseUtc(todo.property("LAST-MODIFIED"))
        )
    }

    /**
     * [base] with its `VTODO` updated to [fields], or a new calendar when [base] is null.
     * LAST-MODIFIED and DTSTAMP move to [now] and SEQUENCE grows when anything changed.
     */
    fun write(base: IcsComponent?, fields: VtodoFields, now: Instant): IcsComponent {
        val calendar = base ?: IcsComponent(
            "VCALENDAR",
            listOf(IcsProperty("VERSION", value = "2.0"), IcsProperty("PRODID", value = PRODUCT_ID))
        )
        val old = calendar.components("VTODO").firstOrNull()
        val start = old ?: IcsComponent(
            "VTODO",
            listOf(
                IcsProperty("UID", value = fields.uid),
                IcsProperty("CREATED", value = IcsDate.utc(now)),
                IcsProperty("DTSTAMP", value = IcsDate.utc(now))
            )
        )
        val before = old?.let(::fields)
        val updated = apply(start, before, fields)
        if (old != null && updated == old) return calendar
        val stamped = stamp(updated, isNew = old == null, now = now)
        val todos = calendar.components("VTODO")
        val withTodo = calendar.withComponents("VTODO", listOf(stamped) + todos.drop(1))
        return withTimeZones(withTodo, listOfNotNull(fields.due?.zone, fields.start?.zone))
    }

    private fun apply(todo: IcsComponent, before: VtodoFields?, after: VtodoFields): IcsComponent {
        var result = todo
        fun <T> field(get: (VtodoFields) -> T, change: (IcsComponent, T) -> IcsComponent) {
            val value = get(after)
            if (before == null || get(before) != value) result = change(result, value)
        }
        field(VtodoFields::summary) { c, v -> c.withProperty("SUMMARY", text("SUMMARY", v)) }
        field(VtodoFields::notes) { c, v -> c.withProperty("DESCRIPTION", text("DESCRIPTION", v)) }
        field(VtodoFields::url) { c, v ->
            c.withProperty("URL", v?.let { IcsProperty("URL", value = it) })
        }
        field(VtodoFields::due) { c, v ->
            c.withProperty("DUE", v?.let { IcsDate.property("DUE", it) })
        }
        field(VtodoFields::start) { c, v ->
            c.withProperty("DTSTART", v?.let { IcsDate.property("DTSTART", it) })
        }
        field({ it.completed to it.completedAt }) { c, _ -> completion(c, after) }
        field(VtodoFields::priority) { c, v ->
            c.withProperty(
                "PRIORITY",
                v.takeIf {
                    it > 0
                }?.let { IcsProperty("PRIORITY", value = "$it") }
            )
        }
        field(VtodoFields::tags) { c, v ->
            c.withProperty(
                "CATEGORIES",
                v.takeIf { it.isNotEmpty() }?.let { tags ->
                    IcsProperty("CATEGORIES", value = tags.joinToString(",") { IcsText.escape(it) })
                }
            )
        }
        field(VtodoFields::parentUid) { c, v -> VtodoRelations.withParent(c, v) }
        field(VtodoFields::sortOrder) { c, v ->
            c.withProperty(
                "X-APPLE-SORT-ORDER",
                v?.let {
                    IcsProperty("X-APPLE-SORT-ORDER", value = "$it")
                }
            )
        }
        field(VtodoFields::recurrence) { c, v ->
            c.withProperty("RRULE", v?.let { IcsProperty("RRULE", value = it) })
        }
        field(VtodoFields::reminderBefore) { c, v -> VtodoAlarms.withReminder(c, v) }
        field(VtodoFields::attachments) { c, v -> VtodoAttachments.write(c, v) }
        return result
    }

    private fun text(name: String, value: String) =
        value.takeIf { it.isNotEmpty() }?.let { IcsProperty(name, value = IcsText.escape(it)) }

    /** STATUS, COMPLETED and PERCENT-COMPLETE move together. */
    private fun completion(todo: IcsComponent, fields: VtodoFields): IcsComponent =
        if (fields.completed) {
            todo.withProperty("STATUS", IcsProperty("STATUS", value = "COMPLETED"))
                .withProperty(
                    "COMPLETED",
                    fields.completedAt?.let { IcsProperty("COMPLETED", value = IcsDate.utc(it)) }
                )
                .withProperty("PERCENT-COMPLETE", IcsProperty("PERCENT-COMPLETE", value = "100"))
        } else {
            todo.withProperty("STATUS", IcsProperty("STATUS", value = "NEEDS-ACTION"))
                .withProperty("COMPLETED", null)
                .withProperty("PERCENT-COMPLETE", null)
        }

    private fun stamp(todo: IcsComponent, isNew: Boolean, now: Instant): IcsComponent {
        val stamped = todo.withProperty(
            "LAST-MODIFIED",
            IcsProperty("LAST-MODIFIED", value = IcsDate.utc(now))
        )
            .withProperty("DTSTAMP", IcsProperty("DTSTAMP", value = IcsDate.utc(now)))
        if (isNew) return stamped
        val sequence = todo.property("SEQUENCE")?.value?.trim()?.toIntOrNull() ?: 0
        return stamped.withProperty("SEQUENCE", IcsProperty("SEQUENCE", value = "${sequence + 1}"))
    }

    /** Adds the VTIMEZONE of every zone in use that the file does not define yet. */
    private fun withTimeZones(calendar: IcsComponent, zones: List<String>): IcsComponent {
        val defined = calendar.components("VTIMEZONE").mapNotNull {
            it.property("TZID")?.value
        }.toSet()
        val missing = zones.filter {
            it != IcsDate.UTC && it !in defined
        }.distinct().mapNotNull(VTimeZones::of)
        if (missing.isEmpty()) return calendar
        // Time zones go before the components that use them.
        val firstComponent = calendar.children.indexOfFirst { it is IcsComponent }
        val at = if (firstComponent < 0) calendar.children.size else firstComponent
        return calendar.copy(
            children = calendar.children.toMutableList().apply {
                addAll(at, missing)
            }
        )
    }

    private const val MAX_PRIORITY = 9
}
