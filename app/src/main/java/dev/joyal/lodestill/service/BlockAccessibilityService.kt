package dev.joyal.lodestill.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

// Tier 3: while a session is ACTIVE, opening an app from the user's own
// distracting-app list shows the Lodestill blocked screen. Every other app is
// untouched. Enabled only after the in-app disclosure; reads no content.
class BlockAccessibilityService : AccessibilityService() {
    private var lastActionAt = 0L
    private companion object { const val TAG = "BlockSvc" }
    override fun onServiceConnected() {
        super.onServiceConnected()
        BlockState.connected = true
        Log.d(TAG, "connected pid=${android.os.Process.myPid()} proc=${android.app.Application.getProcessName()} pkg=$packageName")
    }
    override fun onUnbind(intent: Intent?): Boolean {
        BlockState.connected = false
        Log.d(TAG, "unbound: service disabled or app force-stopped")
        return super.onUnbind(intent)
    }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        val pid = android.os.Process.myPid()
        if (e == null) { Log.d(TAG, "null event pid=$pid"); return }
        val pkg = e.packageName?.toString() ?: "?"
        Log.d(TAG, "event type=${e.eventType} pkg=$pkg active=${BlockState.active} blocked=${BlockState.blocked.size} pid=$pid")
        if (!BlockState.active) return
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (pkg == packageName) return
        if (!dev.joyal.lodestill.core.BlockRules.shouldAct(pkg, packageName,
                BlockState.active, BlockState.blocked, BlockState.IGNORE)) return
        val now = System.currentTimeMillis()
        if (now - lastActionAt < 1500) return
        lastActionAt = now
        Log.d(TAG, "acting on $pkg: blocked screen")
        // No BACK action: it is delivered late and would dismiss the blocked screen itself.
        startActivity(dev.joyal.lodestill.BlockedActivity.intent(this, pkg))
    }
    override fun onInterrupt() { Log.d(TAG, "interrupted") }
}
