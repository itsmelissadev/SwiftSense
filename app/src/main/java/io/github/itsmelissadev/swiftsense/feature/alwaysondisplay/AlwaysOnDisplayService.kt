package io.github.itsmelissadev.swiftsense.feature.alwaysondisplay

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.content.pm.ServiceInfo
import android.os.Build
import io.github.itsmelissadev.swiftsense.MainActivity
import io.github.itsmelissadev.swiftsense.R

class AlwaysOnDisplayService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var preferenceJob: Job? = null
    private lateinit var preferenceManager: PreferenceManager
    private var activateOnLock = true
    private var dismissPowerButton = true
    
    
    private var isForegroundRunning = false

    companion object {
        private var instance: AlwaysOnDisplayService? = null
        var isAodVisible = false
            set(value) {
                field = value
                instance?.updateForegroundNotification(value)
            }
        var lastLaunchTime = 0L
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (activateOnLock) {
                        if (!isAodVisible) {
                            lastLaunchTime = System.currentTimeMillis()
                            launchAodActivity()
                        } else {
                            if (dismissPowerButton) {
                                wakeScreenUp()
                                dismissAodActivity()
                            }
                        }
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (isAodVisible && System.currentTimeMillis() - lastLaunchTime > 1500L) {
                        dismissAodActivity()
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    dismissAodActivity()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        preferenceManager = PreferenceManager(this)

        preferenceJob = scope.launch {
            launch {
                preferenceManager.aodActivateOnLock.collect { activate ->
                    activateOnLock = activate
                }
            }
            launch {
                preferenceManager.aodDismissPowerButton.collect { dismiss ->
                    dismissPowerButton = dismiss
                }
            }
            launch {
                preferenceManager.aodAsForegroundService.collect { runAsForeground ->
                    if (runAsForeground) {
                        startForegroundService()
                    } else {
                        stopForegroundService()
                    }
                }
            }
        }

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, screenFilter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    private fun wakeScreenUp() {
        val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = pm.newWakeLock(
            android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
            android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
            "SwiftSense:AODWakeLock"
        )
        wakeLock.acquire(3000)
    }

    private fun launchAodActivity() {
        val intent = Intent(this, AlwaysOnDisplayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        startActivity(intent)
    }

    private fun dismissAodActivity() {
        sendBroadcast(Intent(AlwaysOnDisplayActivity.ACTION_FINISH_AOD))
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        preferenceJob?.cancel()
        try { unregisterReceiver(screenStateReceiver) } catch (e: Exception) {}
        stopForegroundService()
        dismissAodActivity()
    }

    private val NOTIFICATION_ID = 2002
    private val CHANNEL_ID = "aod_foreground_service_channel"

    private fun createNotification(isActive: Boolean = false): Notification {
        val mainPI = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = getString(
            if (isActive) R.string.aod_status_active
            else R.string.aod_service_notification_text
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.aod_service_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(mainPI)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.aod_service_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(isAodVisible),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification(isAodVisible))
        }
        isForegroundRunning = true
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        isForegroundRunning = false
    }

    private fun updateForegroundNotification(isActive: Boolean) {
        if (isForegroundRunning) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, createNotification(isActive))
        }
    }
}
