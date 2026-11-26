package dev.joyal.lodestill

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.joyal.lodestill.core.Engine
import dev.joyal.lodestill.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = HfDb.get(app)
    val places = db.places().places().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val notes = db.notes().all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val scaffolding = db.sessions().scaffolding().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allSessions = db.sessions().all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings = Prefs.flow(app).stateIn(viewModelScope, SharingStarted.Eagerly, HfSettings())

    private val _current = MutableStateFlow<FocusSession?>(null)
    val current: StateFlow<FocusSession?> = _current
    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now
    // Keep-screen-on timer mode (opt-in, in-app only; not the system always-on display)
    private val _timerOnly = MutableStateFlow(false)
    val timerOnly: StateFlow<Boolean> = _timerOnly
    fun setTimerOnly(v: Boolean) { _timerOnly.value = v }
    private var ticker: Job? = null
    private var serviceStartedFor: Long = -1

    init {
        viewModelScope.launch {
            db.sessions().currentFlow().collect { s ->
                var cur = s
                if (cur != null) {
                    val r = Engine.settle(cur, System.currentTimeMillis())
                    if (r.event != "none") { db.sessions().update(r.s); cur = r.s }
                }
                _current.value = cur
                if (cur != null && (cur.status == "ACTIVE" || cur.status == "PAUSED")) {
                    startTicker()
                    // Restore the foreground timer after process death or app
                    // relaunch; the Room row is the source of truth.
                    if (serviceStartedFor != cur.id) {
                        serviceStartedFor = cur.id
                        dev.joyal.lodestill.service.TimerService.start(getApplication(), cur.id)
                    }
                } else stopTicker()
            }
        }
        // seed default places on first run
        viewModelScope.launch {
            places.first().let { if (it.isEmpty()) {
                val t = System.currentTimeMillis()
                db.places().insert(Place(name = "Study", icon = "book", createdAt = t))
                db.places().insert(Place(name = "Training", icon = "dumb", createdAt = t))
                db.places().insert(Place(name = "Studio", icon = "brush", createdAt = t))
            } }
        }
    }

    private fun startTicker() {
        if (ticker != null) return
        ticker = viewModelScope.launch {
            while (true) {
                _now.value = System.currentTimeMillis()
                val s = _current.value
                if (s != null && s.status == "ACTIVE") {
                    val r = Engine.settle(s, _now.value)
                    if (r.event == "completed") {
                        db.sessions().update(r.s)
                        setDnd(false)
                        dev.joyal.lodestill.service.TimerService.stop(getApplication())
                    }
                }
                delay(1000)
            }
        }
    }
    private fun stopTicker() { ticker?.cancel(); ticker = null }

    fun onBackground() {
        val s = _current.value ?: return
        viewModelScope.launch { db.sessions().update(Engine.onBackground(s, System.currentTimeMillis())) }
    }
    fun onForeground() {
        val s = _current.value ?: return
        viewModelScope.launch {
            val r = Engine.onForeground(s, System.currentTimeMillis(), settings.value.graceSec, settings.value.finishWindowH)
            if (r.event != "none") {
                db.sessions().update(r.s)
                if (r.event == "interrupted") setDnd(false)
            }
        }
    }

    fun startSession(placeId: Long, minutes: Int, intentText: String?, finishOf: FocusSession? = null) {
        viewModelScope.launch {
            val t = System.currentTimeMillis()
            val target = finishOf?.let { Engine.finishTarget(it) } ?: (minutes * 60L)
            val id = db.sessions().insert(FocusSession(
                placeId = placeId, plannedSec = minutes * 60L, targetSec = target,
                segmentStart = t, startedAt = t, status = "ACTIVE",
                intent = intentText?.ifBlank { null }, linkedSessionId = finishOf?.id))
            if (settings.value.dndEnabled) setDnd(true)
            dev.joyal.lodestill.service.TimerService.start(getApplication(), id)
        }
    }
    fun pause() = act { s, t -> Engine.pause(s, t) }
    fun unpause() = act { s, t -> Engine.unpause(s, t) }
    fun resumeInterrupted() = act { s, t -> Engine.resume(s, t) }
    fun endEarly() = act(endService = true, dndOff = true) { s, t -> Engine.endEarly(s, t, settings.value.finishWindowH) }
    fun setReason(reason: String) {
        val s = _current.value ?: return
        viewModelScope.launch { db.sessions().update(s.copy(reason = reason)) }
    }
    private fun act(endService: Boolean = false, dndOff: Boolean = false, f: (FocusSession, Long) -> Engine.Result) {
        val s = _current.value ?: return
        viewModelScope.launch {
            val r = f(s, System.currentTimeMillis())
            if (r.event != "none") db.sessions().update(r.s)
            if (endService) dev.joyal.lodestill.service.TimerService.stop(getApplication())
            if (dndOff) setDnd(false)
        }
    }

    fun addPlace(name: String, icon: String) = viewModelScope.launch {
        db.places().insert(Place(name = name.trim(), icon = icon, createdAt = System.currentTimeMillis()))
    }
    fun addNote(text: String, placeId: Long?, sessionId: Long?) = viewModelScope.launch {
        if (text.isNotBlank()) db.notes().insert(Note(placeId = placeId, sessionId = sessionId,
            text = text.trim(), createdAt = System.currentTimeMillis()))
    }

    fun setTheme(v: String) = viewModelScope.launch { Prefs.setTheme(getApplication(), v) }
    fun setTier(v: Int) = viewModelScope.launch {
        Prefs.setTier(getApplication(), v)
        // Opt-out takes effect immediately, not on the next service tick.
        if (v < 3) dev.joyal.lodestill.service.BlockState.active = false
    }

    val blockedApps = db.blockedApps().all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun toggleBlocked(pkg: String, label: String) = viewModelScope.launch {
        if (blockedApps.value.any { it.packageName == pkg }) db.blockedApps().delete(pkg)
        else db.blockedApps().insert(BlockedApp(pkg, label, System.currentTimeMillis()))
    }
    fun installedLaunchableApps(): List<Pair<String, String>> {
        val pm = getApplication<Application>().packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        return pm.queryIntentActivities(i, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != getApplication<Application>().packageName }
            .distinctBy { it.first }.sortedBy { it.second.lowercase() }
    }
    fun setLen(v: Int) = viewModelScope.launch { Prefs.setLen(getApplication(), v) }
    fun setDndEnabled(v: Boolean) = viewModelScope.launch { Prefs.setDnd(getApplication(), v) }

    fun setDnd(on: Boolean) {
        val nm = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) return
        nm.setInterruptionFilter(if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY
            else NotificationManager.INTERRUPTION_FILTER_ALL)
    }
    fun hasDndAccess(): Boolean {
        val nm = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }
    fun dndSettingsIntent() = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun hasUsageAccess(): Boolean {
        val appOps = getApplication<Application>().getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), getApplication<Application>().packageName)
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
    fun hasOverlay() = android.provider.Settings.canDrawOverlays(getApplication())
    fun usageSettingsIntent() = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
    fun overlaySettingsIntent() = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        android.net.Uri.parse("package:" + getApplication<Application>().packageName))
    fun accessibilitySettingsIntent() = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
    fun isAccessibilityOn(): Boolean {
        val cr = getApplication<Application>().contentResolver
        val enabled = android.provider.Settings.Secure.getString(cr,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.contains("dev.joyal.lodestill", ignoreCase = true)
    }

    // aggregates
    fun layersBuilt(placeId: Long, sessions: List<FocusSession>) =
        sessions.count { it.placeId == placeId && it.layerState == "BUILT" }
    fun totalFocusSec(placeId: Long, sessions: List<FocusSession>) =
        sessions.filter { it.placeId == placeId }.sumOf { it.accumulatedSec }
    fun distinctDays(placeId: Long, sessions: List<FocusSession>) =
        sessions.filter { it.placeId == placeId }.map { dayKey(it.startedAt) }.toSet().size
    fun todayFocusSec(sessions: List<FocusSession>): Long {
        val today = dayKey(System.currentTimeMillis())
        return sessions.filter { dayKey(it.startedAt) == today }.sumOf { it.accumulatedSec }
    }
    fun placeSessionCounts(placeId: Long, sessions: List<FocusSession>, sinceMs: Long): Long =
        sessions.filter { it.placeId == placeId && it.startedAt >= sinceMs }.sumOf { it.accumulatedSec }
    companion object {
        fun dayKey(ms: Long): String {
            val d = java.util.Calendar.getInstance().apply { timeInMillis = ms }
            return "%04d-%02d-%02d".format(d.get(java.util.Calendar.YEAR), d.get(java.util.Calendar.MONTH) + 1, d.get(java.util.Calendar.DAY_OF_MONTH))
        }
    }
}
