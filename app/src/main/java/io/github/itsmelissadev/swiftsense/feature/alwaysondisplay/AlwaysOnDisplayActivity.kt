package io.github.itsmelissadev.swiftsense.feature.alwaysondisplay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.abs

class AlwaysOnDisplayActivity : ComponentActivity() {

    private lateinit var preferenceManager: PreferenceManager
    private lateinit var aodView: AodOverlayView
    private var preferenceJob: Job? = null

    private var dismissDoubleTap = true
    private lateinit var gestureDetector: GestureDetector
    private lateinit var insetsController: WindowInsetsControllerCompat
    
    private var timeoutSeconds = 0
    private var timeoutJob: Job? = null

    private fun resetTimeout() {
        timeoutJob?.cancel()
        if (timeoutSeconds > 0) {
            timeoutJob = CoroutineScope(Dispatchers.Main).launch {
                delay(timeoutSeconds * 1000L)
                finish()
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_TIME_TICK -> aodView.invalidate()
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                    if (level != -1 && scale != -1) {
                        aodView.batteryLevel = (level * 100 / scale.toFloat()).toInt()
                    }
                    aodView.batteryIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                    val (watts, mA) = readChargingInfo(voltageMv)
                    aodView.chargingWatts = watts
                    aodView.chargingCurrentMa = mA
                }
                Intent.ACTION_USER_PRESENT -> finish()
                ACTION_FINISH_AOD -> finish()
            }
        }
    }

    companion object {
        const val ACTION_FINISH_AOD = "io.github.itsmelissadev.swiftsense.FINISH_AOD"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = PreferenceManager(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        insetsController = WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        AlwaysOnDisplayService.isAodVisible = true

        aodView = AodOverlayView(this).apply { isAodVisible = true }
        setContentView(aodView)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (dismissDoubleTap) {
                    finish()
                }
                return true
            }
        })

        val initialBattery: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
            registerReceiver(null, it)
        }
        val level = initialBattery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = initialBattery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = initialBattery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        if (level != -1 && scale != -1) {
            aodView.batteryLevel = (level * 100 / scale.toFloat()).toInt()
        }
        aodView.batteryIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val initialVoltageMv = initialBattery?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val (initWatts, initMa) = readChargingInfo(initialVoltageMv)
        aodView.chargingWatts = initWatts
        aodView.chargingCurrentMa = initMa

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(ACTION_FINISH_AOD)
        }
        registerReceiver(receiver, filter)

        preferenceJob = CoroutineScope(Dispatchers.Main).launch {
            launch { preferenceManager.aodShowClock.collect { aodView.showClock = it } }
            launch { preferenceManager.aodShowDate.collect { aodView.showDate = it } }
            launch { preferenceManager.aodShowBattery.collect { aodView.showBattery = it } }
            launch { preferenceManager.aodShowNotifications.collect { aodView.showNotifications = it } }
            launch { preferenceManager.aodClockStyle.collect { aodView.clockStyle = it } }
            launch { preferenceManager.aodBatteryStyle.collect { aodView.batteryStyle = it } }
            launch { preferenceManager.aodClockColor.collect { aodView.clockColor = it } }
            launch { preferenceManager.aodBatteryColor.collect { aodView.batteryColor = it } }
            launch { preferenceManager.aodFontFamily.collect { aodView.fontFamily = it } }
            launch { preferenceManager.aodBurnInProtection.collect { aodView.burnInProtection = it } }
            launch { preferenceManager.aodBurnInInterval.collect { aodView.burnInInterval = it } }
            launch { preferenceManager.aodBurnInMode.collect { aodView.burnInMode = it } }
            launch { preferenceManager.aodBurnInRgbShift.collect { aodView.burnInRgbShift = it } }
            launch {
                preferenceManager.aodBrightness.collect { brightness ->
                    window.attributes = window.attributes.apply { screenBrightness = brightness }
                    aodView.brightness = brightness
                }
            }
            launch { preferenceManager.aodDismissDoubleTap.collect { dismissDoubleTap = it } }
            launch { preferenceManager.aodShowWattage.collect { aodView.showWattage = it } }
            launch {
                preferenceManager.aodTimeoutSeconds.collect { seconds ->
                    timeoutSeconds = seconds
                    resetTimeout()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        resetTimeout()
        val decorHeight = window.decorView.height
        val blockZone = resources.displayMetrics.density * 56f
        if (ev.y < blockZone || ev.y > decorHeight - blockZone) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        AlwaysOnDisplayService.isAodVisible = false
        preferenceJob?.cancel()
        timeoutJob?.cancel()
        try { unregisterReceiver(receiver) } catch (e: Exception) {}
    }

    private fun readChargingInfo(voltageMv: Int): Pair<Float, Int> {
        return try {
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val rawCurrent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            val absCurrent = abs(rawCurrent)
            if (absCurrent == 0) return Pair(0f, 0)

            val currentMa = if (absCurrent > 20_000) absCurrent / 1000 else absCurrent

            val volts = if (voltageMv > 0) voltageMv / 1000f else 5f
            val watts = (currentMa / 1000f) * volts

            Pair(watts, currentMa)
        } catch (e: Exception) { Pair(0f, 0) }
    }
}
