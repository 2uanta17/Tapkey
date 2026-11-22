package com.tapkey.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tapkey_preferences")

class TilePreferences(private val context: Context) {

    companion object {
        val KEY_LOCK_TILE_ENABLED = booleanPreferencesKey("lock_tile_enabled")
        val KEY_VOLUME_TILE_ENABLED = booleanPreferencesKey("volume_tile_enabled")
        val KEY_ACCESSIBILITY_TILE_ENABLED = booleanPreferencesKey("accessibility_tile_enabled")
        val KEY_PAUSED_ACCESSIBILITY_SERVICES = stringSetPreferencesKey("paused_accessibility_services")
    }

    private fun <T> observePreference(transform: (Preferences) -> T): Flow<T> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map(transform)
    }

    val isLockTileEnabled: Flow<Boolean> = observePreference { preferences ->
        preferences[KEY_LOCK_TILE_ENABLED] ?: false
    }

    val isVolumeTileEnabled: Flow<Boolean> = observePreference { preferences ->
        preferences[KEY_VOLUME_TILE_ENABLED] ?: false
    }

    val isAccessibilityTileEnabled: Flow<Boolean> = observePreference { preferences ->
        preferences[KEY_ACCESSIBILITY_TILE_ENABLED] ?: false
    }

    val pausedAccessibilityServices: Flow<Set<String>> = observePreference { preferences ->
        preferences[KEY_PAUSED_ACCESSIBILITY_SERVICES] ?: emptySet()
    }

    suspend fun setLockTileEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LOCK_TILE_ENABLED] = enabled
        }
    }

    suspend fun setVolumeTileEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VOLUME_TILE_ENABLED] = enabled
        }
    }

    suspend fun setAccessibilityTileEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCESSIBILITY_TILE_ENABLED] = enabled
        }
    }

    suspend fun getPausedAccessibilityServices(): Set<String> {
        return pausedAccessibilityServices.first()
    }

    suspend fun setPausedAccessibilityServices(services: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PAUSED_ACCESSIBILITY_SERVICES] = services
        }
    }

    suspend fun clearPausedAccessibilityServices() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_PAUSED_ACCESSIBILITY_SERVICES)
        }
    }
}
