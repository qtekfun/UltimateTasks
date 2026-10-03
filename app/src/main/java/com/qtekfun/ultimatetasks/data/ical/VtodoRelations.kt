// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.ical

/** The parent link of a task (`RELATED-TO`, RFC 5545 §3.8.4.5), next to any other links. */
object VtodoRelations {
    fun parent(todo: IcsComponent): String? = todo.properties
        .filter { it.name.equals("RELATED-TO", true) }
        .firstOrNull { (it.parameter("RELTYPE")?.value ?: "PARENT").equals("PARENT", true) }
        ?.value?.trim()?.takeIf { it.isNotEmpty() }

    /** Replaces the parent link only; sibling and child links are kept. */
    fun withParent(todo: IcsComponent, parentUid: String?): IcsComponent {
        val isParent = { node: IcsNode ->
            node is IcsProperty && node.name.equals("RELATED-TO", true) &&
                (node.parameter("RELTYPE")?.value ?: "PARENT").equals("PARENT", true)
        }
        val index = todo.children.indexOfFirst(isParent)
        val others = todo.children.filterNot(isParent).toMutableList()
        if (parentUid != null) {
            val link =
                IcsProperty("RELATED-TO", listOf(IcsParameter("RELTYPE", "PARENT")), parentUid)
            val firstComponent = others.indexOfFirst { it is IcsComponent }
            val at = if (index >=
                0
            ) {
                index
            } else if (firstComponent < 0) {
                others.size
            } else {
                firstComponent
            }
            others.add(at.coerceAtMost(others.size), link)
        }
        return todo.copy(children = others)
    }
}
