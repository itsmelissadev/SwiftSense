package io.github.itsmelissadev.swiftsense.feature.alwaysondisplay

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Modifier
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
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseButton
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSelectionDialog
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTextField
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTopAppBar

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import kotlin.math.roundToInt

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
    val theme by preferenceManager.aodTheme.collectAsState(initial = "minimal")
    val textColor by preferenceManager.aodTextColor.collectAsState(initial = android.graphics.Color.WHITE)
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

    var showClockStyleDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showBurnInModeDialog by remember { mutableStateOf(false) }
    
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
                        stringResource(R.string.aod_theme_previews),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            AodThemePreviewItem(
                                clockStyle = "digital",
                                themeColor = textColor,
                                fontFamilyStr = fontFamily,
                                isSelected = clockStyle == "digital",
                                onClick = { scope.launch { preferenceManager.setAodClockStyle("digital") } }
                            )
                        }
                        item {
                            AodThemePreviewItem(
                                clockStyle = "analog",
                                themeColor = textColor,
                                fontFamilyStr = fontFamily,
                                isSelected = clockStyle == "analog",
                                onClick = { scope.launch { preferenceManager.setAodClockStyle("analog") } }
                            )
                        }
                        item {
                            AodThemePreviewItem(
                                clockStyle = "modern_stacked",
                                themeColor = textColor,
                                fontFamilyStr = fontFamily,
                                isSelected = clockStyle == "modern_stacked",
                                onClick = { scope.launch { preferenceManager.setAodClockStyle("modern_stacked") } }
                            )
                        }
                        item {
                            AodThemePreviewItem(
                                clockStyle = "modern_glow",
                                themeColor = textColor,
                                fontFamilyStr = fontFamily,
                                isSelected = clockStyle == "modern_glow",
                                onClick = { scope.launch { preferenceManager.setAodClockStyle("modern_glow") } }
                            )
                        }
                    }
                    
                    Text(
                        stringResource(R.string.aod_text_color),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val presetColors = listOf(android.graphics.Color.WHITE, android.graphics.Color.RED, android.graphics.Color.GREEN, android.graphics.Color.BLUE, android.graphics.Color.YELLOW, android.graphics.Color.CYAN, android.graphics.Color.MAGENTA)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(presetColors) { colorInt ->
                            val isSelected = textColor == colorInt
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { scope.launch { preferenceManager.setAodTextColor(colorInt) } }
                            )
                        }
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
                    stringResource(R.string.aod_show_notifications) to showNotifications to { v: Boolean -> scope.launch { preferenceManager.setAodShowNotifications(v) } }
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

    if (showClockStyleDialog) {
        val clockStyles = listOf(
            "digital" to stringResource(R.string.aod_clock_style_digital),
            "analog_minimal" to stringResource(R.string.aod_clock_style_analog)
        )
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.aod_clock_style),
            options = clockStyles,
            selected = clockStyle,
            onSelectedChange = { style ->
                scope.launch { preferenceManager.setAodClockStyle(style) }
            },
            onDismissRequest = { showClockStyleDialog = false }
        )
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
}

@Composable
fun AodThemePreviewItem(
    clockStyle: String,
    themeColor: Int,
    fontFamilyStr: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val typeFace = when(fontFamilyStr) {
        "monospace" -> Typeface.MONOSPACE
        "serif" -> Typeface.SERIF
        else -> Typeface.DEFAULT
    }
    
    Box(
        modifier = Modifier
            .size(90.dp, 160.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .background(Color.Black)
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f - 20.dp.toPx()
            
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = themeColor
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    typeface = typeFace
                }
                
                when (clockStyle) {
                    "digital" -> {
                        paint.textSize = 24.dp.toPx()
                        canvas.nativeCanvas.drawText("12:34", cx, cy, paint)
                    }
                    "analog" -> {
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 2.dp.toPx()
                        paint.color = android.graphics.Color.argb(100, android.graphics.Color.red(themeColor), android.graphics.Color.green(themeColor), android.graphics.Color.blue(themeColor))
                        canvas.nativeCanvas.drawCircle(cx, cy, 30.dp.toPx(), paint)
                        paint.color = themeColor
                        paint.strokeWidth = 3.dp.toPx()
                        canvas.nativeCanvas.drawLine(cx, cy, cx, cy - 15.dp.toPx(), paint)
                        canvas.nativeCanvas.drawLine(cx, cy, cx + 15.dp.toPx(), cy, paint)
                    }
                    "modern_stacked" -> {
                        paint.textSize = 32.dp.toPx()
                        val textY = cy - 4.dp.toPx()
                        canvas.nativeCanvas.drawText("12", cx, textY, paint)
                        canvas.nativeCanvas.drawText("34", cx, textY + paint.descent() - paint.ascent() - 4.dp.toPx(), paint)
                    }
                    "modern_glow" -> {
                        paint.textSize = 28.dp.toPx()
                        paint.setShadowLayer(8f, 0f, 0f, android.graphics.Color.argb(200, android.graphics.Color.red(themeColor), android.graphics.Color.green(themeColor), android.graphics.Color.blue(themeColor)))
                        canvas.nativeCanvas.drawText("12:34", cx, cy, paint)
                        paint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                    }
                }
                
                paint.style = Paint.Style.FILL
                paint.textSize = 10.dp.toPx()
                val alphaColor = android.graphics.Color.argb(180, android.graphics.Color.red(themeColor), android.graphics.Color.green(themeColor), android.graphics.Color.blue(themeColor))
                paint.color = alphaColor
                canvas.nativeCanvas.drawText("Pzt, 15", cx, cy + 30.dp.toPx(), paint)
            }
        }
    }
}
