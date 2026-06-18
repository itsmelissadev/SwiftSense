package io.github.itsmelissadev.swiftsense.feature.screenresolution

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.shizuku.ShizukuShellRunner
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
import io.github.itsmelissadev.swiftsense.ui.components.ShadcnDialog
import io.github.itsmelissadev.swiftsense.ui.components.ShadcnDialogButton
import io.github.itsmelissadev.swiftsense.ui.components.ShizukuStatusWidget
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseButton
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseOutlinedButton
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSection
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTextField
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import rikka.shizuku.Shizuku

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenResolutionScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var physicalWidth by rememberSaveable { mutableIntStateOf(0) }
    var physicalHeight by rememberSaveable { mutableIntStateOf(0) }
    var physicalDpi by rememberSaveable { mutableIntStateOf(0) }

    var currentWidth by rememberSaveable { mutableIntStateOf(0) }
    var currentHeight by rememberSaveable { mutableIntStateOf(0) }
    var currentDpi by rememberSaveable { mutableIntStateOf(0) }

    var inputWidth by rememberSaveable { mutableStateOf("") }
    var inputHeight by rememberSaveable { mutableStateOf("") }
    var inputDpi by rememberSaveable { mutableStateOf("") }

    var maintainAspectRatio by rememberSaveable { mutableStateOf(true) }
    var showConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    var countdown by rememberSaveable { mutableIntStateOf(10) }

    var showSavePlanDialog by remember { mutableStateOf(false) }
    var planNameInput by remember { mutableStateOf("") }
    val savedPlans by preferenceManager.resolutionPlans.collectAsState(initial = emptySet())

    fun refreshDisplayInfo() {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        currentWidth = metrics.widthPixels
        currentHeight = metrics.heightPixels
        currentDpi = metrics.densityDpi

        if (!showConfirmationDialog) {
            inputWidth = currentWidth.toString()
            inputHeight = currentHeight.toString()
            inputDpi = currentDpi.toString()
        }

        scope.launch(Dispatchers.IO) {
            val sizeResult = ShizukuShellRunner.runCommand("wm size")
            var pWidth = currentWidth
            var pHeight = currentHeight
            sizeResult.onSuccess { output ->
                if (output.contains("Physical size:")) {
                    val size =
                        output.substringAfter("Physical size:").trim().split("\n")[0].split("x")
                    pWidth = size[0].toInt()
                    pHeight = size[1].toInt()
                }
            }

            val densityResult = ShizukuShellRunner.runCommand("wm density")
            var pDpi = currentDpi
            densityResult.onSuccess { output ->
                if (output.contains("Physical density:")) {
                    pDpi = output.substringAfter("Physical density:").trim().split("\n")[0].toInt()
                }
            }

            withContext(Dispatchers.Main) {
                physicalWidth = pWidth
                physicalHeight = pHeight
                physicalDpi = pDpi
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshDisplayInfo()
    }

    LaunchedEffect(showConfirmationDialog) {
        if (showConfirmationDialog) {
            while (countdown > 0 && showConfirmationDialog) {
                delay(1000)
                countdown--
            }
            if (showConfirmationDialog) {
                resetResolution()
                showConfirmationDialog = false
                refreshDisplayInfo()
            }
        }
    }

    if (showSavePlanDialog) {
        ShadcnDialog(
            onDismissRequest = { showSavePlanDialog = false },
            title = stringResource(R.string.save_plan),
            content = {
                SwiftSenseTextField(
                    value = planNameInput,
                    onValueChange = { planNameInput = it },
                    label = stringResource(R.string.plan_name)
                )
            },
            confirmButton = {
                ShadcnDialogButton(
                    text = stringResource(R.string.save_plan),
                    onClick = {
                        val planJson = JSONObject().apply {
                            put("name", planNameInput)
                            put("width", inputWidth)
                            put("height", inputHeight)
                            put("dpi", inputDpi)
                        }.toString()
                        scope.launch {
                            preferenceManager.addResolutionPlan(planJson)
                            showSavePlanDialog = false
                            planNameInput = ""
                            Toast.makeText(context, R.string.toast_plan_saved, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                )
            },
            dismissButton = {
                ShadcnDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { showSavePlanDialog = false }
                )
            }
        )
    }

    if (showConfirmationDialog) {
        ShadcnDialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = stringResource(R.string.confirm_resolution_title),
            description = stringResource(R.string.confirm_resolution_desc, countdown),
            confirmButton = {
                ShadcnDialogButton(
                    text = stringResource(R.string.action_keep_changes),
                    onClick = {
                        showConfirmationDialog = false
                        refreshDisplayInfo()
                    }
                )
            },
            dismissButton = {
                ShadcnDialogButton(
                    text = stringResource(R.string.action_revert),
                    isPrimary = false,
                    onClick = {
                        scope.launch {
                            resetResolution()
                            showConfirmationDialog = false
                            refreshDisplayInfo()
                        }
                    }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            SwiftSenseTopAppBar(
                title = stringResource(R.string.feature_screen_resolution),
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { refreshDisplayInfo() }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            ShizukuStatusWidget()

            SwiftSenseSection(title = stringResource(R.string.display_info)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.display_current),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${currentWidth} x ${currentHeight} (${currentDpi} DPI)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.display_physical),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${physicalWidth} x ${physicalHeight} (${physicalDpi} DPI)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            SwiftSenseSection(title = stringResource(R.string.resolution_settings)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FeatureCard(
                        title = stringResource(R.string.maintain_aspect_ratio),
                        description = if (maintainAspectRatio) stringResource(R.string.ratio_locked) else stringResource(
                            R.string.ratio_unlocked
                        ),
                        checked = maintainAspectRatio,
                        onCheckedChange = { maintainAspectRatio = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SwiftSenseTextField(
                            value = inputWidth,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() }) {
                                    inputWidth = newValue
                                    if (maintainAspectRatio && newValue.isNotEmpty() && physicalWidth > 0) {
                                        val ratio =
                                            physicalHeight.toFloat() / physicalWidth.toFloat()
                                        inputHeight = (newValue.toInt() * ratio).toInt().toString()
                                        val dpiRatio = newValue.toFloat() / physicalWidth.toFloat()
                                        inputDpi = (physicalDpi * dpiRatio).toInt().toString()
                                    }
                                }
                            },
                            label = stringResource(R.string.resolution_width),
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        SwiftSenseTextField(
                            value = inputHeight,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() }) {
                                    inputHeight = newValue
                                    if (maintainAspectRatio && newValue.isNotEmpty() && physicalHeight > 0) {
                                        val ratio =
                                            physicalWidth.toFloat() / physicalHeight.toFloat()
                                        inputWidth = (newValue.toInt() * ratio).toInt().toString()
                                        val dpiRatio = newValue.toFloat() / physicalHeight.toFloat()
                                        inputDpi = (physicalDpi * dpiRatio).toInt().toString()
                                    }
                                }
                            },
                            label = stringResource(R.string.resolution_height),
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    SwiftSenseTextField(
                        value = inputDpi,
                        onValueChange = { if (it.all { char -> char.isDigit() }) inputDpi = it },
                        label = stringResource(R.string.resolution_dpi),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            val isShizukuReady =
                Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SwiftSenseButton(
                        text = stringResource(R.string.action_apply),
                        onClick = {
                            scope.launch {
                                if (applyResolution(context, inputWidth, inputHeight, inputDpi)) {
                                    countdown = 10
                                    showConfirmationDialog = true
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isShizukuReady
                    )

                    SwiftSenseOutlinedButton(
                        text = stringResource(R.string.save_plan),
                        onClick = { showSavePlanDialog = true },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Save
                    )
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            resetResolution()
                            refreshDisplayInfo()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(6.dp),
                    enabled = isShizukuReady,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        stringResource(R.string.action_reset),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            SwiftSenseSection(title = stringResource(R.string.resolution_plans)) {
                if (savedPlans.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_plans),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        savedPlans.forEach { planJson ->
                            val plan = JSONObject(planJson)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                                ),
                                onClick = {
                                    inputWidth = plan.getString("width")
                                    inputHeight = plan.getString("height")
                                    inputDpi = plan.getString("dpi")
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 12.dp
                                    ).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = plan.getString("name"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "${plan.getString("width")}x${
                                                    plan.getString(
                                                        "height"
                                                    )
                                                } • ${plan.getString("dpi")} DPI",
                                                modifier = Modifier.padding(
                                                    horizontal = 6.dp,
                                                    vertical = 2.dp
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                preferenceManager.deleteResolutionPlan(planJson)
                                                Toast.makeText(
                                                    context,
                                                    R.string.toast_plan_deleted,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isShizukuReady) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = stringResource(R.string.shizuku_required_desc),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private suspend fun applyResolution(
    context: Context,
    width: String,
    height: String,
    dpi: String
): Boolean {
    if (width.isEmpty() || height.isEmpty() || dpi.isEmpty()) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, R.string.toast_invalid_input, Toast.LENGTH_SHORT).show()
        }
        return false
    }
    return withContext(Dispatchers.IO) {
        val sizeResult = ShizukuShellRunner.runCommand("wm size ${width}x${height}")
        val densityResult = ShizukuShellRunner.runCommand("wm density $dpi")
        sizeResult.isSuccess && densityResult.isSuccess
    }
}

private suspend fun resetResolution() {
    withContext(Dispatchers.IO) {
        ShizukuShellRunner.runCommand("wm size reset")
        ShizukuShellRunner.runCommand("wm density reset")
    }
}
