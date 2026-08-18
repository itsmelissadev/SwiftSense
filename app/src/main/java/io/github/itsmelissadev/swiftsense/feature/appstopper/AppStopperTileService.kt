package io.github.itsmelissadev.swiftsense.feature.appstopper

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppStopperTileService : TileService() {
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
            AppStopperService.isRunning.collectLatest {
                updateTile(it)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        listeningJob?.cancel()
    }

    private fun updateTile(isRunning: Boolean) {
        val tile = qsTile ?: return

        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.feature_app_stopper)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_bolt_24px)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val isRunning = AppStopperService.isRunning.value
        val nextState = !isRunning

        serviceScope.launch {
            preferenceManager.setStopperServiceRunning(nextState)
            val intent = Intent(this@AppStopperTileService, AppStopperService::class.java)
            if (nextState) {
                startForegroundService(intent)
            } else {
                stopService(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
