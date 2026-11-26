package dev.joyal.lodestill.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ds by preferencesDataStore("lodestill")

data class HfSettings(
    val defaultLengthMin: Int = 45,
    val graceSec: Int = 90,
    val finishWindowH: Int = 24,
    val theme: String = "system", // system | light | dark
    val dndEnabled: Boolean = false,
    val tier: Int = 0
)

object Prefs {
    private val LEN = intPreferencesKey("len"); private val GRACE = intPreferencesKey("grace")
    private val WIN = intPreferencesKey("win"); private val THEME = stringPreferencesKey("theme")
    private val DND = booleanPreferencesKey("dnd"); private val TIER = intPreferencesKey("tier")

    fun flow(c: Context): Flow<HfSettings> = c.ds.data.map { p ->
        HfSettings(p[LEN] ?: 45, p[GRACE] ?: 90, p[WIN] ?: 24, p[THEME] ?: "system",
            p[DND] ?: false, p[TIER] ?: 0)
    }
    suspend fun setLen(c: Context, v: Int) = c.ds.edit { it[LEN] = v }
    suspend fun setGrace(c: Context, v: Int) = c.ds.edit { it[GRACE] = v }
    suspend fun setWin(c: Context, v: Int) = c.ds.edit { it[WIN] = v }
    suspend fun setTheme(c: Context, v: String) = c.ds.edit { it[THEME] = v }
    suspend fun setDnd(c: Context, v: Boolean) = c.ds.edit { it[DND] = v }
    suspend fun setTier(c: Context, v: Int) = c.ds.edit { it[TIER] = v }
}
