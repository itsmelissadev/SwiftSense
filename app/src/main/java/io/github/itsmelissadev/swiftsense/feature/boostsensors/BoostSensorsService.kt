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
import android.os.Process
import android.util.SparseIntArray
import android.util.SparseLongArray
import androidx.compose.runtime.mutableStateMapOf
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BoostSensorsService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var preferenceManager: PreferenceManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observationJob: Job? = null
    private var liveHzJob: Job? = null

    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null

    private val currentRegisteredSensors = mutableSetOf<Int>()

    private val eventCounts = SparseIntArray()
    private val lastTimestamps = SparseLongArray()

    @Volatile
    private var showLiveHz = false

    companion object {
        private const val CHANNEL_ID = "boost_sensors_channel"
        private const val NOTIFICATION_ID = 101
        const val ACTION_STOP = "io.github.itsmelissadev.swiftsense.feature.boostsensors.STOP"

        val liveHz = mutableStateMapOf<Int, Int>()

        private var isServiceRunning = false
        fun isRunning() = isServiceRunning
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        preferenceManager = PreferenceManager(this)

        sensorThread = HandlerThread("SwiftSensorThread", Process.THREAD_PRIORITY_DISPLAY)
        sensorThread?.start()
        sensorHandler = Handler(sensorThread!!.looper)

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
                    liveHz.clear()
                    eventCounts.clear()
                    lastTimestamps.clear()
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
            preferenceManager.preferences.map { prefs ->
                val sensorTypes = listOf(
                    Sensor.TYPE_GYROSCOPE,
                    Sensor.TYPE_ACCELEROMETER,
                    Sensor.TYPE_MAGNETIC_FIELD,
                    Sensor.TYPE_ROTATION_VECTOR,
                    Sensor.TYPE_GRAVITY,
                    Sensor.TYPE_LINEAR_ACCELERATION
                )

                sensorTypes.mapNotNull { type ->
                    val sensor = sensorManager.getDefaultSensor(type)
                    if (sensor != null) {
                        val enabled =
                            prefs[androidx.datastore.preferences.core.booleanPreferencesKey(
                                PreferenceManager.SENSOR_STATES_PREFIX + sensor.type
                            )] ?: false
                        if (enabled) sensor else null
                    } else null
                }
            }.distinctUntilChanged().collect { activeSensors ->
                updateSensorListeners(activeSensors)
                updateNotification(activeSensors)
            }
        }

    }

    private fun updateSensorListeners(activeSensors: List<Sensor>) {
        val newSensorTypes = activeSensors.map { it.type }.toSet()

        val toUnregister = currentRegisteredSensors.filter { !newSensorTypes.contains(it) }
        toUnregister.forEach { type ->
            sensorManager.getDefaultSensor(type)?.let { sensorManager.unregisterListener(this, it) }
            liveHz.remove(type)
            lastTimestamps.delete(type)
            eventCounts.delete(type)
            currentRegisteredSensors.remove(type)
        }

        activeSensors.forEach { sensor ->
            if (!currentRegisteredSensors.contains(sensor.type)) {
                sensorManager.registerListener(
                    this,
                    sensor,
                    SensorManager.SENSOR_DELAY_FASTEST,
                    sensorHandler
                )
                currentRegisteredSensors.add(sensor.type)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !showLiveHz) return

        val type = event.sensor.type
        val timestamp = event.timestamp

        val currentCount = eventCounts.get(type, 0) + 1
        eventCounts.put(type, currentCount)

        val lastTime = lastTimestamps.get(type, 0L)

        if (timestamp - lastTime >= 1_000_000_000L) {
            if (lastTime != 0L) {
                liveHz[type] = currentCount
            }
            eventCounts.put(type, 0)
            lastTimestamps.put(type, timestamp)
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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

    override fun onDestroy() {
        isServiceRunning = false
        observationJob?.cancel()
        liveHzJob?.cancel()
        sensorManager.unregisterListener(this)
        sensorThread?.quitSafely()
        serviceScope.cancel()

        eventCounts.clear()
        lastTimestamps.clear()
        liveHz.clear()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
