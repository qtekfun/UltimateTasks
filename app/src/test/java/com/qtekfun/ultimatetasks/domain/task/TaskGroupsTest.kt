// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.domain.task

import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TaskGroupsTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val now = Instant.parse("2026-10-03T10:00:00Z") // 12:00 in Madrid

    private fun task(uid: String, due: String? = null, list: String = "/a/") = TaskEntity(
        accountId = 1,
        listHref = list,
        href = "$list$uid.ics",
        uid = uid,
        summary = uid,
        due = due
    )

    private fun groups(
        source: TaskSource,
        vararg tasks: TaskEntity,
        order: List<String> = listOf("/a/", "/b/")
    ) = TaskGroups.group(source, tasks.toList(), order, now, zone).map { group ->
        group.key to
            group.tasks.map { it.uid }
    }

    @Test
    fun `today shows overdue days apart and keeps today's passed times in today`() {
        assertEquals(
            listOf(
                GroupKey.Overdue to listOf("old"),
                GroupKey.Day(LocalDate.parse("2026-10-03")) to listOf("morning", "allday")
            ),
            groups(
                TaskSource.Smart(SmartList.TODAY),
                task("old", "2026-10-01"),
                task("morning", "2026-10-03T09:00"),
                task("allday", "2026-10-03")
            )
        )
    }

    @Test
    fun `scheduled has one section per day, overdue first`() {
        assertEquals(
            listOf(
                GroupKey.Overdue to listOf("old", "morning"),
                GroupKey.Day(LocalDate.parse("2026-10-03")) to listOf("allday"),
                GroupKey.Day(LocalDate.parse("2026-10-05")) to listOf("later")
            ),
            groups(
                TaskSource.Smart(SmartList.SCHEDULED),
                task("later", "2026-10-05"),
                task("old", "2026-10-01"),
                task("morning", "2026-10-03T09:00"),
                task("allday", "2026-10-03")
            )
        )
    }

    @Test
    fun `all is grouped by list in the home order, unknown lists last`() {
        assertEquals(
            listOf(
                GroupKey.InList("/a/") to listOf("a"),
                GroupKey.InList("/b/") to listOf("b"),
                GroupKey.InList("/z/") to listOf("z")
            ),
            groups(
                TaskSource.Smart(SmartList.ALL),
                task("z", list = "/z/"),
                task("b", list = "/b/"),
                task("a", list = "/a/")
            )
        )
    }

    @Test
    fun `a list and completed are one section, none when empty`() {
        assertEquals(
            listOf(GroupKey.None to listOf("x")),
            groups(TaskSource.List("/a/"), task("x"))
        )
        assertEquals(
            listOf(GroupKey.None to listOf("x")),
            groups(TaskSource.Smart(SmartList.COMPLETED), task("x"))
        )
        assertEquals(emptyList<Pair<GroupKey, List<String>>>(), groups(TaskSource.List("/a/")))
        assertEquals(
            listOf(GroupKey.None to listOf("undated")),
            groups(TaskSource.Smart(SmartList.TODAY), task("undated"))
        )
    }
}
