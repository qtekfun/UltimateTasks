// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.data.ical

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/** Every file of the corpus survives reading and writing, and an edit touches only its line. */
class IcsRoundTripTest {
    private val corpus: List<File> =
        File(requireNotNull(javaClass.getResource("/ics-corpus")).toURI())
            .walk()
            .filter { it.isFile && it.extension == "ics" }
            .sortedBy { it.path }
            .toList()

    @TestFactory
    fun `files are written back byte for byte`() = corpus.flatMap { file ->
        val text = file.readText()
        listOf(text, text.replace("\r\n", "\n").replace("\n", "\r\n")).mapIndexed { i, variant ->
            DynamicTest.dynamicTest("${file.parentFile?.name}/${file.name} #$i") {
                val written = IcsWriter.write(IcsParser.parse(variant))
                // Components left open in the file are closed; nothing else may change.
                assertEquals(variant, written.take(variant.length))
                assertTrue(written.drop(variant.length).matches(Regex("(END:[A-Z]+\r\n)*")))
            }
        }
    }

    @TestFactory
    fun `changing the summary leaves every other line untouched`() = corpus.map { file ->
        DynamicTest.dynamicTest("${file.parentFile?.name}/${file.name}") {
            val text = file.readText()
            val calendar = IcsParser.parse(text).single()
            val todo = calendar.components("VTODO").firstOrNull() ?: return@dynamicTest
            val edited = calendar.withComponents(
                "VTODO",
                calendar.components("VTODO").map {
                    if (it === todo) {
                        it.withProperty("SUMMARY", IcsProperty("SUMMARY", value = "Edited"))
                    } else {
                        it
                    }
                }
            )
            val before = lines(text).filterNot { it.startsWith("SUMMARY") }
            val after = lines(IcsWriter.write(edited))
            assertTrue("SUMMARY:Edited" in after)
            assertEquals(before, after.filterNot { it == "SUMMARY:Edited" }.take(before.size))
        }
    }

    /** Unfolded lines without their line breaks. */
    private fun lines(text: String) = text.replace("\r\n", "\n").replace("\n ", "")
        .replace("\n\t", "").split("\n").filter { it.isNotEmpty() }
}
