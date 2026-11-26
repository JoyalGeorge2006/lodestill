package dev.joyal.lodestill

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.joyal.lodestill.data.FocusSession
import dev.joyal.lodestill.data.HfDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Regression: a session that reaches its target by the clock must show "Layer built";
// Done must return to the idle Today screen.
@RunWith(AndroidJUnit4::class)
class CompletionTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
    @Before fun before() = runBlocking { TestReset.all(app) }
    @After fun after() = runBlocking { TestReset.all(app) }

    @Test fun timedCompletionShowsLayerBuiltThenDoneReturnsIdle() {
        runBlocking {
            val db = HfDb.get(app)
            val p = db.places().places().first { it.isNotEmpty() }.first()
            val t = System.currentTimeMillis()
            // 45-minute session, 2 seconds from its target
            db.sessions().insert(FocusSession(placeId = p.id, plannedSec = 2700, targetSec = 2700,
                segmentStart = t - 2_698_000, startedAt = t - 2_698_000, status = "ACTIVE"))
        }
        // approved active-session subline must render while running
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("kept either way", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitUntil(timeoutMillis = 20_000) {
            rule.onAllNodesWithText("Layer built").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("This week").assertIsDisplayed()
        rule.onNodeWithText("Layer built").assertIsDisplayed()
        rule.onNodeWithText("Done").performClick()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("Start a session").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Start a session").assertIsDisplayed()
    }
}
