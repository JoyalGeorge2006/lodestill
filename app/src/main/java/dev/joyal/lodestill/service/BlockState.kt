package dev.joyal.lodestill.service

// Shared live state between TimerService, the accessibility service and the UI.
// Room stays the source of truth; this is only a cheap in-memory mirror for
// components that cannot suspend-query on every event.
object BlockState {
    @Volatile var active: Boolean = false
    @Volatile var placeName: String = ""
    // Only apps the user explicitly picked as distracting. Guard and Block act
    // on this set and nothing else; an empty set means they act on nothing.
    @Volatile var blocked: Set<String> = emptySet()

    // Escape routes that must always stay reachable, even if somehow selected:
    // calls/dialer, home/launcher, system UI, settings, input methods.
    val IGNORE = listOf("com.android.systemui", "android", "com.android.launcher",
        "com.google.android.apps.nexuslauncher", "com.android.incallui", "com.android.dialer",
        "com.google.android.dialer", "com.android.settings", "com.samsung.android.dialer",
        "com.google.android.inputmethod", "com.android.emergency", "com.android.phone")
}
