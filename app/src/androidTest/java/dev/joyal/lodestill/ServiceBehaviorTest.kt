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
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

// Real runtime behavior: session lifecycle drives the blocking mirrors, and
// opt-out clears them immediately. Runs on the emulator with permissions
// granted via adb in CI.
@RunWith(AndroidJUnit4::class)
class ServiceBehaviorTest {
    private val app = InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as Application

    @After fun reset() = runBlocking {
        Prefs.setTier(app, 0); Prefs.setDnd(app, false)
        BlockState.active = false; BlockState.blocked = emptySet()
    }

    @Test fun blockMirrorFollowsSessionLifecycleAndOptOut() = runBlocking {
        val vm = AppViewModel(app)
        val places = withTimeout(10_000) { vm.places.first { it.isNotEmpty() } }
        Prefs.setTier(app, 3)
        vm.startSession(places.first().id, 25, "test")
        // TimerService tick (2s at tier>=2) mirrors ACTIVE + blocked set
        withTimeout(10_000) { while (!BlockState.active) delay(100) }
        assertTrue(BlockState.active)
        // opt-out: dropping below tier 3 clears the mirror immediately
        vm.setTier(1)
        withTimeout(5_000) { while (BlockState.active) delay(50) }
        assertFalse(BlockState.active)
        // session end leaves scaffolding, service mirrors cleared
        vm.setTier(3)
        withTimeout(10_000) { while (!BlockState.active) delay(100) }
        vm.endEarly()
        withTimeout(10_000) { while (BlockState.active) delay(100) }
        assertFalse(BlockState.active)
    }

    @Test fun dndWithoutPermissionDoesNotCrash() = runBlocking {
        val vm = AppViewModel(app)
        val places = withTimeout(10_000) { vm.places.first { it.isNotEmpty() } }
        Prefs.setDnd(app, true)
        vm.startSession(places.first().id, 25, null)
        delay(1_500) // would crash the process if setInterruptionFilter ran unauthorized
        vm.endEarly()
    }
}
