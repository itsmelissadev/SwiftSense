package io.github.itsmelissadev.swiftsense.feature.alwaysondisplay

import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.itsmelissadev.swiftsense.R

class AlwaysOnDisplayTileService : TileService() {


    override fun onStartListening() {
        super.onStartListening()
        val isEnabled = AccessibilityUtil.isAccessibilityServiceEnabled(this, AlwaysOnDisplayService::class.java)
        updateTile(isEnabled)
    }

    @Suppress("DEPRECATION")
    override fun onClick() {
        super.onClick()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityAndCollapse(intent)
    }

    private fun updateTile(isEnabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = getString(if (isEnabled) R.string.aod_status_active else R.string.aod_status_inactive)
        tile.updateTile()
    }
}
