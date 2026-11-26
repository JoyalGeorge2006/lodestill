package dev.joyal.lodestill

import android.app.Application
import dev.joyal.lodestill.data.HfDb
import dev.joyal.lodestill.data.Prefs
import dev.joyal.lodestill.service.BlockState
import dev.joyal.lodestill.service.TimerService
import kotlinx.coroutines.flow.first

// Shared test isolation: end any leftover session, stop the timer service,
// clear mirrors, tiers and picked apps. Called from @Before AND @After so a
// failed test cannot poison the next one.
object TestReset {
    suspend fun all(app: Application) {
        val db = HfDb.get(app)
        Prefs.setTier(app, 0); Prefs.setDnd(app, false)
        db.sessions().current()?.let {
            db.sessions().update(it.copy(status = "ENDED", segmentStart = null,
                endedAt = System.currentTimeMillis()))
        }
        TimerService.stop(app)
        BlockState.active = false; BlockState.blocked = emptySet()
        db.blockedApps().all().first().forEach { db.blockedApps().delete(it.packageName) }
    }
}
