package dev.joyal.lodestill.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "book",
    val manual: Boolean = false,
    val createdAt: Long
)

// status: ACTIVE, PAUSED, INTERRUPTED, COMPLETED, ENDED
// layerState: NONE, SCAFFOLDING, BUILT, DROPPED
@Entity(tableName = "sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placeId: Long,
    val plannedSec: Long,
    val targetSec: Long,          // plannedSec, or remaining when finishing scaffolding
    val accumulatedSec: Long = 0, // banked focused seconds (up to leaving time)
    val segmentStart: Long? = null, // epoch ms when current focused segment began; null when not running
    val awayAt: Long? = null,     // epoch ms when app left; grace runs from here
    val startedAt: Long,
    val endedAt: Long? = null,
    val status: String = "ACTIVE",
    val reason: String? = null,   // interruption reason tag
    val layerState: String = "NONE",
    val scaffoldDeadline: Long? = null,
    val intent: String? = null,
    val linkedSessionId: Long? = null // scaffolding session this one finishes
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placeId: Long?,
    val sessionId: Long?,
    val text: String,
    val createdAt: Long
)

@Entity(tableName = "blocked_apps", primaryKeys = ["packageName"])
data class BlockedApp(
    val packageName: String,
    val label: String,
    val addedAt: Long
)
