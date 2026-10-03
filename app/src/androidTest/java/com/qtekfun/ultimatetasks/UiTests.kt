// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qtekfun.ultimatetasks.data.local.UltimateTasksDatabase
import com.qtekfun.ultimatetasks.data.local.entity.AccountEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskEntity
import com.qtekfun.ultimatetasks.data.local.entity.TaskListEntity
import com.qtekfun.ultimatetasks.data.settings.OnboardingPrefs
import com.qtekfun.ultimatetasks.sync.queue.QueuedOperation
import com.qtekfun.ultimatetasks.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val ACCOUNT = 1L
private const val WORK = "/remote.php/dav/calendars/ana/work/"

/**
 * Key flows on a real device (T29, SPEC §7): login, completing with undo, adding in the list and
 * Today. The server is never reached: tasks are seeded in an in-memory database and every change
 * must land in Room and in the sync queue.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UiTests {
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
        hilt.inject()
        val preferences = context.getSharedPreferences(UI_TEST_SETTINGS, Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        // The wizard would ask for notifications with a system dialog the test cannot answer.
        OnboardingPrefs(preferences).markWizardShown()
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun text(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun waitForText(text: String) = compose.waitUntil(TIMEOUT) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    /** A signed-in account with a "Work" list and [tasks], as after a sync. */
    private fun seed(vararg tasks: Pair<String, String?>) = runBlocking {
        database.accountDao().insert(
            AccountEntity(ACCOUNT, "https://cloud.example/", "ana", "Ana")
        )
        database.taskListDao().upsert(
            listOf(TaskListEntity(accountId = ACCOUNT, href = WORK, name = "Work"))
        )
        tasks.forEachIndexed { index, (summary, due) ->
            database.taskDao().insert(
                TaskEntity(
                    accountId = ACCOUNT,
                    listHref = WORK,
                    href = "${WORK}t$index.ics",
                    uid = "t$index",
                    summary = summary,
                    due = due
                )
            )
        }
    }

    private fun task(summary: String) = runBlocking {
        database.taskDao().inList(ACCOUNT, WORK).firstOrNull { it.summary == summary }
    }

    private fun queued() = runBlocking {
        database.pendingOperationDao().all(ACCOUNT).map { QueuedOperation.decode(it.payload) }
    }

    private fun openWork() {
        waitForText("Work")
        compose.onNodeWithText("Work").performClick()
    }

    @Test
    fun loginRefusesAnUnencryptedServer() {
        launch()
        waitForText(text(R.string.login_button))

        compose.onNode(hasSetTextAction()).performTextInput("http://cloud.example")
        compose.onNodeWithText(text(R.string.login_button)).performClick()

        waitForText(text(R.string.login_error_insecure))
    }

    @Test
    fun completingATaskCanBeUndone() {
        seed("Buy milk" to null)
        launch()
        openWork()
        waitForText("Buy milk")

        compose.onNodeWithText("Buy milk").performCustomAccessibilityActionWithLabel(
            text(R.string.task_mark_done, "Buy milk")
        )
        compose.waitUntil(TIMEOUT) { task("Buy milk")?.completed == true }
        // The change is saved first, then queued.
        compose.waitUntil(TIMEOUT) { queued() == listOf(QueuedOperation.UpdateTask) }

        compose.onNodeWithText(text(R.string.undo)).performClick()
        compose.waitUntil(TIMEOUT) { task("Buy milk")?.completed == false }
    }

    @Test
    fun aTaskTypedInTheListIsSavedAndQueued() {
        seed()
        launch()
        openWork()
        compose.onNodeWithText(text(R.string.new_task)).performClick()

        compose.onNode(hasSetTextAction()).performTextInput("Call Ana")
        compose.onNode(hasSetTextAction()).performImeAction()

        compose.waitUntil(TIMEOUT) { task("Call Ana") != null }
        // The change is saved first, then queued.
        compose.waitUntil(TIMEOUT) { queued() == listOf(QueuedOperation.CreateTask) }
    }

    @Test
    fun todayShowsOnlyWhatIsDueToday() {
        val today = LocalDate.now()
        seed("Due today" to today.toString(), "Due tomorrow" to today.plusDays(1).toString())
        launch()
        waitForText(text(R.string.smart_today))
        compose.onNodeWithText(text(R.string.smart_today)).performClick()

        waitForText("Due today")
        assertTrue(compose.onAllNodesWithText("Due tomorrow").fetchSemanticsNodes().isEmpty())
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
