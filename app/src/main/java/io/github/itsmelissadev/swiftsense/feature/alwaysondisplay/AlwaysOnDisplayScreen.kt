package io.github.itsmelissadev.swiftsense.feature.alwaysondisplay

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSection
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSelectionDialog
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTextField
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTopAppBar
import android.graphics.Paint
import android.graphics.Typeface
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class AodStyleSpec(
    val id: String,
    val labelRes: Int
)

val AOD_CLOCK_STYLES = listOf(
    AodStyleSpec("digital", R.string.aod_clock_style_digital),
    AodStyleSpec("digital_glow_cyan", R.string.aod_clock_style_digital_glow_cyan),
    AodStyleSpec("modern_stacked", R.string.aod_clock_style_modern_stacked),
    AodStyleSpec("modern_glow", R.string.aod_clock_style_modern_glow),
    AodStyleSpec("analog", R.string.aod_clock_style_analog),
    AodStyleSpec("glow_rose", R.string.aod_clock_style_glow_rose),
    AodStyleSpec("digital_amber", R.string.aod_clock_style_digital_amber),
    AodStyleSpec("matrix", R.string.aod_clock_style_matrix),
    AodStyleSpec("minimal_dot", R.string.aod_clock_style_minimal_dot),
    AodStyleSpec("dual_tone", R.string.aod_clock_style_dual_tone)
)

