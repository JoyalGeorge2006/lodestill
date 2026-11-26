package dev.joyal.lodestill.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import dev.joyal.lodestill.MainActivity
import dev.joyal.lodestill.core.Engine
import dev.joyal.lodestill.data.HfDb
import dev.joyal.lodestill.data.Prefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// Foreground timer + tier-2 guard. Start/end are stored in Room, so process
// death or battery saver cannot corrupt the timer; the notification, overlay
// and BlockState mirror are just that: mirrors.
class TimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var sessionId: Long = -1
    private var overlay: TextView? = null

    private val tick = object : Runnable {
        override fun run() {
            val db = HfDb.get(this@TimerService)
            // Sticky restart after process death delivers no intent; fall back
            // to the current session row before giving up.
            val s = runBlocking {
                db.sessions().byId(sessionId) ?: db.sessions().current()
            }?.also { sessionId = it.id }
            if (s == null || (s.status != "ACTIVE" && s.status != "PAUSED")) {
                BlockState.active = false; BlockState.blocked = emptySet(); hideOverlay(); stopSelf(); return
            }
            val now = System.currentTimeMillis()
            val settings = runBlocking { Prefs.flow(this@TimerService).first() }
            BlockState.active = s.status == "ACTIVE" && settings.tier >= 3
            if (s.status == "ACTIVE") {
                BlockState.placeName = runBlocking { db.places().byId(s.placeId)?.name ?: "Focus" }
                if (settings.tier >= 2) BlockState.blocked = runBlocking {
                    db.blockedApps().all().first() }.map { it.packageName }.toSet()
                if (Engine.remainingSec(s, now) <= 0) { notifyDone(); stopSelf(); return }
                if (settings.tier == 2) guardCheck() else hideOverlay()
                handler.postDelayed(this, if (settings.tier >= 2) 2_000 else 15_000)
            } else {
                hideOverlay()
                handler.postDelayed(this, 15_000)
            }
            startForeground(1, buildNotif("Focusing · ${Engine.fmtRemaining(Engine.remainingSec(s, now))} left"))
        }
    }

    // Tier 2: usage access + overlay. When another app comes to front during an
    // active session, float a return banner over it.
    private fun guardCheck() {
        val usm = getSystemService(UsageStatsManager::class.java) ?: return
        val now = System.currentTimeMillis()
        // Walk the event stream: the foreground app is the last one that moved
        // to foreground and has not since moved to background. This catches an
        // app already open when the session started, and ignores stale history
        // (an app that was used and then left is not foreground).
        val events = usm.queryEvents(now - 2 * 60 * 60_000L, now)
        val e = UsageEvents.Event()
        val list = mutableListOf<Pair<dev.joyal.lodestill.core.Foreground.Kind, String>>()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val k = when (e.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> dev.joyal.lodestill.core.Foreground.Kind.FOREGROUND
                UsageEvents.Event.MOVE_TO_BACKGROUND -> dev.joyal.lodestill.core.Foreground.Kind.BACKGROUND
                else -> dev.joyal.lodestill.core.Foreground.Kind.OTHER
            }
            list.add(k to e.packageName)
        }
        val pkg = dev.joyal.lodestill.core.Foreground.resolve(list)
        if (pkg == null) { hideOverlay(); return }
        if (pkg == packageName) { hideOverlay(); return }
        // guardCheck only runs while a session is ACTIVE, so active=true here.
        if (!dev.joyal.lodestill.core.BlockRules.shouldAct(pkg, packageName,
                true, BlockState.blocked, BlockState.IGNORE)) return
        showOverlay()
    }

    private fun showOverlay() {
        if (overlay != null || !Settings.canDrawOverlays(this)) return
        val wm = getSystemService(WindowManager::class.java)
        val tv = TextView(this).apply {
            text = "Focus session running · tap to return"
            setBackgroundColor(0xFF1B1C1E.toInt()); setTextColor(0xFFFFFFFF.toInt())
            setPadding(32, 24, 32, 24); textSize = 14f
            setOnClickListener {
                startActivity(Intent(this@TimerService, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT)
        lp.gravity = Gravity.TOP
        runCatching { wm.addView(tv, lp); overlay = tv }
    }
    private fun hideOverlay() {
        overlay?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        overlay = null
    }

    override fun onStartCommand(i: Intent?, f: Int, st: Int): Int {
        sessionId = i?.getLongExtra("session", -1) ?: -1
        // Foreground must be declared within a few seconds of
        // startForegroundService(), before any database work in the tick.
        startForeground(1, buildNotif("Focusing"))
        handler.removeCallbacks(tick); handler.post(tick)
        return START_STICKY
    }
    private fun buildNotif(text: String): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "session")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Lodestill").setContentText(text).setContentIntent(pi)
            .setOngoing(true).build()
    }
    private fun notifyDone() {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, "alerts")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Session complete").setContentText("Your layer is built.")
            .setContentIntent(pi).setAutoCancel(true).build()
        getSystemService(android.app.NotificationManager::class.java).notify(2, n)
    }
    override fun onDestroy() { handler.removeCallbacks(tick); hideOverlay(); BlockState.active = false; BlockState.blocked = emptySet(); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null
    companion object {
        fun start(c: Context, session: Long) =
            c.startForegroundService(Intent(c, TimerService::class.java).putExtra("session", session))
        fun stop(c: Context) = c.stopService(Intent(c, TimerService::class.java))
    }
}
