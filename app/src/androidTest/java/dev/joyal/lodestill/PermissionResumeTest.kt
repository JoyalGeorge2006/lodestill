package dev.joyal.lodestill

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
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

    @Before fun before() = runBlocking { TestReset.all(app); dev.joyal.lodestill.data.Prefs.setTier(app, 2) }
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
}
