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

// Step 2: after CI force-stopped and relaunched the app, the ACTIVE session
// must still be there and the foreground timer must be running again (mirror
// comes back only when the real TimerService tick reads the session).
@RunWith(AndroidJUnit4::class)
class ProcessDeathVerifyTest {
    private val app = InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as Application

    @After fun reset() = runBlocking {
        Prefs.setTier(app, 0)
        BlockState.active = false; BlockState.blocked = emptySet()
    }

    @Test fun sessionAndServiceSurviveProcessKill() = runBlocking {
        // Mirror proof FIRST: the relaunched app's own ViewModel must already
        // have restarted TimerService (this test's VM must not be the restorer).
        withTimeout(20_000) { while (!BlockState.active) delay(200) }
        val vm = AppViewModel(app)
        val s = withTimeout(10_000) { vm.current.first { it != null } }!!
        assertEquals("ACTIVE", s.status)
        assertEquals("process-death", s.intent)
        vm.endEarly()
    }
}
