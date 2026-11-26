package dev.joyal.lodestill

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.joyal.lodestill.data.Prefs
import dev.joyal.lodestill.service.BlockState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

// Step 1 of the real process-death check: start a tier-3 session and leave it
// ACTIVE. CI then force-stops the app process, relaunches it, and runs
// ProcessDeathVerifyTest.
@RunWith(AndroidJUnit4::class)
class ProcessDeathSetupTest {
    private val app = InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as Application

    @Test fun startSessionAndLeaveActive() = runBlocking {
        val vm = AppViewModel(app)
        val places = withTimeout(10_000) { vm.places.first { it.isNotEmpty() } }
        Prefs.setTier(app, 3)
        vm.startSession(places.first().id, 25, "process-death")
        withTimeout(10_000) { while (!BlockState.active) delay(100) }
        assertTrue(BlockState.active)
    }
}
