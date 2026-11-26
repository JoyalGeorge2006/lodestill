package dev.joyal.lodestill.core

// Pure gate shared by Guard (tier 2) and Block (tier 3). Acts only on apps the
// user explicitly picked, never on the escape-route allowlist, never when no
// session is active.
object BlockRules {
    fun shouldAct(pkg: String?, ownPackage: String, active: Boolean,
                  blocked: Set<String>, ignore: List<String>): Boolean {
        if (!active || pkg == null) return false
        if (pkg == ownPackage) return false
        if (ignore.any { pkg.startsWith(it) }) return false
        return pkg in blocked
    }
}
