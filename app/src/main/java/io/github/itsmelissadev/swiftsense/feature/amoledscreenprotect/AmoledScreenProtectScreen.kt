package io.github.itsmelissadev.swiftsense.feature.amoledscreenprotect

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSection
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSelectionDialog
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
    var showRefreshModeDialog by remember { mutableStateOf(false) }
    var showCycleDurationDialog by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(AmoledProtectService.isServiceRunning) }

    LaunchedEffect(Unit) {
        while (isActive) {
            isRunning = AmoledProtectService.isServiceRunning
            delay(1000)
        }
    }

    val filterSpecs = listOf(
        AmoledFilterSpec("checker_grid", stringResource(R.string.amoled_filter_checker_grid)),
        AmoledFilterSpec("dots", stringResource(R.string.amoled_filter_dots)),
        AmoledFilterSpec("horizontal_lines", stringResource(R.string.amoled_filter_horizontal)),
        AmoledFilterSpec("vertical_lines", stringResource(R.string.amoled_filter_vertical)),
        AmoledFilterSpec("grid", stringResource(R.string.amoled_filter_grid)),
        AmoledFilterSpec("diagonal", stringResource(R.string.amoled_filter_diagonal)),
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
                onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
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
}

@Composable
fun AmoledFilterPreviewCard(
    spec: AmoledFilterSpec,
    isSelected: Boolean,
    intensity: Float,
    filterOpacity: Float,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2A)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val overlayAlpha = (filterOpacity * 0.85f).coerceIn(0.1f, 0.95f)
    val dotSpacing = (1f - intensity * 0.6f).coerceIn(0.15f, 1f)

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
                val step = (w * dotSpacing * 0.18f).coerceAtLeast(3f)
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
                    "horizontal_lines" -> {
                        var y = 0f
                        while (y < h) {
                            drawLine(
                                color = color,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                            y += step * 1.5f
                        }
                    }
                    "vertical_lines" -> {
                        var x = 0f
                        while (x < w) {
                            drawLine(
                                color = color,
                                start = Offset(x, 0f),
                                end = Offset(x, h),
                                strokeWidth = 1f
                            )
                            x += step * 1.5f
                        }
                    }
                    "grid" -> {
                        var y = 0f
                        while (y < h) {
                            drawLine(color = color, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 0.7f)
                            y += step * 1.5f
                        }
                        var x = 0f
                        while (x < w) {
                            drawLine(color = color, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 0.7f)
                            x += step * 1.5f
                        }
                    }
                    "diagonal" -> {
                        val diag = step * 2f
                        var offset = -h
                        while (offset < w + h) {
                            drawLine(
                                color = color,
                                start = Offset(offset, 0f),
                                end = Offset(offset + h, h),
                                strokeWidth = 0.8f
                            )
                            offset += diag
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
