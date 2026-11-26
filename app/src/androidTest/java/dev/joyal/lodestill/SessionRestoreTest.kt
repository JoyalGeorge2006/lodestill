package dev.joyal.lodestill

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.joyal.lodestill.data.Prefs
import dev.joyal.lodestill.service.BlockState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

// Process-death / session restore: a fresh ViewModel against the same DB must
// see the still-active session (TimerService restarted it would rely on this).
@RunWith(AndroidJUnit4::class)
class SessionRestoreTest {
    private val app = InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as Application

    @After fun reset() = runBlocking {
        Prefs.setTier(app, 0)
        BlockState.active = false; BlockState.blocked = emptySet()
    }

    @Test fun activeSessionSurvivesViewModelRecreation() = runBlocking {
        val vm1 = AppViewModel(app)
        val places = withTimeout(10_000) { vm1.places.first { it.isNotEmpty() } }
        vm1.startSession(places.first().id, 25, "restore-check")
        val started = withTimeout(10_000) { vm1.current.first { it != null } }!!
        assertEquals("ACTIVE", started.status)

        // simulate process death: brand-new ViewModel, same app/DB
        val vm2 = AppViewModel(app)
        val restored = withTimeout(10_000) { vm2.current.first { it != null } }!!
        assertEquals(started.id, restored.id)
        assertEquals("ACTIVE", restored.status)

        vm2.endEarly()
        val ended = withTimeout(10_000) { vm2.current.first { it == null || it.status != "ACTIVE" } }
        assertTrue(ended == null || ended!!.status != "ACTIVE")
    }
}
