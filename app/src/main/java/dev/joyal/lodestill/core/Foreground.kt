package dev.joyal.lodestill.core

/** Resolves the true foreground package from an ordered usage-event stream. */
object Foreground {
    enum class Kind { FOREGROUND, BACKGROUND, OTHER }
    fun resolve(events: List<Pair<Kind, String>>): String? {
        var pkg: String? = null
        for ((k, p) in events) when (k) {
            Kind.FOREGROUND -> pkg = p
            Kind.BACKGROUND -> if (p == pkg) pkg = null
            Kind.OTHER -> {}
        }
        return pkg
    }
}
