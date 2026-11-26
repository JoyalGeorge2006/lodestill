package dev.joyal.lodestill

import dev.joyal.lodestill.core.BlockRules
import org.junit.Assert.*
import org.junit.Test

class BlockRulesTest {
    private val own = "dev.joyal.lodestill.debug"
    private val ignore = listOf("com.android.systemui", "android", "com.android.launcher",
        "com.android.incallui", "com.android.dialer", "com.android.settings", "com.android.phone")
    private val blocked = setOf("com.instagram.android", "com.google.android.youtube")

    @Test fun `selected app acts while session active`() {
        assertTrue(BlockRules.shouldAct("com.instagram.android", own, true, blocked, ignore))
    }
    @Test fun `unselected app never acts`() {
        assertFalse(BlockRules.shouldAct("com.whatsapp", own, true, blocked, ignore))
    }
    @Test fun `nothing acts when no session is active`() {
        assertFalse(BlockRules.shouldAct("com.instagram.android", own, false, blocked, ignore))
    }
    @Test fun `own app never acts`() {
        assertFalse(BlockRules.shouldAct(own, own, true, blocked, ignore))
    }
    @Test fun `null package never acts`() {
        assertFalse(BlockRules.shouldAct(null, own, true, blocked, ignore))
    }
    @Test fun `calls and dialer escape even if selected`() {
        val evil = blocked + "com.android.incallui"
        assertFalse(BlockRules.shouldAct("com.android.incallui", own, true, evil, ignore))
        assertFalse(BlockRules.shouldAct("com.google.android.dialer", own, true, blocked, ignore))
    }
    @Test fun `system ui and launcher escape even if selected`() {
        val evil = blocked + "com.android.systemui" + "com.android.launcher3"
        assertFalse(BlockRules.shouldAct("com.android.systemui", own, true, evil, ignore))
        // launcher3 starts with the launcher prefix on the allowlist
        val ig = ignore + "com.android.launcher3"
        assertFalse(BlockRules.shouldAct("com.android.launcher3", own, true, evil, ig))
    }
    @Test fun `settings always escapes`() {
        assertFalse(BlockRules.shouldAct("com.android.settings", own, true, blocked + "com.android.settings", ignore))
    }
    @Test fun `empty blocked list acts on nothing`() {
        assertFalse(BlockRules.shouldAct("com.instagram.android", own, true, emptySet(), ignore))
    }
    @Test fun `prefixed system packages are caught by allowlist`() {
        assertFalse(BlockRules.shouldAct("com.android.systemui.pip", own, true, blocked + "com.android.systemui.pip", ignore))
    }
}
