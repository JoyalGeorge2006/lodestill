package dev.joyal.lodestill.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import dev.joyal.lodestill.MainActivity

// Tier 3: while a session is ACTIVE, opening an app from the user's own
// distracting-app list returns the user to Lodestill. Every other app is
// untouched. Enabled only after the in-app disclosure; reads no content.
class BlockAccessibilityService : AccessibilityService() {
    private var lastActionAt = 0L
    private companion object { const val TAG = "BlockSvc" }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null || !BlockState.active) return
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        Log.d(TAG, "window event pkg=$pkg active=${BlockState.active} blocked=${BlockState.blocked.size}")
        if (!dev.joyal.lodestill.core.BlockRules.shouldAct(pkg, packageName,
                BlockState.active, BlockState.blocked, BlockState.IGNORE)) return
        val now = System.currentTimeMillis()
        if (now - lastActionAt < 1500) return
        lastActionAt = now
        Log.d(TAG, "acting on $pkg: back + return to Lodestill")
        performGlobalAction(GLOBAL_ACTION_BACK)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    override fun onInterrupt() {}
}
