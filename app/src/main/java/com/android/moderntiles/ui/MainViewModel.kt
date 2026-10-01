package com.android.moderntiles.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.moderntiles.R
import com.android.moderntiles.data.TilePreferences
import com.android.moderntiles.services.LockTileService
import com.android.moderntiles.services.SoundTileService
import com.android.moderntiles.util.PermissionUtils
import com.android.moderntiles.util.TileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isAccessibilityEnabled: Boolean = false,
    val isDndAccessGranted: Boolean = false,
    val isLockTileEnabled: Boolean = false,
    val isSoundTileEnabled: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = TilePreferences(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Collect tile states from DataStore and combine into UI state
        viewModelScope.launch {
            combine(
                preferences.isLockTileEnabled,
                preferences.isSoundTileEnabled
            ) { lockEnabled, soundEnabled ->
                Pair(lockEnabled, soundEnabled)
            }.collect { (lockEnabled, soundEnabled) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isLockTileEnabled = lockEnabled,
                        isSoundTileEnabled = soundEnabled
                    )
                }
            }
        }

        refreshPermissions()
    }

    /**
     * Refreshes permission statuses (called on launch and on resume from system settings).
     */
    fun refreshPermissions() {
        val context = getApplication<Application>()
        val accessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(context)
        val dndGranted = PermissionUtils.isDndAccessGranted(context)

        _uiState.update { currentState ->
            currentState.copy(
                isAccessibilityEnabled = accessibilityEnabled,
                isDndAccessGranted = dndGranted
            )
        }
    }

    /**
     * Toggles the Lock Screen tile component and persists preference.
     */
    fun toggleLockTile(enabled: Boolean) {
        val context = getApplication<Application>()
        TileManager.setTileEnabled(context, LockTileService::class.java, enabled)
        viewModelScope.launch {
            preferences.setLockTileEnabled(enabled)
        }
    }

    /**
     * Toggles the Sound Profile tile component and persists preference.
     */
    fun toggleSoundTile(enabled: Boolean) {
        val context = getApplication<Application>()
        TileManager.setTileEnabled(context, SoundTileService::class.java, enabled)
        viewModelScope.launch {
            preferences.setSoundTileEnabled(enabled)
        }
    }

    /**
     * Requests the system to pin the Lock Screen tile to Quick Settings.
     */
    fun pinLockTile() {
        val context = getApplication<Application>()
        TileManager.requestPinTile(
            context = context,
            serviceClass = LockTileService::class.java,
            title = context.getString(R.string.tile_lock_label),
            iconResId = R.drawable.ic_tile_lock
        )
        // Ensure preference reflects active state
        viewModelScope.launch {
            preferences.setLockTileEnabled(true)
        }
    }

    /**
     * Requests the system to pin the Sound Profile tile to Quick Settings.
     */
    fun pinSoundTile() {
        val context = getApplication<Application>()
        TileManager.requestPinTile(
            context = context,
            serviceClass = SoundTileService::class.java,
            title = context.getString(R.string.tile_sound_label),
            iconResId = R.drawable.ic_tile_ring
        )
        // Ensure preference reflects active state
        viewModelScope.launch {
            preferences.setSoundTileEnabled(true)
        }
    }
}
