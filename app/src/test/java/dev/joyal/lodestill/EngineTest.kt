package dev.joyal.lodestill

import dev.joyal.lodestill.core.Engine
import dev.joyal.lodestill.data.FocusSession
import org.junit.Assert.*
import org.junit.Test

private fun sess(
    planned: Long = 2700, target: Long = planned, acc: Long = 0, seg: Long? = 1_000_000L,
    away: Long? = null, status: String = "ACTIVE", layer: String = "NONE", deadline: Long? = null
) = FocusSession(id = 1, placeId = 7, plannedSec = planned, targetSec = target,
    accumulatedSec = acc, segmentStart = seg, awayAt = away, startedAt = 900_000L,
    status = status, layerState = layer, scaffoldDeadline = deadline)

class EngineTest {
    @Test fun `focus accrues only while running`() {
        val s = sess(seg = 1_000_000L)
        assertEquals(60, Engine.focusedSec(s, 1_060_000))
        assertEquals(2640, Engine.remainingSec(s, 1_060_000))
        val p = Engine.pause(s, 1_060_000).s
        assertEquals(60, Engine.focusedSec(p, 1_600_000)) // no accrual while paused
    }

    @Test fun `return within grace keeps session seamless`() {
        val awayAt = 1_100_000L
        val s = sess(seg = 1_000_000L, away = awayAt)
        val r = Engine.onForeground(s, awayAt + 60_000, 90, 24)
        assertEquals("grace-resume", r.event)
        assertNull(r.s.awayAt)
        // focus accrues through grace (user came back in time)
        assertEquals(160, Engine.focusedSec(r.s, awayAt + 60_000))
    }

    @Test fun `past grace interrupts and banks focus up to leaving`() {
        val awayAt = 1_100_000L
        val s = sess(seg = 1_000_000L, away = awayAt)
        val r = Engine.onForeground(s, awayAt + 120_000, 90, 24)
        assertEquals("interrupted", r.event)
        assertEquals("INTERRUPTED", r.s.status)
        assertEquals(100, r.s.accumulatedSec) // 100 s before leaving, not the 120 s with away
        assertEquals("SCAFFOLDING", r.s.layerState)
        assertEquals(awayAt + 24 * 3600_000L, r.s.scaffoldDeadline)
        assertEquals(2600, r.s.targetSec - r.s.accumulatedSec) // 43:20 left to finish
    }

    @Test fun `resume after interruption targets remaining time`() {
        val s = sess(acc = 1320, seg = null, status = "INTERRUPTED", layer = "SCAFFOLDING",
            deadline = 100_000_000L)
        val r = Engine.resume(s, 2_000_000L)
        assertEquals("resumed", r.event)
        assertEquals("ACTIVE", r.s.status)
        assertEquals(1380, r.s.targetSec) // 45 min - 22 min
        assertEquals(1380, Engine.remainingSec(r.s, 2_000_000L))
    }

    @Test fun `reaching target completes and builds layer`() {
        val s = sess(target = 120, acc = 60, seg = 1_000_000L)
        val r = Engine.settle(s, 1_061_000)
        assertEquals("completed", r.event)
        assertEquals("COMPLETED", r.s.status)
        assertEquals("BUILT", r.s.layerState)
    }

    @Test fun `end early saves time and leaves scaffolding`() {
        val s = sess(seg = 1_000_000L)
        val r = Engine.endEarly(s, 2_320_000, 24) // 22 min done
        assertEquals("ended-early", r.event)
        assertEquals(1320, r.s.accumulatedSec)
        assertEquals("SCAFFOLDING", r.s.layerState)
        assertEquals(1380, Engine.finishTarget(r.s))
    }

    @Test fun `expired scaffolding drops but time is kept`() {
        val s = sess(acc = 1320, seg = null, status = "INTERRUPTED", layer = "SCAFFOLDING",
            deadline = 5_000_000L)
        val r = Engine.settle(s, 6_000_000L)
        assertEquals("scaffolding-dropped", r.event)
        assertEquals("DROPPED", r.s.layerState)
        assertEquals(1320, r.s.accumulatedSec) // time stays counted
    }

    @Test fun `pause and unpause preserve target`() {
        val s = sess(target = 2700, acc = 600, seg = 1_000_000L)
        val p = Engine.pause(s, 1_300_000).s // +300 s
        assertEquals(900, p.accumulatedSec)
        val u = Engine.unpause(p, 2_000_000L).s
        assertEquals(1800, Engine.remainingSec(u, 2_000_000L))
    }

    @Test fun `completing a finish-session counts the layer`() {
        // scaffolding of 1380 remaining; finish session completes -> built
        val s = sess(planned = 2700, target = 1380, acc = 1320, seg = 2_000_000L)
        val r = Engine.settle(s, 2_061_000)
        assertEquals("completed", r.event)
        assertEquals("BUILT", r.s.layerState)
    }
}
