package com.android.moderntiles.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tile_preferences")

class TilePreferences(private val context: Context) {

    companion object {
        val KEY_LOCK_TILE_ENABLED = booleanPreferencesKey("lock_tile_enabled")
        val KEY_SOUND_TILE_ENABLED = booleanPreferencesKey("sound_tile_enabled")
    }

    val isLockTileEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_LOCK_TILE_ENABLED] ?: false
        }

    val isSoundTileEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_SOUND_TILE_ENABLED] ?: false
        }

    suspend fun setLockTileEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LOCK_TILE_ENABLED] = enabled
        }
    }

    suspend fun setSoundTileEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SOUND_TILE_ENABLED] = enabled
        }
    }
}
