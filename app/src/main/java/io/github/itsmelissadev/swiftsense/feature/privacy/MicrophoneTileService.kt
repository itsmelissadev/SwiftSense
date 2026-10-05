package io.github.itsmelissadev.swiftsense.feature.privacy

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MicrophoneTileService : TileService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var preferenceManager: PreferenceManager
    private var listeningJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        listeningJob?.cancel()
        listeningJob = serviceScope.launch {
            preferenceManager.isGlobalMicrophoneEnabled.collectLatest { isEnabled ->
                updateTile(isEnabled)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        listeningJob?.cancel()
    }

    private fun updateTile(isEnabled: Boolean) {
        val tile = qsTile ?: return

        if (!SensorPrivacyHelper.isSupported()) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = getString(R.string.tile_microphone_access)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_mic_24px)
            tile.updateTile()
            return
        }

        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_microphone_access)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(if (isEnabled) R.string.status_on else R.string.status_off)
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_mic_24px)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()

        if (!SensorPrivacyHelper.isSupported()) {
            Toast.makeText(this, R.string.sensor_privacy_requires_android_12, Toast.LENGTH_SHORT).show()
            return
        }

        if (!SensorPrivacyHelper.isShizukuAvailable()) {
            Toast.makeText(this, R.string.shizuku_not_authorized_desc, Toast.LENGTH_SHORT).show()
            return
        }

        serviceScope.launch {
            val currentState = preferenceManager.isGlobalMicrophoneEnabled.first()
            val nextState = !currentState

            val result = withContext(Dispatchers.IO) {
                SensorPrivacyHelper.setSensorEnabled("microphone", nextState)
            }

            if (result.isSuccess) {
                preferenceManager.setGlobalMicrophoneEnabled(nextState)
            } else {
                withContext(Dispatchers.Main) {
                    val errorMsg = result.exceptionOrNull()?.message
                    Toast.makeText(this@MicrophoneTileService, errorMsg ?: getString(R.string.shizuku_not_running), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
