package io.github.itsmelissadev.swiftsense.feature.boostsensors

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoostSensorsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val scope = rememberCoroutineScope()
    val isServiceRunning by preferenceManager.isServiceRunning.collectAsState(initial = false)
    val showLiveHz by preferenceManager.showLiveHz.collectAsState(initial = false)
    val prefs by preferenceManager.preferences.collectAsState(initial = null)

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensorList = remember {
        val types = listOf(
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION
        )
        types.mapNotNull { sensorManager.getDefaultSensor(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.feature_boost_sensors).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                FeatureCard(
                    title = if (isServiceRunning) stringResource(R.string.status_on).uppercase() else stringResource(
                        R.string.status_off
                    ).uppercase(),
                    description = if (isServiceRunning) stringResource(R.string.status_running) else stringResource(R.string.status_stopped),
                    icon = if (isServiceRunning) Icons.Rounded.RocketLaunch else Icons.Rounded.PowerSettingsNew,
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
                FeatureCard(
                    title = stringResource(R.string.show_live_hz_option).uppercase(),
                    description = stringResource(R.string.show_live_hz_desc),
                    icon = Icons.Rounded.Analytics,
                    checked = showLiveHz,
                    onCheckedChange = { scope.launch { preferenceManager.setShowLiveHz(it) } }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.tab_sensors).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(sensorList) { sensor ->
                val isEnabled = prefs?.get(booleanPreferencesKey(PreferenceManager.SENSOR_STATES_PREFIX + sensor.type)) ?: false

                val (sensorName, sensorIcon) = when (sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> stringResource(R.string.sensor_gyroscope) to Icons.Rounded.ScreenRotation
                    Sensor.TYPE_ACCELEROMETER -> stringResource(R.string.sensor_accelerometer) to Icons.AutoMirrored.Rounded.DirectionsRun
                    Sensor.TYPE_MAGNETIC_FIELD -> stringResource(R.string.sensor_magnetic_field) to Icons.Rounded.Sensors
                    Sensor.TYPE_ROTATION_VECTOR -> stringResource(R.string.sensor_rotation_vector) to Icons.Rounded.ViewInAr
                    Sensor.TYPE_GRAVITY -> stringResource(R.string.sensor_gravity) to Icons.Rounded.South
                    Sensor.TYPE_LINEAR_ACCELERATION -> stringResource(R.string.sensor_linear_acceleration) to Icons.Rounded.Speed
                    else -> sensor.name to Icons.Rounded.Sensors
                }

                FeatureCard(
                    title = sensorName.uppercase(),
                    icon = sensorIcon,
                    checked = isEnabled,
                    onCheckedChange = { scope.launch { preferenceManager.setSensorState(sensor.type, it) } },
                    trailingContent = if (showLiveHz && isServiceRunning && isEnabled) {
                        { ThrottledLiveHzText(sensor.type) }
                    } else null
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ThrottledLiveHzText(sensorType: Int) {
    var displayHz by remember { mutableIntStateOf(0) }
    LaunchedEffect(sensorType) {
        while(true) {
            displayHz = BoostSensorsService.liveHz[sensorType] ?: 0
            delay(1000L)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = "$displayHz HZ",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
