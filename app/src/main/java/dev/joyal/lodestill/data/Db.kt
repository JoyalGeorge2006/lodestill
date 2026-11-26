package dev.joyal.lodestill.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY createdAt ASC") fun places(): Flow<List<Place>>
    @Insert suspend fun insert(p: Place): Long
    @Update suspend fun update(p: Place)
    @Query("SELECT * FROM places WHERE id=:id") suspend fun byId(id: Long): Place?
    @Query("DELETE FROM places WHERE id=:id") suspend fun delete(id: Long)
}

@Dao
interface SessionDao {
    @Insert suspend fun insert(s: FocusSession): Long
    @Update suspend fun update(s: FocusSession)
    @Query("SELECT * FROM sessions WHERE id=:id") suspend fun byId(id: Long): FocusSession?
    @Query("SELECT * FROM sessions WHERE status IN ('ACTIVE','PAUSED','INTERRUPTED') ORDER BY startedAt DESC LIMIT 1")
    suspend fun current(): FocusSession?
    @Query("SELECT * FROM sessions WHERE status IN ('ACTIVE','PAUSED','INTERRUPTED') ORDER BY startedAt DESC LIMIT 1")
    fun currentFlow(): Flow<FocusSession?>
    @Query("SELECT * FROM sessions WHERE placeId=:placeId ORDER BY startedAt DESC") fun forPlace(placeId: Long): Flow<List<FocusSession>>
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC") fun all(): Flow<List<FocusSession>>
    @Query("SELECT * FROM sessions WHERE layerState='SCAFFOLDING' ORDER BY scaffoldDeadline ASC")
    fun scaffolding(): Flow<List<FocusSession>>
    @Query("SELECT * FROM sessions WHERE startedAt>=:sinceMs AND status IN ('COMPLETED','ENDED','INTERRUPTED','ACTIVE','PAUSED')")
    fun since(sinceMs: Long): Flow<List<FocusSession>>
}

@Dao
interface NoteDao {
    @Insert suspend fun insert(n: Note): Long
    @Query("SELECT * FROM notes ORDER BY createdAt DESC") fun all(): Flow<List<Note>>
}

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps ORDER BY label ASC") fun all(): Flow<List<BlockedApp>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(a: BlockedApp)
    @Query("DELETE FROM blocked_apps WHERE packageName=:pkg") suspend fun delete(pkg: String)
}

@Database(entities = [Place::class, FocusSession::class, Note::class, BlockedApp::class], version = 1, exportSchema = true)
abstract class HfDb : RoomDatabase() {
    abstract fun places(): PlaceDao
    abstract fun sessions(): SessionDao
    abstract fun notes(): NoteDao
    abstract fun blockedApps(): BlockedAppDao
    companion object {
        @Volatile private var I: HfDb? = null
        fun get(c: Context): HfDb = I ?: synchronized(this) {
            I ?: Room.databaseBuilder(c.applicationContext, HfDb::class.java, "lodestill.db").build().also { I = it }
        }
    }
}
