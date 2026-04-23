package io.github.itsmelissadev.swiftsense.feature.amoledscreenprotect

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
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

    // Density
    val density by preferenceManager.amoledIntensity.collectAsState(initial = 0.5f)
    var densitySlider by remember(density) { mutableFloatStateOf(density) }

    // Opacity
    val opacity by preferenceManager.amoledOpacity.collectAsState(initial = 0.5f)
    var opacitySlider by remember(opacity) { mutableFloatStateOf(opacity) }

    // Filter type
    val filterType by preferenceManager.amoledFilterType.collectAsState(initial = "checker_grid")

    // Cycle duration
    val cycleDuration by preferenceManager.amoledShiftSpeed.collectAsState(initial = 30)

    // Warning state
    val isWarningDismissed by preferenceManager.amoledWarningDismissed.collectAsState(initial = false)

    // Refresh Mode
    val refreshMode by preferenceManager.amoledRefreshMode.collectAsState(initial = "smooth")

    // Active regions
    val activeRegions by preferenceManager.amoledRegions.collectAsState(initial = setOf("full_screen"))

    var isRunning by remember { mutableStateOf(AmoledProtectService.isServiceRunning) }

    LaunchedEffect(Unit) {
        while (isActive) {
            isRunning = AmoledProtectService.isServiceRunning
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.feature_amoled_protect).uppercase(),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Health Warning Card ──
            if (!isWarningDismissed) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
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
                                text = stringResource(R.string.amoled_health_warning_title).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
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

            // ── Service Status Card ──
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

            // ── Refresh Mode Selection ──
            SettingsSection(title = stringResource(R.string.amoled_refresh_mode)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = refreshMode == "smooth",
                        onClick = { scope.launch { preferenceManager.setAmoledRefreshMode("smooth") } },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 0,
                            count = 2,
                            baseShape = RoundedCornerShape(8.dp)
                        )
                    ) {
                        Text(
                            stringResource(R.string.amoled_mode_smooth),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    SegmentedButton(
                        selected = refreshMode == "jump",
                        onClick = { scope.launch { preferenceManager.setAmoledRefreshMode("jump") } },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 1,
                            count = 2,
                            baseShape = RoundedCornerShape(8.dp)
                        )
                    ) {
                        Text(
                            stringResource(R.string.amoled_mode_jump),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

            // ── Filter Type Selection ──
            SettingsSection(title = stringResource(R.string.amoled_filter_type)) {
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

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterTypes.forEach { (key, label) ->
                        val isSelected = filterType == key
                        val containerColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            label = ""
                        )
                        val contentColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            label = ""
                        )

                        Surface(
                            onClick = { scope.launch { preferenceManager.setAmoledFilterType(key) } },
                            shape = RoundedCornerShape(8.dp),
                            color = containerColor,
                            contentColor = contentColor,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(
                                    alpha = 0.1f
                                )
                            )
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── Sliders Section ──
            SettingsSection(title = stringResource(R.string.filter_intensity)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Density
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.amoled_pixel_density).uppercase(),
                                modifier = Modifier.alpha(0.6f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
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
                    // Opacity
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.amoled_filter_opacity).uppercase(),
                                modifier = Modifier.alpha(0.6f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${(opacitySlider * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
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
                            }
                        )
                    }
                }
            }

            // ── Cycle Duration ──
            SettingsSection(title = stringResource(R.string.amoled_cycle_duration)) {
                val durations = listOf(
                    10 to stringResource(R.string.amoled_duration_10s),
                    30 to stringResource(R.string.amoled_duration_30s),
                    60 to stringResource(R.string.amoled_duration_1m),
                    120 to stringResource(R.string.amoled_duration_2m),
                    180 to stringResource(R.string.amoled_duration_3m),
                    300 to stringResource(R.string.amoled_duration_5m)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durations.forEach { (seconds, label) ->
                        val isSelected = cycleDuration == seconds
                        Surface(
                            onClick = { scope.launch { preferenceManager.setAmoledShiftSpeed(seconds) } },
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // ── Active Regions ──
            SettingsSection(title = stringResource(R.string.amoled_active_regions)) {
                val regions = listOf(
                    "full_screen" to stringResource(R.string.amoled_region_fullscreen),
                    "status_bar" to stringResource(R.string.amoled_region_statusbar),
                    "navigation_bar" to stringResource(R.string.amoled_region_navbar)
                )

                regions.forEach { (key, label) ->
                    val isChecked = activeRegions.contains(key)
                    Surface(
                        onClick = {
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
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}
