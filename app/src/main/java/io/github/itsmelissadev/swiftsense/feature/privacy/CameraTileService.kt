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

class CameraTileService : TileService() {
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
            preferenceManager.isGlobalCameraEnabled.collectLatest { isEnabled ->
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
            tile.label = getString(R.string.tile_camera_access)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_camera_24px)
            tile.updateTile()
            return
        }

        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_camera_access)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(if (isEnabled) R.string.status_on else R.string.status_off)
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_camera_24px)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()

        if (!SensorPrivacyHelper.isSupported()) {
            Toast.makeText(this, R.string.sensor_privacy_requires_android_12, Toast.LENGTH_SHORT).show()
            return
        }

        serviceScope.launch {
            val isReady = io.github.itsmelissadev.swiftsense.service.shell.ShellBridge.ensureBackendReady(this@CameraTileService)
            if (!isReady) {
                withContext(Dispatchers.Main) {
                    val mode = io.github.itsmelissadev.swiftsense.service.shell.ShellBridge.getActiveMode(this@CameraTileService)
                    val messageRes = if (mode == "adb") {
                        R.string.swiftsense_service_not_running
                    } else {
                        R.string.shizuku_not_authorized_desc
                    }
                    Toast.makeText(this@CameraTileService, messageRes, Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val currentState = preferenceManager.isGlobalCameraEnabled.first()
            val nextState = !currentState

            val result = withContext(Dispatchers.IO) {
                SensorPrivacyHelper.setSensorEnabled("camera", nextState)
            }

            if (result.isSuccess) {
                preferenceManager.setGlobalCameraEnabled(nextState)
            } else {
                withContext(Dispatchers.Main) {
                    val errorMsg = result.exceptionOrNull()?.message
                    val fallbackMsg = getString(R.string.execution_service_not_ready)
                    val toastText = if (!errorMsg.isNullOrBlank()) {
                        getString(R.string.execution_failed, errorMsg)
                    } else {
                        fallbackMsg
                    }
                    Toast.makeText(this@CameraTileService, toastText, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
