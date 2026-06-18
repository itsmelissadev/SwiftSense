package io.github.itsmelissadev.swiftsense.feature.amoledscreenprotect

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSection
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSelectionDialog
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTextField
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTopAppBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AmoledScreenProtectScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferenceManager = remember { PreferenceManager(context) }

    val density by preferenceManager.amoledIntensity.collectAsState(initial = 0.5f)
    var densitySlider by remember(density) { mutableFloatStateOf(density) }

    val opacity by preferenceManager.amoledOpacity.collectAsState(initial = 0.5f)
    var opacitySlider by remember(opacity) { mutableFloatStateOf(opacity) }

    val filterType by preferenceManager.amoledFilterType.collectAsState(initial = "checker_grid")

    val cycleDuration by preferenceManager.amoledShiftSpeed.collectAsState(initial = 30)

    val isWarningDismissed by preferenceManager.amoledWarningDismissed.collectAsState(initial = false)

    val refreshMode by preferenceManager.amoledRefreshMode.collectAsState(initial = "smooth")

    val activeRegions by preferenceManager.amoledRegions.collectAsState(initial = setOf("full_screen"))
    var showRefreshModeDialog by remember { mutableStateOf(false) }
    var showFilterTypeDialog by remember { mutableStateOf(false) }
    var showCycleDurationDialog by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(AmoledProtectService.isServiceRunning) }

    LaunchedEffect(Unit) {
        while (isActive) {
            isRunning = AmoledProtectService.isServiceRunning
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            SwiftSenseTopAppBar(
                title = stringResource(R.string.feature_amoled_protect),
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            if (!isWarningDismissed) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.amoled_health_warning_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        preferenceManager.setAmoledWarningDismissed(
                                            true
                                        )
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.amoled_health_warning_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            FeatureCard(
                title = stringResource(if (isRunning) R.string.amoled_on else R.string.amoled_off),
                description = stringResource(R.string.service_status),
                icon = if (isRunning) Icons.Default.Shield else Icons.Default.ShieldMoon,
                containerColor = if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else null,
                onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            )

            SwiftSenseSection(title = stringResource(R.string.amoled_filter_settings)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val filterTypes = listOf(
                        "checker_grid" to stringResource(R.string.amoled_filter_checker_grid),
                        "dots" to stringResource(R.string.amoled_filter_dots),
                        "horizontal_lines" to stringResource(R.string.amoled_filter_horizontal),
                        "vertical_lines" to stringResource(R.string.amoled_filter_vertical),
                        "grid" to stringResource(R.string.amoled_filter_grid),
                        "diagonal" to stringResource(R.string.amoled_filter_diagonal),
                        "noise" to stringResource(R.string.amoled_filter_noise),
                        "pixel_shift" to stringResource(R.string.amoled_filter_pixel_shift)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SwiftSenseTextField(
                            value = filterTypes.find { it.first == filterType }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.amoled_filter_type),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showFilterTypeDialog = true }
                        )
                    }

                    val durations = listOf(
                        10 to stringResource(R.string.amoled_duration_10s),
                        30 to stringResource(R.string.amoled_duration_30s),
                        60 to stringResource(R.string.amoled_duration_1m),
                        120 to stringResource(R.string.amoled_duration_2m),
                        180 to stringResource(R.string.amoled_duration_3m),
                        300 to stringResource(R.string.amoled_duration_5m)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SwiftSenseTextField(
                            value = durations.find { it.first == cycleDuration }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.amoled_cycle_duration),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showCycleDurationDialog = true }
                        )
                    }

                    val modes = listOf(
                        "smooth" to stringResource(R.string.amoled_mode_smooth),
                        "jump" to stringResource(R.string.amoled_mode_jump)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SwiftSenseTextField(
                            value = modes.find { it.first == refreshMode }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.amoled_refresh_mode),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showRefreshModeDialog = true }
                        )
                    }

                    Text(
                        text = if (refreshMode == "smooth") stringResource(R.string.amoled_mode_smooth_desc) else stringResource(
                            R.string.amoled_mode_jump_desc
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            SwiftSenseSection(title = stringResource(R.string.filter_intensity)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.amoled_pixel_density),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${(densitySlider * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Slider(
                            value = densitySlider,
                            onValueChange = { densitySlider = it },
                            onValueChangeFinished = {
                                scope.launch {
                                    preferenceManager.setAmoledIntensity(
                                        densitySlider
                                    )
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.amoled_filter_opacity),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${(opacitySlider * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Slider(
                            value = opacitySlider,
                            onValueChange = { opacitySlider = it },
                            onValueChangeFinished = {
                                scope.launch {
                                    preferenceManager.setAmoledOpacity(
                                        opacitySlider
                                    )
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            SwiftSenseSection(title = stringResource(R.string.amoled_active_regions)) {
                val regions = listOf(
                    "full_screen" to stringResource(R.string.amoled_region_fullscreen),
                    "status_bar" to stringResource(R.string.amoled_region_statusbar),
                    "navigation_bar" to stringResource(R.string.amoled_region_navbar)
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    regions.forEach { (key, label) ->
                        val isChecked = activeRegions.contains(key)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        val newRegions = activeRegions.toMutableSet()
                                        if (isChecked) {
                                            if (newRegions.size > 1) newRegions.remove(key)
                                        } else {
                                            newRegions.add(key)
                                        }
                                        preferenceManager.setAmoledRegions(newRegions)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Switch(
                                checked = isChecked,
                                onCheckedChange = {
                                    scope.launch {
                                        val newRegions = activeRegions.toMutableSet()
                                        if (isChecked) {
                                            if (newRegions.size > 1) newRegions.remove(key)
                                        } else {
                                            newRegions.add(key)
                                        }
                                        preferenceManager.setAmoledRegions(newRegions)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showRefreshModeDialog) {
        val modes = listOf(
            "smooth" to stringResource(R.string.amoled_mode_smooth),
            "jump" to stringResource(R.string.amoled_mode_jump)
        )
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.amoled_refresh_mode),
            options = modes,
            selected = refreshMode,
            onSelectedChange = { mode ->
                scope.launch { preferenceManager.setAmoledRefreshMode(mode) }
            },
            onDismissRequest = { showRefreshModeDialog = false }
        )
    }

    if (showFilterTypeDialog) {
        val filterTypes = listOf(
            "checker_grid" to stringResource(R.string.amoled_filter_checker_grid),
            "dots" to stringResource(R.string.amoled_filter_dots),
            "horizontal_lines" to stringResource(R.string.amoled_filter_horizontal),
            "vertical_lines" to stringResource(R.string.amoled_filter_vertical),
            "grid" to stringResource(R.string.amoled_filter_grid),
            "diagonal" to stringResource(R.string.amoled_filter_diagonal),
            "noise" to stringResource(R.string.amoled_filter_noise),
            "pixel_shift" to stringResource(R.string.amoled_filter_pixel_shift)
        )
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.amoled_filter_type),
            options = filterTypes,
            selected = filterType,
            onSelectedChange = { type ->
                scope.launch { preferenceManager.setAmoledFilterType(type) }
            },
            onDismissRequest = { showFilterTypeDialog = false }
        )
    }

    if (showCycleDurationDialog) {
        val durations = listOf(
            10 to stringResource(R.string.amoled_duration_10s),
            30 to stringResource(R.string.amoled_duration_30s),
            60 to stringResource(R.string.amoled_duration_1m),
            120 to stringResource(R.string.amoled_duration_2m),
            180 to stringResource(R.string.amoled_duration_3m),
            300 to stringResource(R.string.amoled_duration_5m)
        )
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.amoled_cycle_duration),
            options = durations,
            selected = cycleDuration,
            onSelectedChange = { seconds ->
                scope.launch { preferenceManager.setAmoledShiftSpeed(seconds) }
            },
            onDismissRequest = { showCycleDurationDialog = false }
        )
    }
}


