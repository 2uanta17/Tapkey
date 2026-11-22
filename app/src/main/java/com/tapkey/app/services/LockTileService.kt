package com.tapkey.app.services

import android.content.Intent
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tapkey.app.R
import com.tapkey.app.util.PermissionUtils
import com.tapkey.app.util.TileManager

class LockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
        // Pre-warm accessibility service reference when shade is pulled
        LockScreenAccessibilityService.instance
    }

    private fun updateTileState() {
        qsTile?.let { tile ->
            tile.label = getString(R.string.tile_lock_label)
            tile.subtitle = null // Clean single-line appearance
            tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_lock)

            // Standard neutral push-button state for quick action tiles
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        // 1. Direct synchronous execution on main thread without any coroutine dispatch lag
        val accessibilityService = LockScreenAccessibilityService.instance
        if (accessibilityService != null) {
            accessibilityService.lockScreen()
            return
        }

        // 2. If instance is not bound, verify if permission is enabled in Settings
        if (PermissionUtils.isAccessibilityServiceEnabled(this)) {
            val retryService = LockScreenAccessibilityService.instance
            if (retryService != null) {
                retryService.lockScreen()
                return
            }
        }

        // 3. Fallback: prompt user to enable the accessibility service in system settings
        fallbackToAccessibilitySettings()
    }

    private fun fallbackToAccessibilitySettings() {
        TileManager.startActivityAndCollapseCompat(
            this,
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        )
    }
}
