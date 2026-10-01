package com.android.moderntiles.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.android.moderntiles.R

class SoundTileService : TileService() {

    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private var isReceiverRegistered = false

    private val ringerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                updateTileVisuals()
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileVisuals()

        if (!isReceiverRegistered) {
            val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
            registerReceiver(ringerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            isReceiverRegistered = true
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(ringerReceiver)
            } catch (_: IllegalArgumentException) {
                // Receiver was not registered
            }
            isReceiverRegistered = false
        }
    }

    override fun onClick() {
        super.onClick()
        val currentMode = audioManager.ringerMode
        val targetMode = when (currentMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    AudioManager.RINGER_MODE_SILENT
                } else {
                    AudioManager.RINGER_MODE_NORMAL
                }
            }
            AudioManager.RINGER_MODE_SILENT -> AudioManager.RINGER_MODE_NORMAL
            else -> AudioManager.RINGER_MODE_NORMAL
        }

        try {
            audioManager.ringerMode = targetMode
        } catch (_: SecurityException) {
            // Fallback gracefully to Normal if notification policy access is revoked
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }

        updateTileVisuals()
    }

    private fun updateTileVisuals() {
        qsTile?.let { tile ->
            tile.label = getString(R.string.tile_sound_label)
            when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> {
                    tile.state = Tile.STATE_ACTIVE
                    tile.subtitle = getString(R.string.tile_sound_normal)
                    tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_ring)
                }
                AudioManager.RINGER_MODE_VIBRATE -> {
                    tile.state = Tile.STATE_ACTIVE
                    tile.subtitle = getString(R.string.tile_sound_vibrate)
                    tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_vibrate)
                }
                AudioManager.RINGER_MODE_SILENT -> {
                    tile.state = Tile.STATE_INACTIVE
                    tile.subtitle = getString(R.string.tile_sound_silent)
                    tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_silent)
                }
                else -> {
                    tile.state = Tile.STATE_INACTIVE
                    tile.subtitle = getString(R.string.tile_sound_normal)
                    tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_ring)
                }
            }
            tile.updateTile()
        }
    }
}
