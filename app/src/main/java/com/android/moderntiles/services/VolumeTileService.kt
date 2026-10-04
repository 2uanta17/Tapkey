package com.android.moderntiles.services

import android.content.Context
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.android.moderntiles.R

class VolumeTileService : TileService() {

    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        qsTile?.let { tile ->
            tile.label = getString(R.string.tile_volume_label)
            tile.subtitle = null // Clean single-line appearance
            tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_volume)
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        // Shows the native system on-screen volume slider
        audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }
}
