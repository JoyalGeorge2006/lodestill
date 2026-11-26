package dev.joyal.lodestill

import android.app.Application
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dev.joyal.lodestill.data.BlockedApp
import dev.joyal.lodestill.data.HfDb
import dev.joyal.lodestill.data.Prefs
import dev.joyal.lodestill.service.BlockState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

// Live Guard (tier 2, UsageStats poll) coverage: real third-party apps are
// launched while a session runs and the overlay is observed through UiAutomator.
// The tier-3 Block return path is verified by BlockScenarioSetup/TeardownTest,
// driven from the shell without UiAutomation, which suppresses accessibility
// services by default (UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).
@RunWith(AndroidJUnit4::class)
class LiveBlockingTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val ctx = instr.targetContext
    private val app = ctx.applicationContext as Application
    private val device by lazy { UiDevice.getInstance(instr) }
    private val own = ctx.packageName

    private fun launchableApps(): List<String> {
        val pm = ctx.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(i, 0).map { it.activityInfo.packageName }.distinct()
            .filter { p -> p != own && !p.startsWith("dev.joyal") &&
                BlockState.IGNORE.none { p.startsWith(it) } }.sorted()
    }

    private fun launch(pkg: String) {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }

    @org.junit.Before fun before() = runBlocking { TestReset.all(app) }

    @After fun reset() = runBlocking {
        TestReset.all(app)
        device.pressHome()
        Unit
    }

    private fun pickTwo(): Pair<String, String> {
        val apps = launchableApps()
        assertTrue("need at least 2 non-exempt launchable apps, found: $apps", apps.size >= 2)
        // Prefer small stable apps (no first-run flows like Chrome's).
        val preferred = apps.filter { it.contains("calculator") || it.contains("deskclock") || it.contains("clock") }
        val selected = preferred.firstOrNull() ?: apps.first()
        val unselected = (preferred.drop(1) + apps).first { it != selected }
        return selected to unselected
    }

    @Test fun guardOverlayOnSelectedAppOnlyAndOptOutStopsIt() = runBlocking {
        val (selected, unselected) = pickTwo()

        val vm = AppViewModel(app)
        val places = withTimeout(10_000) { vm.places.first { it.isNotEmpty() } }
        val db = HfDb.get(app)
        db.blockedApps().insert(BlockedApp(selected, selected, System.currentTimeMillis()))
        Prefs.setTier(app, 2)
        vm.startSession(places.first().id, 25, "guard-live")
        withTimeout(10_000) { while (BlockState.blocked.isEmpty()) delay(100) }

        // unselected app: no overlay
        launch(unselected)
        device.wait(Until.hasObject(By.pkg(unselected).depth(0)), 10_000)
        delay(6_000) // several 2s guard ticks
        assertNull("overlay must not appear over an unselected app",
            device.findObject(By.textContains("tap to return")))

        // selected app: Guard overlay appears
        launch(selected)
        device.wait(Until.hasObject(By.pkg(selected).depth(0)), 10_000)
        val shown = device.wait(Until.hasObject(By.textContains("tap to return")), 20_000)
        assertTrue("Guard overlay must appear over the selected app", shown)

        // opt-out mid-blocking: tier drops to 1, overlay must disappear
        vm.setTier(1)
        val gone = device.wait(Until.gone(By.textContains("tap to return")), 15_000)
        assertTrue("overlay must disappear after opt-out", gone)

        vm.endEarly()
        device.pressHome()
        Unit
    }

    }
