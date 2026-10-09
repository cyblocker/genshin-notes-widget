package com.genshin.dailynote.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.genshin.dailynote.data.local.ExtrapolationUtils
import com.genshin.dailynote.data.model.DailyNoteData
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "genshin_notes_prefs")

data class UserConfig(
    val uid: String = "",
    val server: String = "os_asia",
    val cookie: String = "",
    val lastSyncTimestamp: Long = 0L,
    val lastSyncError: String? = null,
    val autoExtrapolate: Boolean = true
)

class UserPreferencesRepository(
    private val context: Context,
    private val gson: Gson = Gson()
) {

    companion object {
        val KEY_UID = stringPreferencesKey("genshin_uid")
        val KEY_SERVER = stringPreferencesKey("genshin_server")
        val KEY_COOKIE = stringPreferencesKey("genshin_cookie")
        val KEY_CACHED_DATA_JSON = stringPreferencesKey("cached_daily_note_json")
        val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val KEY_LAST_SYNC_ERROR = stringPreferencesKey("last_sync_error")
        val KEY_AUTO_EXTRAPOLATE = booleanPreferencesKey("auto_extrapolate")
    }

    val userConfigFlow: Flow<UserConfig> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserConfig(
                uid = preferences[KEY_UID] ?: "",
                server = preferences[KEY_SERVER] ?: "os_asia",
                cookie = preferences[KEY_COOKIE] ?: "",
                lastSyncTimestamp = preferences[KEY_LAST_SYNC_TIMESTAMP] ?: 0L,
                lastSyncError = preferences[KEY_LAST_SYNC_ERROR],
                autoExtrapolate = preferences[KEY_AUTO_EXTRAPOLATE] ?: true
            )
        }

    suspend fun getUserConfig(): UserConfig = userConfigFlow.first()

    suspend fun saveCredentials(uid: String, server: String, cookie: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_UID] = uid.trim()
            prefs[KEY_SERVER] = server.trim()
            prefs[KEY_COOKIE] = cookie.trim()
        }
    }

    suspend fun saveCachedNote(data: DailyNoteData, timestamp: Long = System.currentTimeMillis()) {
        val json = gson.toJson(data)
        context.dataStore.edit { prefs ->
            prefs[KEY_CACHED_DATA_JSON] = json
            prefs[KEY_LAST_SYNC_TIMESTAMP] = timestamp
            prefs.remove(KEY_LAST_SYNC_ERROR)
        }
    }

    suspend fun saveSyncError(errorMessage: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_ERROR] = errorMessage
        }
    }

    suspend fun clearSyncError() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_SYNC_ERROR)
        }
    }

    suspend fun getCachedDailyNote(): DailyNoteData? {
        val prefs = context.dataStore.data.first()
        val json = prefs[KEY_CACHED_DATA_JSON] ?: return null
        return try {
            gson.fromJson(json, DailyNoteData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getExtrapolatedNote(): ExtrapolationUtils.ExtrapolatedNote? {
        val prefs = context.dataStore.data.first()
        val json = prefs[KEY_CACHED_DATA_JSON] ?: return null
        val lastSync = prefs[KEY_LAST_SYNC_TIMESTAMP] ?: 0L
        val autoExtrapolate = prefs[KEY_AUTO_EXTRAPOLATE] ?: true

        val rawData = try {
            gson.fromJson(json, DailyNoteData::class.java)
        } catch (e: Exception) {
            null
        } ?: return null

        return if (autoExtrapolate) {
            ExtrapolationUtils.extrapolate(rawData, lastSync)
        } else {
            ExtrapolationUtils.extrapolate(rawData, lastSync, currentTimeMillis = lastSync)
        }
    }
}
