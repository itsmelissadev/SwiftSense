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

class AlwaysOnDisplayService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var preferenceJob: Job? = null
    private lateinit var preferenceManager: PreferenceManager
    private var activateOnLock = true
    private var dismissPowerButton = true
    
    
    companion object {
        var isAodVisible = false
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
        preferenceJob?.cancel()
        try { unregisterReceiver(screenStateReceiver) } catch (e: Exception) {}
        dismissAodActivity()
    }
}
