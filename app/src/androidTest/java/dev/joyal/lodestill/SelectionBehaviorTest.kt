package dev.joyal.lodestill

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.joyal.lodestill.core.BlockRules
import dev.joyal.lodestill.service.BlockState
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

// Device-level proof that Guard/Block act ONLY on user-selected apps, using the
// same mirrors and call shape as TimerService (Guard) and
// BlockAccessibilityService (Block): BlockRules.shouldAct(pkg, own,
// BlockState.active, BlockState.blocked, BlockState.IGNORE).
@RunWith(AndroidJUnit4::class)
class SelectionBehaviorTest {
    private val own = "dev.joyal.lodestill.debug"

    @After fun reset() { BlockState.active = false; BlockState.blocked = emptySet() }

    private fun act(pkg: String?) =
        BlockRules.shouldAct(pkg, own, BlockState.active, BlockState.blocked, BlockState.IGNORE)

    @Test fun selectedAppIsActedOnUnselectedIsNot() {
        BlockState.active = true
        BlockState.blocked = setOf("com.example.social", "com.example.video")
        assertTrue(act("com.example.social"))     // user picked it
        assertTrue(act("com.example.video"))      // user picked it
        assertFalse(act("com.example.messages"))  // messaging app, not picked
        assertFalse(act("com.example.maps"))      // anything unpicked
        assertFalse(act(null))                    // no foreground package
    }

    @Test fun noActWithoutActiveSession() {
        BlockState.active = false
        BlockState.blocked = setOf("com.example.social")
        assertFalse(act("com.example.social"))
    }

    @Test fun ownAppAndSystemEscapeAlwaysExempt() {
        BlockState.active = true
        BlockState.blocked = setOf("com.example.social", own, "com.android.systemui")
        assertFalse(act(own))                      // own app never blocked
        assertFalse(act("com.android.systemui"))   // escape allowlist
        assertFalse(act("com.android.launcher3"))  // home always reachable
        assertFalse(act("com.android.settings"))
        assertFalse(act("com.android.phone"))
        assertFalse(act("com.android.emergency"))
    }
}
