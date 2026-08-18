package io.github.itsmelissadev.swiftsense.feature.amoledscreenprotect

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.feature.alwaysondisplay.AccessibilityUtil
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
import io.github.itsmelissadev.swiftsense.ui.components.ShadcnDialog
import io.github.itsmelissadev.swiftsense.ui.components.ShadcnDialogButton
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSection
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSelectionDialog
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTextField
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTopAppBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

data class AmoledFilterSpec(
    val key: String,
    val label: String
)

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
    val customGapHeight by preferenceManager.amoledCustomGapHeight.collectAsState(initial = 0.40f)
    var customGapHeightSlider by remember(customGapHeight) { mutableFloatStateOf(customGapHeight) }
    val customPosition by preferenceManager.amoledCustomPosition.collectAsState(initial = 0.50f)
    var customPositionSlider by remember(customPosition) { mutableFloatStateOf(customPosition) }

    val tintEnabled by preferenceManager.amoledTintEnabled.collectAsState(initial = false)
    val tintColor by preferenceManager.amoledTintColor.collectAsState(initial = "amber")
    val tintCustomHex by preferenceManager.amoledTintCustomHex.collectAsState(initial = "#FFA500")
    val tintIntensity by preferenceManager.amoledTintIntensity.collectAsState(initial = 0.35f)
    var tintIntensitySlider by remember(tintIntensity) { mutableFloatStateOf(tintIntensity) }
    var showCustomColorDialog by remember { mutableStateOf(false) }

    var showRefreshModeDialog by remember { mutableStateOf(false) }
    var showCycleDurationDialog by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(AmoledProtectService.isServiceRunning) }

    LaunchedEffect(Unit) {
        while (isActive) {
            isRunning = AmoledProtectService.isServiceRunning || AccessibilityUtil.isAccessibilityServiceEnabled(context, AmoledProtectService::class.java)
            delay(500)
        }
    }

    val filterSpecs = listOf(
        AmoledFilterSpec("checker_grid", stringResource(R.string.amoled_filter_checker_grid)),
        AmoledFilterSpec("dots", stringResource(R.string.amoled_filter_dots)),
        AmoledFilterSpec("pentile_matrix", stringResource(R.string.amoled_filter_pentile)),
        AmoledFilterSpec("dynamic_inversion", stringResource(R.string.amoled_filter_inversion)),
        AmoledFilterSpec("blue_shield", stringResource(R.string.amoled_filter_blue_shield)),
        AmoledFilterSpec("noise", stringResource(R.string.amoled_filter_noise)),
        AmoledFilterSpec("pixel_shift", stringResource(R.string.amoled_filter_pixel_shift))
    )

    val durations = listOf(
        10 to stringResource(R.string.amoled_duration_10s),
        30 to stringResource(R.string.amoled_duration_30s),
        60 to stringResource(R.string.amoled_duration_1m),
        120 to stringResource(R.string.amoled_duration_2m),
        180 to stringResource(R.string.amoled_duration_3m),
        300 to stringResource(R.string.amoled_duration_5m)
    )

    val modes = listOf(
        "smooth" to stringResource(R.string.amoled_mode_smooth),
        "jump" to stringResource(R.string.amoled_mode_jump)
    )

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
                                        preferenceManager.setAmoledWarningDismissed(true)
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
                checked = isRunning,
                onCheckedChange = { targetState ->
                    if (AccessibilityUtil.isShizukuAvailable()) {
                        scope.launch {
                            val success = AccessibilityUtil.toggleAccessibilityServiceWithShizuku(
                                context,
                                AmoledProtectService::class.java,
                                targetState
                            )
                            if (!success) {
                                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        }
                    } else {
                        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            )

            SwiftSenseSection(title = stringResource(R.string.amoled_filter_settings)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        stringResource(R.string.amoled_filter_type),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filterSpecs) { spec ->
                            val isSelected = filterType == spec.key
                            AmoledFilterPreviewCard(
                                spec = spec,
                                isSelected = isSelected,
                                intensity = densitySlider,
                                filterOpacity = opacitySlider,
                                tintEnabled = tintEnabled,
                                tintColor = tintColor,
                                tintCustomHex = tintCustomHex,
                                tintIntensity = tintIntensitySlider,
                                onClick = {
                                    scope.launch { preferenceManager.setAmoledFilterType(spec.key) }
                                }
                            )
                        }
                    }

                    val durSelected = durations.find { it.first == cycleDuration }?.second ?: ""
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .clickable { showCycleDurationDialog = true },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        stringResource(R.string.amoled_cycle_duration),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        durSelected,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    Icons.Default.Info,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val modeSelected = modes.find { it.first == refreshMode }?.second ?: ""
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .clickable { showRefreshModeDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    stringResource(R.string.amoled_refresh_mode),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    modeSelected,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                Icons.Default.Info,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = if (refreshMode == "smooth") stringResource(R.string.amoled_mode_smooth_desc) else stringResource(R.string.amoled_mode_jump_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                                    preferenceManager.setAmoledIntensity(densitySlider)
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
                                    preferenceManager.setAmoledOpacity(opacitySlider)
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

            SwiftSenseSection(title = stringResource(R.string.amoled_tint_section_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    preferenceManager.setAmoledTintEnabled(!tintEnabled)
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                stringResource(R.string.amoled_tint_switch_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                stringResource(R.string.amoled_tint_switch_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = tintEnabled,
                            onCheckedChange = { active ->
                                scope.launch {
                                    preferenceManager.setAmoledTintEnabled(active)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (tintEnabled) {
                        Text(
                            stringResource(R.string.amoled_filter_type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        val tintPresets = listOf(
                            Triple("amber", stringResource(R.string.amoled_tint_preset_amber), Color(0xFFFFA500)),
                            Triple("red", stringResource(R.string.amoled_tint_preset_red), Color(0xFFFF2A2A)),
                            Triple("sepia", stringResource(R.string.amoled_tint_preset_sepia), Color(0xFFE8A858)),
                            Triple("dimmer", stringResource(R.string.amoled_tint_preset_dimmer), Color(0xFF333333)),
                            Triple("custom", stringResource(R.string.amoled_tint_preset_custom), Color(android.graphics.Color.parseColor(try { tintCustomHex } catch (_: Exception) { "#FFA500" })))
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tintPresets.forEach { (key, label, presetColor) ->
                                val isSelected = tintColor == key
                                Surface(
                                    onClick = {
                                        if (key == "custom") {
                                            showCustomColorDialog = true
                                        } else {
                                            scope.launch { preferenceManager.setAmoledTintColor(key) }
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(presetColor)
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.amoled_tint_intensity),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${(tintIntensitySlider * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Slider(
                                value = tintIntensitySlider,
                                onValueChange = { tintIntensitySlider = it },
                                onValueChangeFinished = {
                                    scope.launch {
                                        preferenceManager.setAmoledTintIntensity(tintIntensitySlider)
                                    }
                                },
                                valueRange = 0.05f..0.85f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            SwiftSenseSection(title = stringResource(R.string.amoled_active_regions)) {
                val regions = listOf(
                    "full_screen" to stringResource(R.string.amoled_region_fullscreen),
                    "status_bar" to stringResource(R.string.amoled_region_statusbar),
                    "navigation_bar" to stringResource(R.string.amoled_region_navbar),
                    "custom" to stringResource(R.string.amoled_region_custom)
                )
                val toggleRegion = { key: String, isCurrentlyChecked: Boolean ->
                    scope.launch {
                        val newRegions = when {
                            isCurrentlyChecked -> {
                                if (key == "status_bar" && activeRegions.contains("navigation_bar")) {
                                    setOf("navigation_bar")
                                } else if (key == "navigation_bar" && activeRegions.contains("status_bar")) {
                                    setOf("status_bar")
                                } else {
                                    activeRegions
                                }
                            }
                            else -> {
                                when (key) {
                                    "full_screen" -> setOf("full_screen")
                                    "custom" -> setOf("custom")
                                    "status_bar" -> {
                                        val set = mutableSetOf("status_bar")
                                        if (activeRegions.contains("navigation_bar")) {
                                            set.add("navigation_bar")
                                        }
                                        set
                                    }
                                    "navigation_bar" -> {
                                        val set = mutableSetOf("navigation_bar")
                                        if (activeRegions.contains("status_bar")) {
                                            set.add("status_bar")
                                        }
                                        set
                                    }
                                    else -> setOf(key)
                                }
                            }
                        }
                        preferenceManager.setAmoledRegions(newRegions)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    regions.forEach { (key, label) ->
                        val isChecked = activeRegions.contains(key)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleRegion(key, isChecked) }
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
                                onCheckedChange = { toggleRegion(key, isChecked) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    if (activeRegions.contains("custom")) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        AmoledCustomAreaPreview(
                            gapRatio = customGapHeightSlider,
                            positionRatio = customPositionSlider
                        )

                        val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
                        val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()

                        val aspectRatios = listOf(
                            "16:9" to ((screenWidth * 9f / 16f) / screenHeight).coerceIn(0.10f, 0.90f),
                            "18:9" to ((screenWidth * 9f / 18f) / screenHeight).coerceIn(0.10f, 0.90f),
                            "21:9" to ((screenWidth * 9f / 21f) / screenHeight).coerceIn(0.10f, 0.90f),
                            "4:3" to ((screenWidth * 3f / 4f) / screenHeight).coerceIn(0.10f, 0.90f),
                            "1:1" to (screenWidth / screenHeight).coerceIn(0.10f, 0.90f)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    stringResource(R.string.amoled_aspect_ratio),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    aspectRatios.forEach { (label, ratio) ->
                                        val isSelected = kotlin.math.abs(customGapHeightSlider - ratio) < 0.015f
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                )
                                                .clickable {
                                                    customGapHeightSlider = ratio
                                                    customPositionSlider = 0.50f
                                                    scope.launch {
                                                        preferenceManager.setAmoledCustomGapHeight(ratio)
                                                        preferenceManager.setAmoledCustomPosition(0.50f)
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.amoled_custom_gap_height),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${(customGapHeightSlider * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    stringResource(R.string.amoled_custom_gap_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Slider(
                                    value = customGapHeightSlider,
                                    onValueChange = { customGapHeightSlider = it },
                                    onValueChangeFinished = {
                                        scope.launch {
                                            preferenceManager.setAmoledCustomGapHeight(customGapHeightSlider)
                                        }
                                    },
                                    valueRange = 0.10f..0.90f,
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
                                        stringResource(R.string.amoled_custom_position),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${(customPositionSlider * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    stringResource(R.string.amoled_custom_position_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Slider(
                                    value = customPositionSlider,
                                    onValueChange = { customPositionSlider = it },
                                    onValueChangeFinished = {
                                        scope.launch {
                                            preferenceManager.setAmoledCustomPosition(customPositionSlider)
                                        }
                                    },
                                    valueRange = 0.0f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showRefreshModeDialog) {
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

    if (showCycleDurationDialog) {
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

    if (showCustomColorDialog) {
        val customPresets = listOf(
            "#FFA500", "#FF5722", "#E53935", "#8E24AA",
            "#1E88E5", "#00897B", "#43A047", "#FDD835", "#6D4C41", "#212121"
        )
        var selectedHex by remember { mutableStateOf(tintCustomHex) }
        ShadcnDialog(
            onDismissRequest = { showCustomColorDialog = false },
            title = stringResource(R.string.amoled_tint_custom_color_title),
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    try {
                                        Color(android.graphics.Color.parseColor(selectedHex))
                                    } catch (_: Exception) {
                                        Color(0xFFFFA500)
                                    }
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        )
                        SwiftSenseTextField(
                            value = selectedHex,
                            onValueChange = { selectedHex = it },
                            label = stringResource(R.string.hex_color_label),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.amoled_preset_colors),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            customPresets.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            if (selectedHex.equals(hex, ignoreCase = true)) 2.dp else 1.dp,
                                            if (selectedHex.equals(hex, ignoreCase = true)) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { selectedHex = hex }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                ShadcnDialogButton(
                    text = stringResource(R.string.action_save),
                    isPrimary = true,
                    onClick = {
                        scope.launch {
                            preferenceManager.setAmoledTintCustomHex(selectedHex)
                            preferenceManager.setAmoledTintColor("custom")
                        }
                        showCustomColorDialog = false
                    }
                )
            },
            dismissButton = {
                ShadcnDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { showCustomColorDialog = false }
                )
            }
        )
    }
}

@Composable
fun AmoledFilterPreviewCard(
    spec: AmoledFilterSpec,
    isSelected: Boolean,
    intensity: Float,
    filterOpacity: Float,
    tintEnabled: Boolean = false,
    tintColor: String = "amber",
    tintCustomHex: String = "#FFA500",
    tintIntensity: Float = 0.35f,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2A)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val overlayAlpha = (filterOpacity * 0.95f).coerceIn(0.1f, 1f)
    val dotSpacing = (1f - intensity * 0.92f).coerceIn(0.06f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp, 110.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                .background(Color.Black)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                if (tintEnabled && tintIntensity > 0f) {
                    val tintColorParsed = when (tintColor) {
                        "amber" -> Color(0xFFFFA500)
                        "red" -> Color(0xFFFF2A2A)
                        "sepia" -> Color(0xFFE8A858)
                        "dimmer" -> Color(0xFF222222)
                        "custom" -> {
                            try {
                                Color(android.graphics.Color.parseColor(tintCustomHex))
                            } catch (_: Exception) {
                                Color(0xFFFFA500)
                            }
                        }
                        else -> Color(0xFFFFA500)
                    }
                    drawRect(
                        color = tintColorParsed.copy(alpha = (tintIntensity * 0.7f).coerceIn(0.1f, 0.9f)),
                        topLeft = Offset.Zero,
                        size = size
                    )
                }

                val step = (w * dotSpacing * 0.18f).coerceAtLeast(1.5f)
                val color = Color.White.copy(alpha = overlayAlpha)

                when (spec.key) {
                    "checker_grid" -> {
                        var row = 0
                        var y = 0f
                        while (y < h) {
                            var x = if (row % 2 == 0) 0f else step
                            while (x < w) {
                                drawCircle(color = color, radius = 1f, center = Offset(x, y))
                                x += step * 2f
                            }
                            y += step
                            row++
                        }
                    }
                    "dots" -> {
                        var y = 0f
                        while (y < h) {
                            var x = 0f
                            while (x < w) {
                                drawCircle(color = color, radius = 1.2f, center = Offset(x, y))
                                x += step * 1.5f
                            }
                            y += step * 1.5f
                        }
                    }
                    "pentile_matrix" -> {
                        var row = 0
                        var y = 0f
                        while (y < h) {
                            var x = if (row % 2 == 0) 0f else step * 1.2f
                            while (x < w) {
                                drawCircle(color = color, radius = 1.1f, center = Offset(x, y))
                                x += step * 2.4f
                            }
                            y += step * 1.2f
                            row++
                        }
                    }
                    "dynamic_inversion" -> {
                        var row = 0
                        var y = 0f
                        while (y < h) {
                            var x = if (row % 2 == 0) 0f else step
                            while (x < w) {
                                drawRect(
                                    color = color,
                                    topLeft = Offset(x, y),
                                    size = androidx.compose.ui.geometry.Size(step.coerceAtLeast(1f), step.coerceAtLeast(1f))
                                )
                                x += step * 2f
                            }
                            y += step
                            row++
                        }
                    }
                    "blue_shield" -> {
                        var row = 0
                        var y = 0f
                        val amberColor = Color(0xFFFFB300).copy(alpha = overlayAlpha * 0.7f)
                        while (y < h) {
                            var x = 0f
                            while (x < w) {
                                val c = if ((row + (x / step).toInt()) % 2 == 0) color else amberColor
                                drawCircle(color = c, radius = 1f, center = Offset(x, y))
                                x += step * 1.5f
                            }
                            y += step * 1.5f
                            row++
                        }
                    }
                    "noise" -> {
                        val rng = Random(42)
                        val count = (w * h * intensity * 0.08f).toInt().coerceIn(10, 800)
                        repeat(count) {
                            val nx = rng.nextFloat() * w
                            val ny = rng.nextFloat() * h
                            drawCircle(color = color, radius = 0.8f, center = Offset(nx, ny))
                        }
                    }
                    "pixel_shift" -> {
                        val blockSize = step * 1.2f
                        val rng = Random(7)
                        var y = 0f
                        while (y < h) {
                            var x = 0f
                            while (x < w) {
                                if (rng.nextBoolean()) {
                                    drawRect(
                                        color = color,
                                        topLeft = Offset(x, y),
                                        size = androidx.compose.ui.geometry.Size(blockSize.coerceAtLeast(1f), blockSize.coerceAtLeast(1f))
                                    )
                                }
                                x += blockSize * 2f
                            }
                            y += blockSize * 2f
                        }
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Text(
            text = spec.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
fun AmoledCustomAreaPreview(
    gapRatio: Float,
    positionRatio: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(128.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val gapH = h * gapRatio
                    val center = h * positionRatio
                    val topH = (center - gapH / 2f).coerceIn(0f, h - gapH)
                    val bottomH = (h - (topH + gapH)).coerceAtLeast(0f)

                    if (topH > 0f) {
                        drawRect(
                            color = Color(0xFF3B82F6).copy(alpha = 0.45f),
                            topLeft = Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(w, topH)
                        )
                        var y = 0f
                        while (y < topH) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.3f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                            y += 4f
                        }
                    }

                    val gapTop = topH
                    drawRect(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        topLeft = Offset(0f, gapTop),
                        size = androidx.compose.ui.geometry.Size(w, gapH)
                    )

                    if (bottomH > 0f) {
                        val bottomTop = topH + gapH
                        drawRect(
                            color = Color(0xFF3B82F6).copy(alpha = 0.45f),
                            topLeft = Offset(0f, bottomTop),
                            size = androidx.compose.ui.geometry.Size(w, bottomH)
                        )
                        var y = bottomTop
                        while (y < h) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.3f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                            y += 4f
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.amoled_custom_preview),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6))
                    )
                    Text(
                        text = stringResource(R.string.amoled_custom_protected_area),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = stringResource(R.string.amoled_custom_video_area),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
