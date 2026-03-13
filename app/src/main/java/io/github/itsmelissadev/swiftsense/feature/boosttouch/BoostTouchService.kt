package io.github.itsmelissadev.swiftsense.feature.boosttouch

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.os.Process
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("AccessibilityPolicy")
class BoostTouchService : AccessibilityService() {
    private lateinit var preferenceManager: PreferenceManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    private var touchBoostEnabled = false

    companion object {
        @SuppressLint("StaticFieldLeak")
        var instance: BoostTouchService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)

        preferenceManager = PreferenceManager(this)

        serviceScope.launch {
            preferenceManager.touchBoostEnabled.collectLatest { enabled ->
                touchBoostEnabled = enabled
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onMotionEvent(event: MotionEvent) {
        if (!touchBoostEnabled) {
            super.onMotionEvent(event)
            return
        }

        super.onMotionEvent(event)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        instance = null
        super.onDestroy()
    }
}