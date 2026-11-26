package dev.joyal.lodestill

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.joyal.lodestill.data.BlockedApp
import dev.joyal.lodestill.data.HfDb
import dev.joyal.lodestill.data.Prefs
import dev.joyal.lodestill.service.BlockState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

// Step 1 of the tier-3 Block scenario. The scenario is driven from the CI
// shell (no UiAutomation anywhere: UiAutomation suppresses accessibility
// services by default, so the Block callback never fires under UiDevice).
// This starts a tier-3 session with the shell-provided "selected" package on
// the blocked list and leaves it ACTIVE; the shell then launches an unselected
// and the selected app and checks the foreground package.
@RunWith(AndroidJUnit4::class)
class BlockScenarioSetupTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val app = instr.targetContext.applicationContext as Application

    @Test fun startTier3SessionWithSelectedApp() = runBlocking {
        TestReset.all(app)
        val selected = instr.arguments.getString("selected")
            ?: "com.google.android.deskclock"
        val vm = AppViewModel(app)
        val places = withTimeout(10_000) { vm.places.first { it.isNotEmpty() } }
        HfDb.get(app).blockedApps().insert(BlockedApp(selected, selected, System.currentTimeMillis()))
        Prefs.setTier(app, 3)
        vm.startSession(places.first().id, 25, "block-scenario")
        withTimeout(10_000) { while (!BlockState.active) delay(100) }
        assertTrue(BlockState.active)
        Unit
    }
}
