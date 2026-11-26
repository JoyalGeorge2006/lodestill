package dev.joyal.lodestill

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Exhaustive walk: every screen, every visible control is tapped and its effect
// asserted. Any failed step names itself in the final assertion.
@RunWith(AndroidJUnit4::class)
class ButtonsTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val app = instr.targetContext.applicationContext as Application
    private val device by lazy { UiDevice.getInstance(instr) }
    private val failures = mutableListOf<String>()

    @Before fun before() = runBlocking { TestReset.all(app) }
    @After fun after() = runBlocking { TestReset.all(app) }

    private fun step(name: String, f: () -> Unit) {
        try { rule.waitForIdle(); f(); rule.waitForIdle() }
        catch (e: Throwable) { failures.add("$name: ${e.message?.take(120)}") }
    }
    private fun await(t: String) {
        rule.waitUntil(8_000) { rule.onAllNodesWithText(t).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun tap(t: String) {
        await(t)
        runCatching { rule.onAllNodesWithText(t).onFirst().performScrollTo() }
        rule.onAllNodesWithText(t).onFirst().performClick(); rule.waitForIdle()
    }
    private fun shown(t: String) {
        await(t)
        runCatching { rule.onAllNodesWithText(t).onFirst().performScrollTo() }
        rule.onAllNodesWithText(t).onFirst().assertIsDisplayed()
    }
    private fun type(text: String) {
        rule.onAllNodes(hasSetTextAction()).onLast().performTextInput(text); rule.waitForIdle() }

    @Test fun everyControl() {
        step("nav-places") { tap("PLACES"); shown("World".uppercase()) }
        step("add-place-cancel") { tap("+ Add place"); tap("Cancel"); shown("Places") }
        step("add-place") {
            tap("+ Add place"); type("Gym"); tap("Add place"); shown("Gym") }
        step("place-detail-back") { tap("Gym"); shown("RECENT SESSIONS"); tap("\u2039 Back"); shown("Places") }
        step("scaffolding-to-unfinished") {
            tap("TODAY"); shown("Ready to focus?") }
        step("start-sheet-place-length-intent") {
            tap("Start a session")
            rule.onAllNodesWithText("Place").onLast().performClick()
            tap("GYM")
            rule.onAllNodesWithText("Length").onLast().performClick()
            tap("60M")
            rule.onNodeWithText("Intent (optional)").performClick()
            type("button-walk")
            tap("Begin")
            shown("Pause")
        }
        step("custom-length") {
            tap("End early"); tap("Keep focusing")
            shown("Keep screen on")
        }
        step("pause-resume") { tap("Pause"); shown("Resume"); tap("Resume"); shown("Pause") }
        step("keep-screen-on-exit") {
            tap("Keep screen on"); rule.waitForIdle(); Thread.sleep(600)
            rule.onRoot().performClick(); rule.waitForIdle(); shown("Pause") }
        step("end-early-dialog") {
            tap("End early"); shown("Keep focusing")
            rule.onAllNodesWithText("End early").onLast().performClick()
            shown("Continue session") }
        step("interrupted-let-it-rest") { tap("Let it rest"); shown("Ready to focus?") }
        step("unfinished-finish") {
            shown("SCAFFOLDING"); tap("View  \u203A"); shown("Unfinished")
            rule.onAllNodesWithText("Finish  \u203A").onFirst().performClick()
            shown("Pause"); tap("End early")
            rule.onAllNodesWithText("End early").onLast().performClick()
            tap("Let it rest")
        }
        step("notes-save") {
            tap("NOTES"); type("button walk note")
            tap("Save note"); shown("button walk note") }
        step("settings-appearance") {
            tap("SETTINGS"); shown("APPEARANCE"); tap("Light".uppercase()); tap("Dark".uppercase()) }
        step("settings-length") { tap("60M"); }
        step("settings-dnd-switch") {
            await("Do Not Disturb"); rule.onAllNodesWithText("Do Not Disturb").onFirst().performScrollTo()
            rule.onAllNodes(isToggleable()).onFirst().performClick(); rule.waitForIdle()
            rule.onAllNodes(isToggleable()).onFirst().performClick() }
        step("tier-dnd") { tap("DND") }
        step("tier-guard-navigates") { tap("GUARD"); shown("PERMISSIONS"); tap("\u2039 Back") }
        step("tier-block-disclosure-cancel") { tap("BLOCK"); shown("WHAT IT NEVER DOES"); tap("Not now") }
        step("tier-block-disclosure-accept") {
            tap("BLOCK"); tap("I understand, continue"); shown("PERMISSIONS")
            tap("Notifications"); Thread.sleep(1500); device.pressBack(); Thread.sleep(800) }
        step("permissions-back") { tap("\u2039 Back"); shown("PREFERENCES") }
        step("distractions-toggle") {
            tap("SETTINGS"); rule.onNodeWithText("Distracting apps").performScrollTo()
            tap("Distracting apps")
            type("a")
            val boxes = rule.onAllNodes(isToggleable())
            boxes.onFirst().performClick(); boxes.onFirst().performClick()
            tap("\u2039 Back") }
        step("end-state-idle") { tap("TODAY"); shown("Ready to focus?") }
        assertTrue("failed steps:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
