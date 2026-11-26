package dev.joyal.lodestill

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.joyal.lodestill.data.BlockedApp
import dev.joyal.lodestill.data.FocusSession
import dev.joyal.lodestill.data.HfDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockedAppDaoTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(ctx, HfDb::class.java).build()

    @After fun close() = db.close()

    @Test fun blockedAppRoundTrip() = runBlocking {
        db.blockedApps().insert(BlockedApp("com.a", "A", 1))
        db.blockedApps().insert(BlockedApp("com.b", "B", 2))
        assertEquals(2, db.blockedApps().all().first().size)
        db.blockedApps().delete("com.a")
        val rest = db.blockedApps().all().first()
        assertEquals(1, rest.size)
        assertEquals("com.b", rest[0].packageName)
    }

    @Test fun sessionQueriesTrackLifecycle() = runBlocking {
        val t = System.currentTimeMillis()
        db.places().insert(dev.joyal.lodestill.data.Place(name = "Study", icon = "book", createdAt = t))
        val pid = db.places().places().first().first().id
        val id = db.sessions().insert(FocusSession(placeId = pid, plannedSec = 1500,
            targetSec = 1500, segmentStart = t, startedAt = t, status = "ACTIVE"))
        assertEquals(id, db.sessions().current()!!.id)
        assertTrue(db.sessions().scaffolding().first().isEmpty())
        val s = db.sessions().byId(id)!!
        db.sessions().update(s.copy(status = "INTERRUPTED", layerState = "SCAFFOLDING",
            accumulatedSec = 600, segmentStart = null, scaffoldDeadline = t + 86_400_000))
        assertEquals(1, db.sessions().scaffolding().first().size)
    }
}
