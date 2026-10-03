// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.settings.OnboardingPrefs
import com.qtekfun.ultimatetasks.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Store and README screenshots with made-up tasks, never anyone's real data. Skipped unless
 * asked for, since it only writes images:
 * `adb shell am instrument -w -e screenshots true -e class com.qtekfun.ultimatetasks.Screenshots
 * com.qtekfun.ultimatetasks.test/com.qtekfun.ultimatetasks.HiltTestRunner`, then pull them from
 * the app's files folder, `screenshots/`.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class Screenshots {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject
    lateinit var database: UltimateTasksDatabase

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        val arguments = androidx.test.platform.app.InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("screenshots") == "true")
        hilt.inject()
        val preferences = context.getSharedPreferences(UI_TEST_SETTINGS, Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        OnboardingPrefs(preferences).markWizardShown()
        seed()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun takeScreenshots() {
        waitFor(context.getString(R.string.smart_today))
        save("1")
        compose.onNodeWithText(listName(GROCERIES)).performClick()
        waitFor(t("Oat milk", "Leche de avena"))
        save("2")
        scenario?.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor(listName(PERSONAL))
        compose.onNodeWithText(listName(PERSONAL)).performClick()
        waitFor(t("Book the dentist", "Pedir cita al dentista"))
        compose.onNodeWithText(t("Book the dentist", "Pedir cita al dentista")).performClick()
        waitFor(context.getString(R.string.field_repeat))
        save("3")
    }

    private fun waitFor(text: String) {
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun save(name: String) {
        val folder = File(context.filesDir, "screenshots").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, QUALITY, it)
        }
    }

    private fun seed() = runBlocking {
        val today = LocalDate.now()
        val account = database.accountDao().insert(
            AccountEntity(serverUrl = "https://cloud.example/", userId = "ana", displayName = "Ana")
        )
        val lists = listOf(
            Triple(PERSONAL, "#007AFF", 0),
            Triple(GROCERIES, "#34C759", 1),
            Triple(WORK, "#FF9500", 2),
            Triple(TRAVEL, "#AF52DE", 3)
        )
        database.taskListDao().upsert(
            lists.map { (key, color, order) ->
                TaskListEntity(
                    accountId = account,
                    href = "/lists/$key/",
                    name = listName(key),
                    color = color,
                    sortOrder = order
                )
            }
        )
        var next = 0
        suspend fun task(list: String, summary: String, edit: (TaskEntity) -> TaskEntity = { it }) {
            next++
            database.taskDao().insert(
                edit(
                    TaskEntity(
                        accountId = account,
                        listHref = "/lists/$list/",
                        href = "/lists/$list/$next.ics",
                        uid = "demo-$next",
                        summary = summary
                    )
                )
            )
        }
        task(GROCERIES, t("Oat milk", "Leche de avena"))
        task(GROCERIES, t("Bread", "Pan")) {
            it.copy(notes = t("Whole wheat, sliced", "Integral, de molde"))
        }
        task(GROCERIES, t("Coffee beans", "Café en grano")) { it.copy(priority = 5) }
        task(GROCERIES, t("Apples", "Manzanas"))
        task(GROCERIES, t("Olive oil", "Aceite de oliva")) { it.copy(completed = true) }
        task(PERSONAL, t("Book the dentist", "Pedir cita al dentista")) {
            it.copy(
                due = LocalDateTime.of(thirdFriday(today), LocalTime.of(17, 30)).toString(),
                notes = t("Ask about the cleaning", "Preguntar por la limpieza"),
                recurrence = "FREQ=MONTHLY;BYDAY=3FR"
            )
        }
        task(PERSONAL, t("Call grandma", "Llamar a la abuela")) {
            it.copy(due = today.toString(), priority = 1)
        }
        task(PERSONAL, t("Water the plants", "Regar las plantas")) {
            it.copy(due = today.plusDays(2).toString(), recurrence = "FREQ=WEEKLY")
        }
        task(WORK, t("Prepare the slides", "Preparar las diapositivas")) {
            it.copy(due = today.plusDays(1).toString())
        }
        task(WORK, t("Review the budget", "Revisar el presupuesto")) {
            it.copy(tags = listOf(t("finance", "finanzas")))
        }
        task(WORK, t("Send the weekly report", "Enviar el informe semanal")) {
            it.copy(due = today.minusDays(1).toString())
        }
        task(TRAVEL, t("Renew passport", "Renovar el pasaporte")) {
            it.copy(due = today.plusDays(14).toString())
        }
        task(TRAVEL, t("Pack the charger", "Meter el cargador"))
    }

    private val spanish = context.resources.configuration.locales[0].language == "es"

    /** Demo text in the language of the screenshots. */
    private fun t(english: String, spanish: String) = if (this.spanish) spanish else english

    private fun listName(key: String) = when (key) {
        GROCERIES -> t("Groceries", "Compra")
        WORK -> t("Work", "Trabajo")
        TRAVEL -> t("Travel", "Viaje")
        else -> "Personal"
    }

    /** The next third Friday of a month, today included, so the date matches its repetition. */
    private fun thirdFriday(today: LocalDate): LocalDate {
        val third = today.with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.FRIDAY))
        return if (third.isBefore(today)) {
            today.plusMonths(1).with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.FRIDAY))
        } else {
            third
        }
    }

    private companion object {
        const val PERSONAL = "personal"
        const val GROCERIES = "groceries"
        const val WORK = "work"
        const val TRAVEL = "travel"
        const val TIMEOUT = 5_000L
        const val QUALITY = 100
    }
}
