package dev.joyal.lodestill

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

// Step 2 of the tier-3 Block scenario: end the session and clean up so later
// phases (process death, screenshots) start from a neutral state.
@RunWith(AndroidJUnit4::class)
class BlockScenarioTeardownTest {
    private val app = InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as Application

    @Test fun endScenarioSession() = runBlocking {
        val vm = AppViewModel(app)
        vm.endEarly()
        TestReset.all(app)
        Unit
    }
}
