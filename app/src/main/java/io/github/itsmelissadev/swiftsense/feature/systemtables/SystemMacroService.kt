package io.github.itsmelissadev.swiftsense.feature.systemtables

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.github.itsmelissadev.swiftsense.MainActivity
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.shizuku.ShizukuShellRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SystemMacroService : Service() {
    private lateinit var preferenceManager: PreferenceManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var configObservationJob: Job? = null
    private var loopJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "system_macros_channel"
        private const val NOTIFICATION_ID = 104
        const val ACTION_STOP = "io.github.itsmelissadev.swiftsense.feature.systemtables.STOP"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        preferenceManager = PreferenceManager(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("", 30))
        observeConfigChanges()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            serviceScope.launch {
                preferenceManager.setMacroServiceRunning(false)
                stopSelf()
            }
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun observeConfigChanges() {
        configObservationJob?.cancel()
        configObservationJob = serviceScope.launch {
            combine(
                preferenceManager.autoApplyMacroJson,
                preferenceManager.autoApplyMacroInterval
            ) { macroJson, interval ->
                macroJson to interval
            }.distinctUntilChanged().collectLatest { (macroJson, interval) ->
                loopJob?.cancel()
                if (macroJson != null) {
                    val macro = try {
                        SystemMacro.fromJson(macroJson)
                    } catch (e: Exception) {
                        null
                    }
                    if (macro != null) {
                        updateNotification(macro.name, interval)
                        loopJob = serviceScope.launch {
                            while (true) {
                                applyMacro(macro)
                                delay(interval * 1000L)
                            }
                        }
                    } else {
                        updateNotification("", interval)
                    }
                } else {
                    updateNotification("", interval)
                }
            }
        }
    }

    private suspend fun applyMacro(macro: SystemMacro) {
        withContext(Dispatchers.IO) {
            macro.settings.forEach { setting ->
                val cmd = "settings put ${setting.table.key} ${setting.key} ${setting.targetValue}"
                ShizukuShellRunner.runCommand(cmd)
            }
        }
    }

    private fun updateNotification(macroName: String, intervalSeconds: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(macroName, intervalSeconds))
    }

    private fun createNotification(macroName: String, intervalSeconds: Int): Notification {
        val stopIntent = Intent(this, SystemMacroService::class.java).apply { action = ACTION_STOP }
        val stopPI = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val mainPI = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (macroName.isNotEmpty()) {
            getString(R.string.macro_service_notif_desc, macroName, intervalSeconds)
        } else {
            getString(R.string.status_running)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.macro_service_notif_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_bolt_24px)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(mainPI)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.stop_service),
                stopPI
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.macro_service_notif_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        _isRunning.value = false
        configObservationJob?.cancel()
        loopJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
