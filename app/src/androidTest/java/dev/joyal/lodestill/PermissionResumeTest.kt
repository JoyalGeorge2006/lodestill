package dev.joyal.lodestill

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Real lifecycle: revoke the overlay permission through the system (as the
// Android Settings page would), send the app to the background, bring it back,
// and assert Settings shows the missing-permission row; grant it and repeat.
@RunWith(AndroidJUnit4::class)
class PermissionResumeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val app = instr.targetContext.applicationContext as Application
    private val device by lazy { UiDevice.getInstance(instr) }
    private val pkg get() = instr.targetContext.packageName

    @Before fun before() { runBlocking { TestReset.all(app); dev.joyal.lodestill.data.Prefs.setTier(app, 2) } }
    @After fun after() { device.executeShellCommand("appops set $pkg SYSTEM_ALERT_WINDOW allow")
        runBlocking { TestReset.all(app) } }

    private fun cycle() {
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(500)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.waitForIdle()
    }
    private fun rowShown() = rule.onAllNodesWithText("Needs permission: Display over other apps")
        .fetchSemanticsNodes().isNotEmpty()

    @Test fun settingsRowFollowsRealPermissionChangeOnReturn() {
        rule.onNodeWithText("SETTINGS").performClick(); rule.waitForIdle()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() }
        device.executeShellCommand("appops set $pkg SYSTEM_ALERT_WINDOW allow"); cycle()
        org.junit.Assert.assertFalse("granted: row must be hidden", rowShown())
        device.executeShellCommand("appops set $pkg SYSTEM_ALERT_WINDOW deny"); cycle()
        rule.waitUntil(5_000) { rowShown() }
        org.junit.Assert.assertTrue("revoked: row must appear on return", rowShown())
        device.executeShellCommand("appops set $pkg SYSTEM_ALERT_WINDOW allow"); cycle()
        rule.waitUntil(5_000) { !rowShown() }
        org.junit.Assert.assertFalse("re-granted: row must disappear on return", rowShown())
    }

    @Test fun activeWarningIsVisibleAndOpensAccessibilitySettings() {
        runBlocking {
            dev.joyal.lodestill.data.Prefs.setTier(app, 3)
            val db = dev.joyal.lodestill.data.HfDb.get(app)
            val p = db.places().places().first().first()
            val t = System.currentTimeMillis()
            db.sessions().insert(dev.joyal.lodestill.data.FocusSession(placeId = p.id, plannedSec = 2700,
                targetSec = 2700, accumulatedSec = 0, segmentStart = t - 60_000, startedAt = t - 60_000,
                status = "ACTIVE"))
        }
        val msg = "Block is not active"
        try { rule.waitUntil(8_000) { rule.onAllNodesWithText(msg, substring = true).fetchSemanticsNodes().isNotEmpty() } }
        catch (e: Throwable) {
            val tree = runCatching { rule.onRoot().printToString(60) }.getOrDefault("?")
            throw AssertionError("warning not shown; tier=" + runBlocking { dev.joyal.lodestill.data.Prefs.flow(app).first().tier } +
                " a11y=" + android.provider.Settings.Secure.getString(app.contentResolver, "enabled_accessibility_services") +
                " TREE: " + tree.replace("\n", " | ").take(1500))
        }
        rule.onAllNodesWithText(msg, substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText(msg, substring = true).onFirst().performClick()
        Thread.sleep(2_000)
        org.junit.Assert.assertTrue("tap must open Android Settings, was ${device.currentPackageName}",
            device.currentPackageName.contains("settings"))
        device.pressBack(); Thread.sleep(800)
    }
}
