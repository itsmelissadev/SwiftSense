package io.github.itsmelissadev.swiftsense.feature.boostsensors

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.util.SparseIntArray
import android.util.SparseLongArray
import androidx.core.app.NotificationCompat
import io.github.itsmelissadev.swiftsense.MainActivity
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class BoostSensorsService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var preferenceManager: PreferenceManager
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observationJob: Job? = null
    private var liveHzJob: Job? = null

    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null

    private val currentRegisteredSensors = mutableSetOf<Int>()

    private val eventCounts = SparseIntArray()
    private val lastTimestamps = SparseLongArray()
    private val lock = Any()
    private val isCleanedUp = AtomicBoolean(false)

    @Volatile
    private var showLiveHz = false

    companion object {
        private const val CHANNEL_ID = "boost_sensors_channel"
        private const val NOTIFICATION_ID = 101
        const val ACTION_STOP = "io.github.itsmelissadev.swiftsense.feature.boostsensors.STOP"

        val liveHz = ConcurrentHashMap<Int, Int>()
        val liveValues = ConcurrentHashMap<Int, FloatArray>()

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun isRunning() = _isRunning.value
    }

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        preferenceManager = PreferenceManager(this)

        sensorThread = HandlerThread("SwiftSensorThread", Process.THREAD_PRIORITY_DISPLAY)
        sensorThread?.start()
        sensorHandler = Handler(sensorThread!!.looper)

        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SwiftSense:BoostSensorsWakeLock").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) {
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(""))
        observeSensorChanges()
        observeLiveHzPreference()
    }

    private fun observeLiveHzPreference() {
        liveHzJob?.cancel()
        liveHzJob = serviceScope.launch {
            preferenceManager.showLiveHz.collectLatest { show ->
                showLiveHz = show
                if (!show) {
                    synchronized(lock) {
                        liveHz.clear()
                        liveValues.clear()
                        eventCounts.clear()
                        lastTimestamps.clear()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            serviceScope.launch {
                preferenceManager.setServiceRunning(false)
                stopSelf()
            }
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSensorChanges() {
        observationJob?.cancel()
        observationJob = serviceScope.launch {
            combine(
                preferenceManager.preferences,
                preferenceManager.sensorSpeed,
                preferenceManager.sensorFastestMode
            ) { prefs, speed, fastestMode ->
                val sensorTypes = listOf(
                    Sensor.TYPE_GYROSCOPE,
                    Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
                    Sensor.TYPE_ACCELEROMETER,
                    Sensor.TYPE_MAGNETIC_FIELD,
                    Sensor.TYPE_ROTATION_VECTOR,
                    Sensor.TYPE_GRAVITY,
                    Sensor.TYPE_LINEAR_ACCELERATION
                )

                val activeSensors = sensorTypes.mapNotNull { type ->
                    val sensor = sensorManager.getDefaultSensor(type)
                    if (sensor != null) {
                        val enabled =
                            prefs[androidx.datastore.preferences.core.booleanPreferencesKey(
                                PreferenceManager.SENSOR_STATES_PREFIX + sensor.type
                            )] ?: false
                        if (enabled) sensor else null
                    } else null
                }
                Triple(activeSensors, speed, fastestMode)
            }.distinctUntilChanged().collect { (activeSensors, speed, fastestMode) ->
                updateSensorListeners(activeSensors, speed, fastestMode)
                updateNotification(activeSensors)
            }
        }
    }

    private fun updateSensorListeners(activeSensors: List<Sensor>, speed: String, fastestMode: Boolean) {
        if (isCleanedUp.get()) return

        val newSensorTypes = activeSensors.map { it.type }.toSet()

        val toUnregister = currentRegisteredSensors.filter { !newSensorTypes.contains(it) }
        toUnregister.forEach { type ->
            sensorManager.getDefaultSensor(type)?.let { sensor ->
                try {
                    sensorManager.unregisterListener(this, sensor)
                } catch (_: Exception) {
                }
            }
            liveHz.remove(type)
            liveValues.remove(type)
            lastTimestamps.delete(type)
            eventCounts.delete(type)
            currentRegisteredSensors.remove(type)
        }

        activeSensors.forEach { sensor ->
            val delayUs = when (speed) {
                "very_slow" -> 60000
                "slow" -> 20000
                "medium" -> 10000
                "fast" -> 5000
                "max" -> if (fastestMode) SensorManager.SENSOR_DELAY_FASTEST else sensor.minDelay.coerceAtLeast(0)
                else -> if (fastestMode) SensorManager.SENSOR_DELAY_FASTEST else sensor.minDelay.coerceAtLeast(0)
            }
            sensorManager.unregisterListener(this, sensor)
            sensorManager.registerListener(
                this,
                sensor,
                delayUs,
                sensorHandler
            )
            currentRegisteredSensors.add(sensor.type)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !showLiveHz || isCleanedUp.get()) return

        val type = event.sensor.type
        val values = event.values
        if (values != null && values.isNotEmpty()) {
            liveValues[type] = values.clone()
        }

        synchronized(lock) {
            val nowNanos = SystemClock.elapsedRealtimeNanos()
            val currentCount = eventCounts.get(type, 0) + 1
            eventCounts.put(type, currentCount)

            val lastTime = lastTimestamps.get(type, 0L)
            if (lastTime == 0L) {
                lastTimestamps.put(type, nowNanos)
            } else {
                val deltaNanos = nowNanos - lastTime
                if (deltaNanos >= 500_000_000L) {
                    val calculatedHz = ((currentCount.toLong() * 1_000_000_000L + (deltaNanos / 2)) / deltaNanos).toInt()
                    liveHz[type] = calculatedHz
                    eventCounts.put(type, 0)
                    lastTimestamps.put(type, nowNanos)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateNotification(activeSensors: List<Sensor>) {
        val sensorNames = activeSensors.joinToString(", ") { getSensorName(it.type) }
        val notification = createNotification(sensorNames)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(contentText: String): Notification {
        val stopIntent =
            Intent(this, BoostSensorsService::class.java).apply { action = ACTION_STOP }
        val stopPI = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val mainPI = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.feature_boost_sensors))
            .setContentText(contentText.ifEmpty { getString(R.string.status_running) })
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

    private fun getSensorName(type: Int): String {
        return when (type) {
            Sensor.TYPE_GYROSCOPE -> getString(R.string.sensor_gyroscope)
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> getString(R.string.sensor_gyroscope_uncalibrated)
            Sensor.TYPE_ACCELEROMETER -> getString(R.string.sensor_accelerometer)
            Sensor.TYPE_MAGNETIC_FIELD -> getString(R.string.sensor_magnetic_field)
            Sensor.TYPE_ROTATION_VECTOR -> getString(R.string.sensor_rotation_vector)
            Sensor.TYPE_GRAVITY -> getString(R.string.sensor_gravity)
            Sensor.TYPE_LINEAR_ACCELERATION -> getString(R.string.sensor_linear_acceleration)
            else -> "Sensor $type"
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_optimization),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun safeCleanupSensors() {
        if (!isCleanedUp.compareAndSet(false, true)) return

        observationJob?.cancel()
        observationJob = null
        liveHzJob?.cancel()
        liveHzJob = null

        val registeredCopy = synchronized(lock) {
            currentRegisteredSensors.toList()
        }

        if (registeredCopy.isNotEmpty()) {
            registeredCopy.forEach { type ->
                sensorManager.getDefaultSensor(type)?.let { sensor ->
                    try {
                        sensorManager.flush(this)
                        sensorManager.registerListener(
                            this,
                            sensor,
                            SensorManager.SENSOR_DELAY_NORMAL,
                            sensorHandler
                        )
                        sensorManager.flush(this)
                    } catch (_: Exception) {
                    }
                }
            }
        }

        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {
        }

        sensorHandler?.let { handler ->
            val latch = CountDownLatch(1)
            handler.post {
                latch.countDown()
            }
            try {
                latch.await(200, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
            }
        }

        sensorThread?.quitSafely()
        try {
            sensorThread?.join(200)
        } catch (_: InterruptedException) {
        }
        sensorThread = null
        sensorHandler = null

        synchronized(lock) {
            currentRegisteredSensors.clear()
            eventCounts.clear()
            lastTimestamps.clear()
            liveHz.clear()
            liveValues.clear()
        }
    }

    override fun onDestroy() {
        _isRunning.value = false
        safeCleanupSensors()
        serviceScope.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
