package io.github.itsmelissadev.swiftsense.feature.boostsensors

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.SparseIntArray
import android.util.SparseLongArray
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.South
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveCardGroup
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveHeader
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveHeroToggleCard
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveIconBadge
import io.github.itsmelissadev.swiftsense.ui.components.ExpressivePreferenceItem
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSelectionDialog
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSwitch
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSwitchPreference
import io.github.itsmelissadev.swiftsense.ui.theme.BadgeColors
import io.github.itsmelissadev.swiftsense.ui.theme.ExpressivePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun BoostSensorsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val scope = rememberCoroutineScope()
    val isServiceRunning by BoostSensorsService.isRunning.collectAsState()
    val showLiveHz by preferenceManager.showLiveHz.collectAsState(initial = false)
    val sensorFastestMode by preferenceManager.sensorFastestMode.collectAsState(initial = true)
    val sensorSpeed by preferenceManager.sensorSpeed.collectAsState(initial = "max")
    val prefs by preferenceManager.preferences.collectAsState(initial = null)
    var showSpeedDialog by remember { mutableStateOf(false) }

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensorList = remember {
        val types = listOf(
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION
        )
        types.mapNotNull { sensorManager.getDefaultSensor(it) }
    }

    val speeds = listOf(
        "very_slow" to stringResource(R.string.speed_very_slow),
        "slow" to stringResource(R.string.speed_slow),
        "medium" to stringResource(R.string.speed_medium),
        "fast" to stringResource(R.string.speed_fast),
        "max" to stringResource(R.string.speed_max)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ExpressiveHeader(
                title = stringResource(R.string.feature_boost_sensors),
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                ExpressiveHeroToggleCard(
                    title = if (isServiceRunning) stringResource(R.string.status_on)
                    else stringResource(R.string.status_off),
                    subtitle = if (isServiceRunning) stringResource(R.string.status_running)
                    else stringResource(R.string.status_stopped),
                    checked = isServiceRunning,
                    onCheckedChange = { active ->
                        scope.launch {
                            preferenceManager.setServiceRunning(active)
                            val intent = Intent(context, BoostSensorsService::class.java)
                            if (active) {
                                context.startForegroundService(intent)
                            } else {
                                context.stopService(intent)
                            }
                        }
                    }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )

                ExpressiveCardGroup {
                    item {
                        ExpressiveSwitchPreference(
                            title = stringResource(R.string.show_live_hz_option),
                            description = stringResource(R.string.show_live_hz_desc),
                            icon = Icons.Rounded.Analytics,
                            badgeColors = ExpressivePalette.BlueLight,
                            checked = showLiveHz,
                            onCheckedChange = { scope.launch { preferenceManager.setShowLiveHz(it) } }
                        )
                    }

                    item {
                        ExpressiveSwitchPreference(
                            title = stringResource(R.string.sensor_fastest_mode_option),
                            description = stringResource(R.string.sensor_fastest_mode_desc),
                            icon = Icons.Rounded.Speed,
                            badgeColors = ExpressivePalette.PurpleLight,
                            checked = sensorFastestMode,
                            onCheckedChange = { scope.launch { preferenceManager.setSensorFastestMode(it) } }
                        )
                    }

                    item {
                        ExpressivePreferenceItem(
                            title = stringResource(R.string.sensor_speed),
                            description = speeds.find { it.first == sensorSpeed }?.second ?: "",
                            icon = Icons.Rounded.Tune,
                            badgeColors = ExpressivePalette.AmberLight,
                            onClick = { showSpeedDialog = true }
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.tab_sensors),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )

                ExpressiveCardGroup {
                    sensorList.forEach { sensor ->
                        val isEnabled = prefs?.get(
                            booleanPreferencesKey(PreferenceManager.SENSOR_STATES_PREFIX + sensor.type)
                        ) ?: false

                        val (sensorName, sensorIcon, badgeColors) = when (sensor.type) {
                            Sensor.TYPE_GYROSCOPE -> Triple(
                                stringResource(R.string.sensor_gyroscope),
                                Icons.Rounded.ScreenRotation,
                                ExpressivePalette.BlueLight
                            )
                            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> Triple(
                                stringResource(R.string.sensor_gyroscope_uncalibrated),
                                Icons.Rounded.ScreenRotation,
                                ExpressivePalette.CyanLight
                            )
                            Sensor.TYPE_ACCELEROMETER -> Triple(
                                stringResource(R.string.sensor_accelerometer),
                                Icons.AutoMirrored.Rounded.DirectionsRun,
                                ExpressivePalette.GreenLight
                            )
                            Sensor.TYPE_MAGNETIC_FIELD -> Triple(
                                stringResource(R.string.sensor_magnetic_field),
                                Icons.Rounded.Sensors,
                                ExpressivePalette.PurpleLight
                            )
                            Sensor.TYPE_ROTATION_VECTOR -> Triple(
                                stringResource(R.string.sensor_rotation_vector),
                                Icons.Rounded.ViewInAr,
                                ExpressivePalette.PinkLight
                            )
                            Sensor.TYPE_GRAVITY -> Triple(
                                stringResource(R.string.sensor_gravity),
                                Icons.Rounded.South,
                                ExpressivePalette.OrangeLight
                            )
                            Sensor.TYPE_LINEAR_ACCELERATION -> Triple(
                                stringResource(R.string.sensor_linear_acceleration),
                                Icons.Rounded.Speed,
                                ExpressivePalette.TealLight
                            )
                            else -> Triple(
                                sensor.name,
                                Icons.Rounded.Sensors,
                                ExpressivePalette.AmberLight
                            )
                        }

                        item {
                            SensorPreferenceItem(
                                sensor = sensor,
                                sensorName = sensorName,
                                sensorIcon = sensorIcon,
                                badgeColors = badgeColors,
                                isEnabled = isEnabled,
                                showLiveHz = showLiveHz,
                                isServiceRunning = isServiceRunning,
                                onCheckedChange = {
                                    scope.launch { preferenceManager.setSensorState(sensor.type, it) }
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    if (showSpeedDialog) {
        ExpressiveSelectionDialog(
            title = stringResource(R.string.sensor_speed),
            options = speeds,
            selected = sensorSpeed,
            onSelectedChange = { speed ->
                scope.launch { preferenceManager.setSensorSpeed(speed) }
            },
            onDismissRequest = { showSpeedDialog = false }
        )
    }
}

@Composable
private fun SensorPreferenceItem(
    sensor: Sensor,
    sensorName: String,
    sensorIcon: ImageVector,
    badgeColors: BadgeColors,
    isEnabled: Boolean,
    showLiveHz: Boolean,
    isServiceRunning: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var currentHz by remember { mutableIntStateOf(0) }
    var currentValues by remember { mutableStateOf<FloatArray?>(null) }

    if (showLiveHz && isEnabled && isServiceRunning) {
        LaunchedEffect(sensor.type) {
            while (true) {
                val hz = BoostSensorsService.liveHz[sensor.type] ?: 0
                currentHz = hz
                currentValues = BoostSensorsService.liveValues[sensor.type]
                val dynamicDelay = when {
                    hz <= 0 -> 100L
                    hz < 20 -> 50L
                    hz < 60 -> 25L
                    else -> 16L
                }
                delay(dynamicDelay)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isEnabled) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExpressiveIconBadge(icon = sensorIcon, colors = badgeColors)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = sensorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (showLiveHz && isEnabled && isServiceRunning) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$currentHz HZ",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (showLiveHz && isEnabled && isServiceRunning) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatSensorValues(sensor.type, currentValues),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        ExpressiveSwitch(
            checked = isEnabled,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun formatSensorValues(type: Int, values: FloatArray?): String {
    if (values == null || values.isEmpty()) return "..."
    return when (type) {
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GRAVITY,
        Sensor.TYPE_LINEAR_ACCELERATION -> {
            val x = String.format(Locale.US, "%.2f", values.getOrElse(0) { 0f })
            val y = String.format(Locale.US, "%.2f", values.getOrElse(1) { 0f })
            val z = String.format(Locale.US, "%.2f", values.getOrElse(2) { 0f })
            "X: $x  Y: $y  Z: $z m/s²"
        }
        Sensor.TYPE_GYROSCOPE -> {
            val x = String.format(Locale.US, "%.2f", values.getOrElse(0) { 0f })
            val y = String.format(Locale.US, "%.2f", values.getOrElse(1) { 0f })
            val z = String.format(Locale.US, "%.2f", values.getOrElse(2) { 0f })
            "X: $x  Y: $y  Z: $z rad/s"
        }
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> {
            val x = String.format(Locale.US, "%.2f", values.getOrElse(0) { 0f })
            val y = String.format(Locale.US, "%.2f", values.getOrElse(1) { 0f })
            val z = String.format(Locale.US, "%.2f", values.getOrElse(2) { 0f })
            "X: $x  Y: $y  Z: $z"
        }
        Sensor.TYPE_MAGNETIC_FIELD -> {
            val x = String.format(Locale.US, "%.1f", values.getOrElse(0) { 0f })
            val y = String.format(Locale.US, "%.1f", values.getOrElse(1) { 0f })
            val z = String.format(Locale.US, "%.1f", values.getOrElse(2) { 0f })
            "X: $x  Y: $y  Z: $z µT"
        }
        Sensor.TYPE_ROTATION_VECTOR -> {
            val x = String.format(Locale.US, "%.2f", values.getOrElse(0) { 0f })
            val y = String.format(Locale.US, "%.2f", values.getOrElse(1) { 0f })
            val z = String.format(Locale.US, "%.2f", values.getOrElse(2) { 0f })
            "X: $x  Y: $y  Z: $z"
        }
        else -> {
            if (values.size >= 3) {
                val x = String.format(Locale.US, "%.2f", values[0])
                val y = String.format(Locale.US, "%.2f", values[1])
                val z = String.format(Locale.US, "%.2f", values[2])
                "X: $x  Y: $y  Z: $z"
            } else {
                values.joinToString(", ") { String.format(Locale.US, "%.2f", it) }
            }
        }
    }
}
