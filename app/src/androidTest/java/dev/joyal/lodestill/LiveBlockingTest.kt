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

// Live foreground-switch coverage: real third-party apps are launched on the
// device while a session runs, and the actual Guard overlay (tier 2, UsageStats
// poll) and Block return (tier 3, accessibility callback) are observed through
// UiAutomator. Selected app is acted on; unselected app is untouched; opt-out
// stops the action mid-blocking.
@RunWith(AndroidJUnit4::class)
class LiveBlockingTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val ctx = instr.targetContext
    private val app = ctx.applicationContext as Application
    private val device = UiDevice.getInstance(instr)
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

    @After fun reset() = runBlocking {
        Prefs.setTier(app, 0)
        HfDb.get(app).blockedApps().all().first().forEach { HfDb.get(app).blockedApps().delete(it.packageName) }
        BlockState.active = false; BlockState.blocked = emptySet()
        device.pressHome()
    }

    @Test fun guardOverlayOnSelectedAppOnlyAndOptOutStopsIt() = runBlocking {
        val apps = launchableApps()
        assertTrue("need at least 2 non-exempt launchable apps, found: $apps", apps.size >= 2)
        val selected = apps.first { it.contains("calculator") || it.contains("clock") || true }
        val unselected = apps.first { it != selected }

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
    }

    @Test fun blockReturnsFromSelectedAppOnly() = runBlocking {
        val apps = launchableApps()
        assertTrue("need at least 2 non-exempt launchable apps, found: $apps", apps.size >= 2)
        val selected = apps.first { it.contains("calculator") || it.contains("clock") || true }
        val unselected = apps.first { it != selected }

        val vm = AppViewModel(app)
        val places = withTimeout(10_000) { vm.places.first { it.isNotEmpty() } }
        val db = HfDb.get(app)
        db.blockedApps().insert(BlockedApp(selected, selected, System.currentTimeMillis()))
        Prefs.setTier(app, 3)
        vm.startSession(places.first().id, 25, "block-live")
        withTimeout(10_000) { while (!BlockState.active) delay(100) }

        // unselected app: untouched
        launch(unselected)
        device.wait(Until.hasObject(By.pkg(unselected).depth(0)), 10_000)
        delay(5_000)
        assertTrue("unselected app must stay in foreground",
            device.hasObject(By.pkg(unselected).depth(0)))

        // selected app: accessibility service returns to Lodestill
        launch(selected)
        val back = device.wait(Until.hasObject(By.pkg(own).depth(0)), 20_000)
        assertTrue("Block must return to Lodestill when the selected app opens", back)

        // opt-out during blocking: selected app opens and STAYS
        vm.setTier(1)
        withTimeout(10_000) { while (BlockState.active) delay(100) }
        launch(selected)
        device.wait(Until.hasObject(By.pkg(selected).depth(0)), 10_000)
        delay(5_000)
        assertTrue("after opt-out the selected app must stay open",
            device.hasObject(By.pkg(selected).depth(0)))

        vm.endEarly()
        device.pressHome()
    }
}
