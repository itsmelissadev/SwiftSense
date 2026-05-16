package io.github.itsmelissadev.swiftsense.feature.screenrecorder

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat

class ScreenRecorderTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, ScreenRecorderPromptActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val wrapper = PendingIntentActivityWrapper(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            false
        )

        TileServiceCompat.startActivityAndCollapse(this, wrapper)
    }
}