val AOD_BATTERY_STYLES = listOf(
    AodStyleSpec("horizontal_classic", R.string.aod_battery_style_horizontal_classic),
    AodStyleSpec("vertical_classic", R.string.aod_battery_style_vertical_classic),
    AodStyleSpec("minimal_bar", R.string.aod_battery_style_minimal_bar),
    AodStyleSpec("circular", R.string.aod_battery_style_circular),
    AodStyleSpec("dotted", R.string.aod_battery_style_dotted),
    AodStyleSpec("pill", R.string.aod_battery_style_pill),
    AodStyleSpec("neon_outline", R.string.aod_battery_style_neon_outline),
    AodStyleSpec("segmented", R.string.aod_battery_style_segmented),
    AodStyleSpec("leaf", R.string.aod_battery_style_leaf),
    AodStyleSpec("text_only", R.string.aod_battery_style_text_only)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlwaysOnDisplayScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferenceManager = remember { PreferenceManager(context) }

    var isEnabled by remember { mutableStateOf(AccessibilityUtil.isAccessibilityServiceEnabled(context, AlwaysOnDisplayService::class.java)) }
    val showClock by preferenceManager.aodShowClock.collectAsState(initial = true)
    val showDate by preferenceManager.aodShowDate.collectAsState(initial = true)
    val showBattery by preferenceManager.aodShowBattery.collectAsState(initial = true)
    val showNotifications by preferenceManager.aodShowNotifications.collectAsState(initial = true)

    val clockStyle by preferenceManager.aodClockStyle.collectAsState(initial = "digital")
    val clockColor by preferenceManager.aodClockColor.collectAsState(initial = android.graphics.Color.WHITE)
    val batteryStyle by preferenceManager.aodBatteryStyle.collectAsState(initial = "horizontal_classic")
    val batteryColor by preferenceManager.aodBatteryColor.collectAsState(initial = android.graphics.Color.WHITE)
    
    val fontFamily by preferenceManager.aodFontFamily.collectAsState(initial = "monospace")
    val burnInProtection by preferenceManager.aodBurnInProtection.collectAsState(initial = true)
    val burnInInterval by preferenceManager.aodBurnInInterval.collectAsState(initial = 20)
    val burnInMode by preferenceManager.aodBurnInMode.collectAsState(initial = "jump")
    val burnInRgbShift by preferenceManager.aodBurnInRgbShift.collectAsState(initial = false)

    val brightness by preferenceManager.aodBrightness.collectAsState(initial = 0.5f)
    var brightnessSlider by remember(brightness) { mutableFloatStateOf(brightness) }
    var burnInSlider by remember(burnInInterval) { mutableFloatStateOf(burnInInterval.toFloat()) }

    val activateOnLock by preferenceManager.aodActivateOnLock.collectAsState(initial = true)
    val timeoutSeconds by preferenceManager.aodTimeoutSeconds.collectAsState(initial = 0)
    val warningDismissed by preferenceManager.aodWarningDismissed.collectAsState(initial = false)

    val dismissPowerButton by preferenceManager.aodDismissPowerButton.collectAsState(initial = true)
    val dismissDoubleTap by preferenceManager.aodDismissDoubleTap.collectAsState(initial = true)
    val showWattage by preferenceManager.aodShowWattage.collectAsState(initial = false)
    val aodAsForegroundService by preferenceManager.aodAsForegroundService.collectAsState(initial = false)

    var showFontDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showBurnInModeDialog by remember { mutableStateOf(false) }
    
    var showClockColorDialog by remember { mutableStateOf(false) }
    var showBatteryColorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (isActive) {
            isEnabled = AccessibilityUtil.isAccessibilityServiceEnabled(context, AlwaysOnDisplayService::class.java)
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            SwiftSenseTopAppBar(
                title = stringResource(R.string.feature_always_on_display),
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

            if (!warningDismissed) {
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
                                text = stringResource(R.string.aod_battery_warning_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    scope.launch { preferenceManager.setAodWarningDismissed(true) }
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
                            text = stringResource(R.string.aod_battery_warning_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            FeatureCard(
                title = stringResource(if (isEnabled) R.string.aod_status_active else R.string.aod_status_inactive),
                description = stringResource(if (isEnabled) R.string.service_status else R.string.feature_always_on_display_desc),
                icon = ImageVector.vectorResource(id = R.drawable.ic_aod_24px),
                containerColor = if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else null,
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )

            SwiftSenseSection(title = stringResource(R.string.aod_section_settings)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        stringResource(R.string.aod_clock_styles),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(AOD_CLOCK_STYLES) { spec ->
                            val isSelected = clockStyle == spec.id
                            AodStylePreviewCard(
                                labelRes = spec.labelRes,
                                isSelected = isSelected,
                                onClick = { scope.launch { preferenceManager.setAodClockStyle(spec.id) } },
                                content = { AodClockPreview(spec.id, clockColor, fontFamily) }
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showClockColorDialog = true }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.aod_clock_color), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(clockColor)).border(1.dp, Color.Gray.copy(alpha=0.5f), CircleShape))
                    }

                    Text(
                        stringResource(R.string.aod_battery_styles),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(AOD_BATTERY_STYLES) { spec ->
                            val isSelected = batteryStyle == spec.id
                            AodStylePreviewCard(
                                labelRes = spec.labelRes,
                                isSelected = isSelected,
                                onClick = { scope.launch { preferenceManager.setAodBatteryStyle(spec.id) } },
                                content = { AodBatteryPreview(spec.id, batteryColor) }
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showBatteryColorDialog = true }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.aod_battery_color), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(batteryColor)).border(1.dp, Color.Gray.copy(alpha=0.5f), CircleShape))
                    }

                    val fonts = listOf(
                        "default" to stringResource(R.string.aod_font_default),
                        "monospace" to stringResource(R.string.aod_font_monospace),
                        "serif" to stringResource(R.string.aod_font_serif)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SwiftSenseTextField(
                            value = fonts.find { it.first == fontFamily }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.aod_font_family),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showFontDialog = true }
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.aod_brightness),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${(brightnessSlider * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Slider(
                            value = brightnessSlider,
                            onValueChange = { brightnessSlider = it },
                            onValueChangeFinished = {
                                scope.launch {
                                    preferenceManager.setAodBrightness(brightnessSlider)
                                }
                            },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            SwiftSenseSection(title = stringResource(R.string.aod_section_content)) {
                val toggles = listOf(
                    stringResource(R.string.aod_show_clock) to showClock to { v: Boolean -> scope.launch { preferenceManager.setAodShowClock(v) } },
                    stringResource(R.string.aod_show_date) to showDate to { v: Boolean -> scope.launch { preferenceManager.setAodShowDate(v) } },
                    stringResource(R.string.aod_show_battery) to showBattery to { v: Boolean -> scope.launch { preferenceManager.setAodShowBattery(v) } },
                    stringResource(R.string.aod_show_notifications) to showNotifications to { v: Boolean -> scope.launch { preferenceManager.setAodShowNotifications(v) } },
                    stringResource(R.string.aod_show_wattage) to showWattage to { v: Boolean -> scope.launch { preferenceManager.setAodShowWattage(v) } }
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    toggles.forEach { toggleInfo ->
                        val label = toggleInfo.first.first
                        val isChecked = toggleInfo.first.second
                        val onChange = toggleInfo.second

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChange(!isChecked) }
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
                                onCheckedChange = { onChange(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
            SwiftSenseSection(title = stringResource(R.string.aod_burn_in_protection)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope.launch { preferenceManager.setAodBurnInProtection(!burnInProtection) } }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                stringResource(R.string.aod_burn_in_protection),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (burnInProtection) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (burnInProtection) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.aod_burn_in_protection_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Switch(
                            checked = burnInProtection,
                            onCheckedChange = { scope.launch { preferenceManager.setAodBurnInProtection(it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    if (burnInProtection) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.aod_burn_in_interval),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${burnInSlider.roundToInt()}s",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Slider(
                                value = burnInSlider,
                                onValueChange = { burnInSlider = it },
                                onValueChangeFinished = {
                                    scope.launch {
                                        preferenceManager.setAodBurnInInterval(burnInSlider.roundToInt())
                                    }
                                },
                                valueRange = 10f..30f,
                                steps = 19,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            val modes = listOf(
                                "jump" to stringResource(R.string.aod_burn_in_mode_jump),
                                "bounce" to stringResource(R.string.aod_burn_in_mode_bounce)
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                SwiftSenseTextField(
                                    value = modes.find { it.first == burnInMode }?.second ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = stringResource(R.string.aod_burn_in_mode),
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showBurnInModeDialog = true }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stringResource(R.string.aod_burn_in_rgb_shift),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Switch(
                                    checked = burnInRgbShift,
                                    onCheckedChange = { scope.launch { preferenceManager.setAodBurnInRgbShift(it) } },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            SwiftSenseSection(title = stringResource(R.string.aod_section_behavior)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope.launch { preferenceManager.setAodActivateOnLock(!activateOnLock) } }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.aod_activate_on_lock),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (activateOnLock) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (activateOnLock) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = activateOnLock,
                            onCheckedChange = { scope.launch { preferenceManager.setAodActivateOnLock(it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope.launch { preferenceManager.setAodDismissPowerButton(!dismissPowerButton) } }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.aod_dismiss_power_button),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (dismissPowerButton) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (dismissPowerButton) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = dismissPowerButton,
                            onCheckedChange = { scope.launch { preferenceManager.setAodDismissPowerButton(it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope.launch { preferenceManager.setAodDismissDoubleTap(!dismissDoubleTap) } }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.aod_dismiss_double_tap),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (dismissDoubleTap) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (dismissDoubleTap) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = dismissDoubleTap,
                            onCheckedChange = { scope.launch { preferenceManager.setAodDismissDoubleTap(it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope.launch { preferenceManager.setAodAsForegroundService(!aodAsForegroundService) } }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.aod_run_as_foreground_service),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (aodAsForegroundService) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (aodAsForegroundService) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.aod_run_as_foreground_service_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = aodAsForegroundService,
                            onCheckedChange = { scope.launch { preferenceManager.setAodAsForegroundService(it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    val timeouts = listOf(
                        0 to stringResource(R.string.aod_timeout_never),
                        30 to stringResource(R.string.aod_timeout_30s),
                        60 to stringResource(R.string.aod_timeout_1m)
                    )
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        SwiftSenseTextField(
                            value = timeouts.find { it.first == timeoutSeconds }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.aod_timeout),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTimeoutDialog = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showFontDialog) {
        val fonts = listOf(
            "default" to stringResource(R.string.aod_font_default),
            "monospace" to stringResource(R.string.aod_font_monospace),
            "serif" to stringResource(R.string.aod_font_serif)
        )
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.aod_font_family),
            options = fonts,
            selected = fontFamily,
            onSelectedChange = { f ->
                scope.launch { preferenceManager.setAodFontFamily(f) }
            },
            onDismissRequest = { showFontDialog = false }
        )
    }

    if (showTimeoutDialog) {
        val timeouts = listOf(
            0 to stringResource(R.string.aod_timeout_never),
            30 to stringResource(R.string.aod_timeout_30s),
            60 to stringResource(R.string.aod_timeout_1m)
        )
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.aod_timeout),
            options = timeouts,
            selected = timeoutSeconds,
            onSelectedChange = { s ->
                scope.launch { preferenceManager.setAodTimeoutSeconds(s) }
            },
            onDismissRequest = { showTimeoutDialog = false }
        )
    }

    if (showBurnInModeDialog) {
        val modes = listOf(
            "jump" to stringResource(R.string.aod_burn_in_mode_jump),
            "bounce" to stringResource(R.string.aod_burn_in_mode_bounce)
        )
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.aod_burn_in_mode),
            options = modes,
            selected = burnInMode,
            onSelectedChange = { m ->
                scope.launch { preferenceManager.setAodBurnInMode(m) }
            },
            onDismissRequest = { showBurnInModeDialog = false }
        )
    }

    if (showClockColorDialog) {
        io.github.itsmelissadev.swiftsense.ui.components.ColorPickerDialog(
            title = stringResource(R.string.aod_clock_color),
            initialColor = clockColor,
            onColorSelected = { scope.launch { preferenceManager.setAodClockColor(it) } },
            onDismissRequest = { showClockColorDialog = false }
        )
    }

    if (showBatteryColorDialog) {
        io.github.itsmelissadev.swiftsense.ui.components.ColorPickerDialog(
            title = stringResource(R.string.aod_battery_color),
            initialColor = batteryColor,
            onColorSelected = { scope.launch { preferenceManager.setAodBatteryColor(it) } },
            onDismissRequest = { showBatteryColorDialog = false }
        )
    }
}

@Composable
fun AodStylePreviewCard(
    @StringRes labelRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2A)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp, 120.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
                .background(Color.Black)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            content()
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(16.dp)
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
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
fun AodClockPreview(clockStyle: String, tc: Int, fontFamily: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = android.graphics.Color.red(tc)
        val g = android.graphics.Color.green(tc)
        val b = android.graphics.Color.blue(tc)

        val typeFace = when (fontFamily) {
            "monospace" -> Typeface.MONOSPACE
            "serif" -> Typeface.SERIF
            else -> Typeface.DEFAULT
        }

        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = tc
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = typeFace
            }

            when (clockStyle) {
                "digital" -> {
                    paint.textSize = 26.dp.toPx()
                    paint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                    canvas.nativeCanvas.drawText("12:34", cx, cy, paint)
                }
                "digital_glow_cyan" -> {
                    paint.textSize = 26.dp.toPx()
                    paint.setShadowLayer(18f, 0f, 0f, android.graphics.Color.argb(200, r, g, b))
                    canvas.nativeCanvas.drawText("12:34", cx, cy, paint)
                    paint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                }
                "modern_stacked" -> {
                    paint.textSize = 34.dp.toPx()
                    paint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                    val lineH = paint.descent() - paint.ascent()
                    val topY = cy - lineH * 0.5f - 2.dp.toPx()
                    canvas.nativeCanvas.drawText("12", cx, topY, paint)
                    canvas.nativeCanvas.drawText("34", cx, topY + lineH - 4.dp.toPx(), paint)
                }
                "modern_glow" -> {
                    paint.textSize = 26.dp.toPx()
                    paint.setShadowLayer(14f, 0f, 0f, android.graphics.Color.argb(220, r, g, b))
                    canvas.nativeCanvas.drawText("12:34", cx, cy, paint)
                    paint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                }
                "analog" -> {
                    val radius = 28.dp.toPx()
                    val circlePaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f.dp.toPx()
                        color = android.graphics.Color.argb(80, r, g, b)
                    }
                    canvas.nativeCanvas.drawCircle(cx, cy, radius, circlePaint)
                    val tickPaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeWidth = 1f.dp.toPx()
                        color = android.graphics.Color.argb(100, r, g, b)
                    }
                    for (i in 0..11) {
                        val angle = Math.toRadians(i * 30.0)
                        canvas.nativeCanvas.drawLine(
                            cx + (radius * 0.86f * sin(angle)).toFloat(),
                            cy - (radius * 0.86f * cos(angle)).toFloat(),
                            cx + (radius * 0.96f * sin(angle)).toFloat(),
                            cy - (radius * 0.96f * cos(angle)).toFloat(),
                            tickPaint
                        )
                    }
                    val handPaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                    }
                    handPaint.strokeWidth = 3f.dp.toPx()
                    handPaint.color = tc
                    canvas.nativeCanvas.drawLine(cx, cy, cx + (radius * 0.5f * sin(Math.toRadians(60.0))).toFloat(), cy - (radius * 0.5f * cos(Math.toRadians(60.0))).toFloat(), handPaint)
                    handPaint.strokeWidth = 2f.dp.toPx()
                    handPaint.color = android.graphics.Color.argb(200, r, g, b)
                    canvas.nativeCanvas.drawLine(cx, cy, cx + (radius * 0.75f * sin(Math.toRadians(150.0))).toFloat(), cy - (radius * 0.75f * cos(Math.toRadians(150.0))).toFloat(), handPaint)
                }
                "glow_rose" -> {
                    paint.textSize = 26.dp.toPx()
                    paint.setShadowLayer(16f, 0f, 0f, android.graphics.Color.argb(180, r, g, b))
                    canvas.nativeCanvas.drawText("12:34", cx, cy - 3.dp.toPx(), paint)
                    paint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                    val linePaint = Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.argb(100, r, g, b)
                        strokeWidth = 1f.dp.toPx()
                    }
                    canvas.nativeCanvas.drawLine(cx - 28.dp.toPx(), cy + 10.dp.toPx(), cx + 28.dp.toPx(), cy + 10.dp.toPx(), linePaint)
                }
                "digital_amber" -> {
                    paint.textSize = 24.dp.toPx()
                    paint.typeface = Typeface.SERIF
                    paint.setShadowLayer(6f, 0f, 0f, android.graphics.Color.argb(150, r, g, b))
                    canvas.nativeCanvas.drawText("12:34", cx, cy, paint)
                    paint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                }
                "matrix" -> {
                    paint.textSize = 24.dp.toPx()
                    paint.setShadowLayer(22f, 0f, 0f, android.graphics.Color.argb(255, r, g, b))
                    canvas.nativeCanvas.drawText("12:34", cx, cy, paint)
                    paint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                    paint.textSize = 7.dp.toPx()
                    paint.color = android.graphics.Color.argb(90, r, g, b)
                    val chars = listOf("0","1","0","1","0","1","0","1","0","1","0","1")
                    chars.forEachIndexed { i, c ->
                        val col = i % 4
                        val row = i / 4
                        canvas.nativeCanvas.drawText(c, cx - 15.dp.toPx() + col * 10.dp.toPx(), cy + 20.dp.toPx() + row * 10.dp.toPx(), paint)
                    }
                }
                "minimal_dot" -> {
                    paint.textSize = 24.dp.toPx()
                    paint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                    canvas.nativeCanvas.drawText("12:34", cx, cy, paint)
                    val dotPaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.FILL
                        color = android.graphics.Color.argb(100, r, g, b)
                    }
                    val dotY = cy + 14.dp.toPx()
                    canvas.nativeCanvas.drawCircle(cx - 18.dp.toPx(), dotY, 2.5f.dp.toPx(), dotPaint)
                    canvas.nativeCanvas.drawCircle(cx, dotY, 2.5f.dp.toPx(), dotPaint)
                    canvas.nativeCanvas.drawCircle(cx + 18.dp.toPx(), dotY, 2.5f.dp.toPx(), dotPaint)
                }
                "dual_tone" -> {
                    paint.textSize = 26.dp.toPx()
                    paint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                    val totalW = paint.measureText("00:00")
                    val charW = paint.measureText("00") / 2f
                    canvas.nativeCanvas.drawText("12", cx - totalW / 2f + charW, cy, paint)
                    val dimPaint = Paint(paint).apply {
                        color = android.graphics.Color.argb(110, r, g, b)
                    }
                    canvas.nativeCanvas.drawText(":", cx, cy - 1.dp.toPx(), dimPaint)
                    canvas.nativeCanvas.drawText("34", cx + totalW / 2f - charW, cy, dimPaint)
                }
                else -> {
                    paint.textSize = 26.dp.toPx()
                    paint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                    canvas.nativeCanvas.drawText("12:34", cx, cy, paint)
                }
            }
        }
    }
}

@Composable
fun AodBatteryPreview(batteryStyle: String, bc: Int) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = android.graphics.Color.red(bc)
        val g = android.graphics.Color.green(bc)
        val b = android.graphics.Color.blue(bc)
        val level = 72

        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = android.graphics.Color.argb(180, r, g, b)
                isAntiAlias = true
            }
            
            when (batteryStyle) {
                "horizontal_classic" -> {
                    val bw = 18f.dp.toPx()
                    val bh = 10f.dp.toPx()
                    val cw = 2f.dp.toPx()
                    val ch = 4f.dp.toPx()
                    val left = cx - bw/2f
                    val top = cy - bh / 2f
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f.dp.toPx()
                    canvas.nativeCanvas.drawRoundRect(left, top, left + bw, top + bh, 2f.dp.toPx(), 2f.dp.toPx(), paint)
                    paint.style = Paint.Style.FILL
                    canvas.nativeCanvas.drawRoundRect(left + bw, cy - ch / 2f, left + bw + cw, cy + ch / 2f, 1f.dp.toPx(), 1f.dp.toPx(), paint)
                    val lw = (bw - 3f.dp.toPx()) * (level / 100f)
                    canvas.nativeCanvas.drawRoundRect(left + 1.5f.dp.toPx(), top + 1.5f.dp.toPx(), left + 1.5f.dp.toPx() + lw, top + bh - 1.5f.dp.toPx(), 1f.dp.toPx(), 1f.dp.toPx(), paint)
                }
                "vertical_classic" -> {
                    val bw = 10f.dp.toPx()
                    val bh = 16f.dp.toPx()
                    val cw = 4f.dp.toPx()
                    val ch = 2f.dp.toPx()
                    val left = cx - bw/2f
                    val top = cy - bh / 2f
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f.dp.toPx()
                    canvas.nativeCanvas.drawRoundRect(left, top, left + bw, top + bh, 2f.dp.toPx(), 2f.dp.toPx(), paint)
                    paint.style = Paint.Style.FILL
                    canvas.nativeCanvas.drawRoundRect(left + bw / 2f - cw / 2f, top - ch, left + bw / 2f + cw / 2f, top, 1f.dp.toPx(), 1f.dp.toPx(), paint)
                    val lh = (bh - 3f.dp.toPx()) * (level / 100f)
                    canvas.nativeCanvas.drawRoundRect(left + 1.5f.dp.toPx(), top + bh - 1.5f.dp.toPx() - lh, left + bw - 1.5f.dp.toPx(), top + bh - 1.5f.dp.toPx(), 1f.dp.toPx(), 1f.dp.toPx(), paint)
                }
                "minimal_bar" -> {
                    val bw = 24f.dp.toPx()
                    val bh = 4f.dp.toPx()
                    val left = cx - bw/2f
                    val top = cy - bh / 2f
                    paint.style = Paint.Style.FILL
                    paint.color = android.graphics.Color.argb(80, r, g, b)
                    canvas.nativeCanvas.drawRoundRect(left, top, left + bw, top + bh, 2f.dp.toPx(), 2f.dp.toPx(), paint)
                    paint.color = android.graphics.Color.argb(180, r, g, b)
                    val lw = bw * (level / 100f)
                    canvas.nativeCanvas.drawRoundRect(left, top, left + lw, top + bh, 2f.dp.toPx(), 2f.dp.toPx(), paint)
                }
                "circular" -> {
                    val radius = 9f.dp.toPx()
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f.dp.toPx()
                    paint.color = android.graphics.Color.argb(80, r, g, b)
                    canvas.nativeCanvas.drawCircle(cx, cy, radius, paint)
                    paint.color = android.graphics.Color.argb(180, r, g, b)
                    val sweep = 360f * (level / 100f)
                    canvas.nativeCanvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, -90f, sweep, false, paint)
                }
                "dotted" -> {
                    val dots = 5
                    val activeDots = Math.ceil(((level / 100f) * dots).toDouble()).toInt()
                    val dotRadius = 1.5f.dp.toPx()
                    val spacing = 4.5f.dp.toPx()
                    paint.style = Paint.Style.FILL
                    val startX = cx - (dots * spacing) / 2f
                    for (i in 0 until dots) {
                        paint.color = android.graphics.Color.argb(if (i < activeDots) 180 else 80, r, g, b)
                        canvas.nativeCanvas.drawCircle(startX + dotRadius + i * spacing, cy, dotRadius, paint)
                    }
                }
                "pill" -> {
                    val bw = 22f.dp.toPx()
                    val bh = 10f.dp.toPx()
                    val left = cx - bw/2f
                    val top = cy - bh / 2f
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f.dp.toPx()
                    canvas.nativeCanvas.drawRoundRect(left, top, left + bw, top + bh, bh / 2f, bh / 2f, paint)
                    paint.style = Paint.Style.FILL
                    val lw = (bw - 4f.dp.toPx()) * (level / 100f)
                    if (lw > 0) {
                        canvas.nativeCanvas.drawRoundRect(left + 2f.dp.toPx(), top + 2f.dp.toPx(), left + 2f.dp.toPx() + lw, top + bh - 2f.dp.toPx(), (bh - 4f.dp.toPx()) / 2f, (bh - 4f.dp.toPx()) / 2f, paint)
                    }
                }
                "neon_outline" -> {
                    val bw = 20f.dp.toPx()
                    val bh = 12f.dp.toPx()
                    val left = cx - bw/2f
                    val top = cy - bh / 2f
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1.5f.dp.toPx()
                    paint.setShadowLayer(8f.dp.toPx(), 0f, 0f, paint.color)
                    canvas.nativeCanvas.drawRoundRect(left, top, left + bw, top + bh, 3f.dp.toPx(), 3f.dp.toPx(), paint)
                    paint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                    paint.style = Paint.Style.FILL
                    val lw = (bw - 4f.dp.toPx()) * (level / 100f)
                    canvas.nativeCanvas.drawRoundRect(left + 2f.dp.toPx(), top + 2f.dp.toPx(), left + 2f.dp.toPx() + lw, top + bh - 2f.dp.toPx(), 1.5f.dp.toPx(), 1.5f.dp.toPx(), paint)
                }
                "segmented" -> {
                    val bw = 20f.dp.toPx()
                    val bh = 10f.dp.toPx()
                    val segments = 4
                    val left = cx - bw/2f
                    val top = cy - bh / 2f
                    val activeSegments = Math.ceil(((level / 100f) * segments).toDouble()).toInt()
                    val segW = (bw - (segments - 1) * 2f.dp.toPx()) / segments
                    paint.style = Paint.Style.FILL
                    for (i in 0 until segments) {
                        paint.color = android.graphics.Color.argb(if (i < activeSegments) 180 else 80, r, g, b)
                        val sx = left + i * (segW + 2f.dp.toPx())
                        canvas.nativeCanvas.drawRect(sx, top, sx + segW, top + bh, paint)
                    }
                }
                "leaf" -> {
                    val bw = 16f.dp.toPx()
                    val bh = 10f.dp.toPx()
                    val left = cx - bw/2f
                    val top = cy - bh / 2f
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1.5f.dp.toPx()
                    val path = android.graphics.Path()
                    path.moveTo(left, top + bh)
                    path.cubicTo(left, top, left + bw / 2f, top, left + bw, top)
                    path.cubicTo(left + bw, top + bh, left + bw / 2f, top + bh, left, top + bh)
                    canvas.nativeCanvas.drawPath(path, paint)
                    paint.style = Paint.Style.FILL
                    val lw = bw * (level / 100f)
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.clipRect(left, top, left + lw, top + bh)
                    canvas.nativeCanvas.drawPath(path, paint)
                    canvas.nativeCanvas.restore()
                }
                "text_only" -> {
                    paint.textSize = 14f.dp.toPx()
                    paint.textAlign = Paint.Align.CENTER
                    val lineH = paint.descent() + paint.ascent()
                    canvas.nativeCanvas.drawText("$level%", cx, cy - lineH/2f, paint)
                }
            }
        }
    }
}
