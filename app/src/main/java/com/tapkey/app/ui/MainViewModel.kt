package com.tapkey.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tapkey.app.R
import com.tapkey.app.data.TilePreferences
import com.tapkey.app.services.AccessibilityToggleTileService
import com.tapkey.app.services.LockTileService
import com.tapkey.app.services.VolumeTileService
import com.tapkey.app.util.PermissionUtils
import com.tapkey.app.util.TileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isAccessibilityEnabled: Boolean = false,
    val hasWriteSecureSettings: Boolean = false,
    val isLockTileEnabled: Boolean = false,
    val isVolumeTileEnabled: Boolean = false,
    val isAccessibilityTileEnabled: Boolean = false,
    val showAccessibilityBottomSheet: Boolean = false,
    val showWriteSecureSettingsBottomSheet: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = TilePreferences(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Collect tile states from DataStore
        viewModelScope.launch {
            combine(
                preferences.isLockTileEnabled,
                preferences.isVolumeTileEnabled,
                preferences.isAccessibilityTileEnabled
            ) { lockEnabled, volumeEnabled, accessibilityTileEnabled ->
                Triple(lockEnabled, volumeEnabled, accessibilityTileEnabled)
            }.collect { (lockEnabled, volumeEnabled, accessibilityTileEnabled) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isLockTileEnabled = lockEnabled,
                        isVolumeTileEnabled = volumeEnabled,
                        isAccessibilityTileEnabled = accessibilityTileEnabled
                    )
                }
            }
        }

        refreshPermissions()
    }

    /**
     * Refreshes accessibility and WRITE_SECURE_SETTINGS permission status.
     */
    fun refreshPermissions() {
        val context = getApplication<Application>()
        val accessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(context)
        val writeSecureSettingsGranted = PermissionUtils.hasWriteSecureSettings(context)

        _uiState.update { currentState ->
            currentState.copy(
                isAccessibilityEnabled = accessibilityEnabled,
                hasWriteSecureSettings = writeSecureSettingsGranted,
                showAccessibilityBottomSheet = if (accessibilityEnabled) false else currentState.showAccessibilityBottomSheet,
                showWriteSecureSettingsBottomSheet = if (writeSecureSettingsGranted) false else currentState.showWriteSecureSettingsBottomSheet
            )
        }
    }

    /**
     * Toggles the Lock Screen tile. Prompts the bottom sheet if permission is missing.
     */
    fun onLockTileToggleRequested(targetEnabled: Boolean) {
        val context = getApplication<Application>()
        val hasPermission = PermissionUtils.isAccessibilityServiceEnabled(context)

        if (targetEnabled && !hasPermission) {
            _uiState.update { it.copy(showAccessibilityBottomSheet = true) }
        } else {
            _uiState.update { it.copy(isLockTileEnabled = targetEnabled) }
            viewModelScope.launch(Dispatchers.IO) {
                TileManager.setTileEnabled(context, LockTileService::class.java, targetEnabled)
                preferences.setLockTileEnabled(targetEnabled)
            }
        }
    }

    /**
     * Pins the Lock Screen tile. Prompts the bottom sheet if permission is missing.
     */
    fun onLockTilePinRequested() {
        val context = getApplication<Application>()
        val hasPermission = PermissionUtils.isAccessibilityServiceEnabled(context)

        if (!hasPermission) {
            _uiState.update { it.copy(showAccessibilityBottomSheet = true) }
        } else {
            _uiState.update { it.copy(isLockTileEnabled = true) }
            viewModelScope.launch(Dispatchers.IO) {
                TileManager.requestPinTile(
                    context = context,
                    serviceClass = LockTileService::class.java,
                    title = context.getString(R.string.tile_lock_label),
                    iconResId = R.drawable.ic_tile_lock
                )
                preferences.setLockTileEnabled(true)
            }
        }
    }

    fun dismissAccessibilityBottomSheet() {
        _uiState.update { it.copy(showAccessibilityBottomSheet = false) }
    }

    /**
     * Toggles the Accessibility toggle tile. Prompts the ADB setup sheet if WRITE_SECURE_SETTINGS is missing.
     */
    fun onAccessibilityTileToggleRequested(targetEnabled: Boolean) {
        val context = getApplication<Application>()
        val hasPermission = PermissionUtils.hasWriteSecureSettings(context)

        if (targetEnabled && !hasPermission) {
            _uiState.update { it.copy(showWriteSecureSettingsBottomSheet = true) }
        } else {
            _uiState.update { it.copy(isAccessibilityTileEnabled = targetEnabled) }
            viewModelScope.launch(Dispatchers.IO) {
                TileManager.setTileEnabled(context, AccessibilityToggleTileService::class.java, targetEnabled)
                preferences.setAccessibilityTileEnabled(targetEnabled)
            }
        }
    }

    /**
     * Pins the Accessibility toggle tile to Quick Settings. Prompts setup sheet if permission is missing.
     */
    fun onAccessibilityTilePinRequested() {
        val context = getApplication<Application>()
        val hasPermission = PermissionUtils.hasWriteSecureSettings(context)

        if (!hasPermission) {
            _uiState.update { it.copy(showWriteSecureSettingsBottomSheet = true) }
        } else {
            _uiState.update { it.copy(isAccessibilityTileEnabled = true) }
            viewModelScope.launch(Dispatchers.IO) {
                TileManager.requestPinTile(
                    context = context,
                    serviceClass = AccessibilityToggleTileService::class.java,
                    title = context.getString(R.string.tile_accessibility_label),
                    iconResId = R.drawable.ic_tile_accessibility
                )
                preferences.setAccessibilityTileEnabled(true)
            }
        }
    }

    fun openWriteSecureSettingsBottomSheet() {
        _uiState.update { it.copy(showWriteSecureSettingsBottomSheet = true) }
    }

    fun dismissWriteSecureSettingsBottomSheet() {
        _uiState.update { it.copy(showWriteSecureSettingsBottomSheet = false) }
    }

    /**
     * Toggles the Volume Panel tile component.
     */
    fun toggleVolumeTile(enabled: Boolean) {
        val context = getApplication<Application>()
        _uiState.update { it.copy(isVolumeTileEnabled = enabled) }
        viewModelScope.launch(Dispatchers.IO) {
            TileManager.setTileEnabled(context, VolumeTileService::class.java, enabled)
            preferences.setVolumeTileEnabled(enabled)
        }
    }

    /**
     * Pins the Volume Panel tile to Quick Settings.
     */
    fun pinVolumeTile() {
        val context = getApplication<Application>()
        _uiState.update { it.copy(isVolumeTileEnabled = true) }
        viewModelScope.launch(Dispatchers.IO) {
            TileManager.requestPinTile(
                context = context,
                serviceClass = VolumeTileService::class.java,
                title = context.getString(R.string.tile_volume_label),
                iconResId = R.drawable.ic_tile_volume
            )
            preferences.setVolumeTileEnabled(true)
        }
    }
}
