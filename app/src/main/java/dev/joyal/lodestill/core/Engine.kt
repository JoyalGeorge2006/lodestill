package dev.joyal.lodestill.core

import dev.joyal.lodestill.data.FocusSession

// Pure session state machine. All times in epoch ms / seconds. Fully unit-testable.
object Engine {

    data class Result(val s: FocusSession, val event: String)

    fun focusedSec(s: FocusSession, nowMs: Long): Long {
        val seg = if (s.segmentStart != null && s.awayAt == null) (nowMs - s.segmentStart) / 1000 else 0L
        return s.accumulatedSec + seg
    }

    fun remainingSec(s: FocusSession, nowMs: Long): Long =
        (s.targetSec - focusedSec(s, nowMs)).coerceAtLeast(0)

    fun progress(s: FocusSession, nowMs: Long): Float =
        if (s.targetSec <= 0) 0f else focusedSec(s, nowMs).toFloat() / s.targetSec

    // App goes to background while ACTIVE: grace clock starts, focus keeps accruing until grace ends.
    fun onBackground(s: FocusSession, nowMs: Long): FocusSession =
        if (s.status == "ACTIVE" && s.awayAt == null) s.copy(awayAt = nowMs) else s

    // App returns. Within grace: seamless. Past grace: interruption; bank focus up to the moment of leaving.
    fun onForeground(s: FocusSession, nowMs: Long, graceSec: Int, windowH: Int): Result {
        val away = s.awayAt ?: return Result(s, "none")
        return if (nowMs - away <= graceSec * 1000L) {
            Result(s.copy(awayAt = null), "grace-resume")
        } else interrupt(s, away, windowH)
    }

    fun interrupt(s: FocusSession, atMs: Long, windowH: Int): Result {
        val banked = s.accumulatedSec + ((atMs - (s.segmentStart ?: atMs)).coerceAtLeast(0) / 1000)
        val rem = (s.targetSec - banked).coerceAtLeast(0)
        val out = s.copy(
            accumulatedSec = banked, segmentStart = null, awayAt = null,
            status = "INTERRUPTED", endedAt = atMs,
            layerState = if (rem > 0) "SCAFFOLDING" else "BUILT",
            scaffoldDeadline = if (rem > 0) atMs + windowH * 3600_000L else null
        )
        return Result(out, "interrupted")
    }

    // User resumes an interrupted/scaffolding session: new target is the remaining time.
    fun resume(s: FocusSession, nowMs: Long): Result =
        Result(s.copy(status = "ACTIVE", endedAt = null, segmentStart = nowMs,
            targetSec = remainingSec(s, nowMs).let { if (it == 0L) s.targetSec else it },
            accumulatedSec = 0), "resumed")

    fun pause(s: FocusSession, nowMs: Long): Result {
        if (s.status != "ACTIVE") return Result(s, "none")
        val banked = s.accumulatedSec + ((nowMs - (s.segmentStart ?: nowMs)).coerceAtLeast(0) / 1000)
        return Result(s.copy(status = "PAUSED", accumulatedSec = banked, segmentStart = null, awayAt = null), "paused")
    }

    fun unpause(s: FocusSession, nowMs: Long): Result =
        if (s.status == "PAUSED") Result(s.copy(status = "ACTIVE", segmentStart = nowMs), "resumed")
        else Result(s, "none")

    fun endEarly(s: FocusSession, nowMs: Long, windowH: Int): Result {
        val banked = s.accumulatedSec + ((nowMs - (s.segmentStart ?: nowMs)).coerceAtLeast(0) / 1000)
        val rem = (s.targetSec - banked).coerceAtLeast(0)
        val out = s.copy(
            status = "ENDED", endedAt = nowMs, accumulatedSec = banked,
            segmentStart = null, awayAt = null,
            layerState = if (rem > 0) "SCAFFOLDING" else "BUILT",
            scaffoldDeadline = if (rem > 0) nowMs + windowH * 3600_000L else null
        )
        return Result(out, "ended-early")
    }

    // Called on ticks/reads: completes when target reached, drops expired scaffolding.
    fun settle(s: FocusSession, nowMs: Long): Result {
        if ((s.status == "ACTIVE" || s.status == "PAUSED") && remainingSec(s, nowMs) <= 0) {
            return Result(s.copy(status = "COMPLETED", endedAt = nowMs,
                accumulatedSec = s.targetSec, segmentStart = null, awayAt = null,
                layerState = "BUILT", scaffoldDeadline = null), "completed")
        }
        if (s.layerState == "SCAFFOLDING" && s.scaffoldDeadline != null && nowMs > s.scaffoldDeadline) {
            return Result(s.copy(layerState = "DROPPED", scaffoldDeadline = null), "scaffolding-dropped")
        }
        return Result(s, "none")
    }

    // Finishing scaffolding: link a fresh session whose target is the scaffolding remainder.
    fun finishTarget(scaffold: FocusSession): Long = (scaffold.targetSec - scaffold.accumulatedSec).coerceAtLeast(0)

    fun fmtRemaining(sec: Long): String = "%d:%02d".format(sec / 60, sec % 60)
}
