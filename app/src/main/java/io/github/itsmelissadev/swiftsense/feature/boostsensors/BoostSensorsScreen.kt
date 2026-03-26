package io.github.itsmelissadev.swiftsense.feature.boostsensors

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.South
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                        stringResource(R.string.feature_boost_sensors),
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(8.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 24.dp
            ), // Increased vertical padding for expressive look
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FeatureCard(
                    title = if (isServiceRunning) stringResource(R.string.status_on) else stringResource(R.string.status_off),
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
                    },
                    containerColor = if (isServiceRunning)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }

            item {
                FeatureCard(
                    title = stringResource(R.string.show_live_hz_option),
                    description = stringResource(R.string.show_live_hz_desc),
                    icon = Icons.Rounded.Analytics,
                    checked = showLiveHz,
                    onCheckedChange = { scope.launch { preferenceManager.setShowLiveHz(it) } }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.tab_sensors),
                    style = MaterialTheme.typography.headlineSmall, // Increased from titleMedium
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary, // Increased contrast
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp) // Spacing
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
                    title = sensorName,
                    icon = sensorIcon,
                    checked = isEnabled,
                    onCheckedChange = { scope.launch { preferenceManager.setSensorState(sensor.type, it) } },
                    trailingContent = if (showLiveHz && isServiceRunning && isEnabled) {
                        { ThrottledLiveHzText(sensor.type) }
                    } else null,
                    containerColor = if (isEnabled)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
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
        color = MaterialTheme.colorScheme.primaryContainer, // Changed to primaryContainer instead of transparent alpha box
        shape = androidx.compose.foundation.shape.RoundedCornerShape(100), // Pill shape
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$displayHz Hz",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}