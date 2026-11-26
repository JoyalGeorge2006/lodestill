package dev.joyal.lodestill

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.joyal.lodestill.data.FocusSession
import dev.joyal.lodestill.data.HfDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// Captures real device renders of every key state so they can be inspected as pixels.
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val inst = InstrumentationRegistry.getInstrumentation()
    private val app = inst.targetContext.applicationContext as Application
    private val dir = File(inst.targetContext.getExternalFilesDir(null), "shots").also { it.mkdirs() }

    @org.junit.After fun after(): Unit = runBlocking { TestReset.all(app); dev.joyal.lodestill.data.Prefs.setTheme(app, "system"); Unit }
    @Before fun before(): Unit = runBlocking { TestReset.all(app); dev.joyal.lodestill.data.Prefs.setTheme(app, "dark"); Unit }

    private fun shot(name: String) {
        rule.waitForIdle(); Thread.sleep(900)
        val bmp: Bitmap = inst.uiAutomation.takeScreenshot()
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun tryStep(name: String, f: () -> Unit) {
        try { f() } catch (e: Throwable) { File(dir, "$name.FAILED.txt").writeText(e.toString()) }
    }
    private fun tap(t: String) { rule.onNodeWithText(t).performClick(); rule.waitForIdle() }
    private fun db() = HfDb.get(app)

    @Test fun capture() {
        tryStep("01-today") { shot("01-today") }
        tryStep("02-start-sheet") { tap("Start a session"); shot("02-start-sheet") }
        tryStep("07-custom-length") {
            rule.onAllNodesWithText("Length").onLast().performClick(); rule.waitForIdle()
            tap("CUSTOM"); shot("07-custom-length")
            tap("Cancel")
        }
        tryStep("03-active") {
            runBlocking {
                val p = db().places().places().first().first()
                val t = System.currentTimeMillis()
                db().sessions().insert(FocusSession(placeId = p.id, plannedSec = 2700, targetSec = 2700,
                    accumulatedSec = 0, segmentStart = t - 773_000, startedAt = t - 773_000, status = "ACTIVE",
                    intent = "Finish chapter 4 notes"))
            }
            Thread.sleep(2500); shot("03-active")
        }
        tryStep("03b-active-block-unavailable") {
            runBlocking { dev.joyal.lodestill.data.Prefs.setTier(app, 3) }
            Thread.sleep(1500); shot("03b-active-block-unavailable")
            runBlocking { dev.joyal.lodestill.data.Prefs.setTier(app, 0) }
            Thread.sleep(800)
        }
        tryStep("17-timer-only") { tap("Keep screen on"); shot("17-timer-only"); rule.onRoot().performClick(); rule.waitForIdle() }
        tryStep("04-interrupted") {
            runBlocking {
                val c = db().sessions().current()!!
                db().sessions().update(c.copy(status = "INTERRUPTED", accumulatedSec = 1320, segmentStart = null,
                    endedAt = System.currentTimeMillis() - 130_000, layerState = "SCAFFOLDING",
                    scaffoldDeadline = System.currentTimeMillis() + 86_000_000))
            }
            Thread.sleep(2500); shot("04-interrupted")
        }
        tryStep("05-complete") {
            runBlocking {
                val c = db().sessions().current()!!
                db().sessions().update(c.copy(status = "ACTIVE", accumulatedSec = 0, targetSec = 2700,
                    segmentStart = System.currentTimeMillis() - 2_698_000, endedAt = null, layerState = "NONE",
                    scaffoldDeadline = null))
            }
            Thread.sleep(6000); shot("05-complete")
            tap("Done")
        }
        tryStep("09-places") { tap("PLACES"); shot("09-places") }
        tryStep("12-notes") { tap("NOTES"); shot("12-notes") }
        tryStep("13-settings") { tap("SETTINGS"); shot("13-settings") }
        tryStep("14-permissions") { rule.onNodeWithText("Blocking and permissions").performScrollTo(); rule.waitForIdle(); tap("Blocking and permissions"); shot("14-permissions"); rule.onNodeWithText("Accessibility service").performScrollTo(); rule.waitForIdle(); shot("14b-permissions-bottom") }
    }
}
