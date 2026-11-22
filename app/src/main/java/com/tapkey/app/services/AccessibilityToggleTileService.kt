package com.tapkey.app.services

import android.content.Intent
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tapkey.app.MainActivity
import com.tapkey.app.R
import com.tapkey.app.data.TilePreferences
import com.tapkey.app.util.PermissionUtils
import com.tapkey.app.util.TileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Quick Settings tile that disables all active Accessibility Services in one tap
 * and restores that exact set of services on the next tap.
 */
class AccessibilityToggleTileService : TileService() {

    private val preferences by lazy { TilePreferences(applicationContext) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val toggleMutex = Mutex()
    private var settingsObserver: ContentObserver? = null

    override fun onStartListening() {
        super.onStartListening()
        registerAccessibilityObserver()
        updateTileState()
    }

    override fun onStopListening() {
        unregisterAccessibilityObserver()
        super.onStopListening()
    }

    override fun onDestroy() {
        unregisterAccessibilityObserver()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()

        if (!PermissionUtils.hasWriteSecureSettings(this)) {
            openSetupSheetInApp()
            return
        }

        serviceScope.launch {
            if (!toggleMutex.tryLock()) return@launch
            try {
                val activeServices = withContext(Dispatchers.IO) {
                    PermissionUtils.getEnabledAccessibilityServices(this@AccessibilityToggleTileService)
                }

                if (activeServices.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val existingPaused = preferences.getPausedAccessibilityServices()
                        val mergedSnapshot = PermissionUtils.mergeServicesForPause(
                            currentlyActive = activeServices,
                            previouslyPaused = existingPaused
                        )
                        preferences.setPausedAccessibilityServices(mergedSnapshot)
                        PermissionUtils.disableAllAccessibilityServices(this@AccessibilityToggleTileService)
                    }
                    updateTileState(isActiveOverride = false)
                } else {
                    val pausedServices = withContext(Dispatchers.IO) {
                        preferences.getPausedAccessibilityServices()
                    }
                    if (pausedServices.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            PermissionUtils.restoreAccessibilityServices(
                                this@AccessibilityToggleTileService,
                                pausedServices
                            )
                            preferences.clearPausedAccessibilityServices()
                        }
                        updateTileState()
                    } else {
                        TileManager.startActivityAndCollapseCompat(
                            this@AccessibilityToggleTileService,
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        )
                    }
                }
            } finally {
                toggleMutex.unlock()
            }
        }
    }

    private fun updateTileState(isActiveOverride: Boolean? = null) {
        qsTile?.let { tile ->
            val isActive = isActiveOverride ?: PermissionUtils.hasAnyActiveAccessibilityService(this)
            tile.label = getString(R.string.tile_accessibility_label)
            tile.subtitle = null // Clean single-line appearance
            tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_accessibility)
            tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    private fun openSetupSheetInApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_WRITE_SECURE_SETTINGS_SHEET, true)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        TileManager.startActivityAndCollapseCompat(this, intent)
    }

    private fun registerAccessibilityObserver() {
        if (settingsObserver != null) return
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                updateTileState()
            }
        }
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            observer
        )
        settingsObserver = observer
    }

    private fun unregisterAccessibilityObserver() {
        settingsObserver?.let { observer ->
            contentResolver.unregisterContentObserver(observer)
            settingsObserver = null
        }
    }
}
