package io.github.itsmelissadev.swiftsense.feature.screenresolution

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.shizuku.ShizukuShellRunner
import io.github.itsmelissadev.swiftsense.ui.components.ShadcnDialog
import io.github.itsmelissadev.swiftsense.ui.components.ShadcnDialogButton
import io.github.itsmelissadev.swiftsense.ui.components.ShizukuStatusWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

        val sizeResult = ShizukuShellRunner.runCommand("wm size")
        sizeResult.onSuccess { output ->
            if (output.contains("Physical size:")) {
                val size = output.substringAfter("Physical size:").trim().split("\n")[0].split("x")
                physicalWidth = size[0].toInt()
                physicalHeight = size[1].toInt()
            }
        }.onFailure {
            physicalWidth = currentWidth
            physicalHeight = currentHeight
        }

        val densityResult = ShizukuShellRunner.runCommand("wm density")
        densityResult.onSuccess { output ->
            if (output.contains("Physical density:")) {
                physicalDpi =
                    output.substringAfter("Physical density:").trim().split("\n")[0].toInt()
            }
        }.onFailure {
            physicalDpi = currentDpi
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
            if (showConfirmationDialog && countdown <= 0) {
                resetResolution(context)
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
                OutlinedTextField(
                    value = planNameInput,
                    onValueChange = { planNameInput = it },
                    label = {
                        Text(
                            stringResource(R.string.plan_name),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
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
                        resetResolution(context)
                        showConfirmationDialog = false
                        refreshDisplayInfo()
                    }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.feature_screen_resolution).uppercase(),
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
                actions = {
                    IconButton(onClick = { refreshDisplayInfo() }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
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
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            ShizukuStatusWidget()

            // Current Info Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        stringResource(
                            R.string.current_resolution,
                            currentWidth,
                            currentHeight,
                            currentDpi
                        ).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(
                            R.string.physical_resolution,
                            physicalWidth,
                            physicalHeight,
                            physicalDpi
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Adjustment Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.maintain_aspect_ratio).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                if (maintainAspectRatio) "LOCKED" else "UNLOCKED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = maintainAspectRatio,
                            onCheckedChange = { maintainAspectRatio = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
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
                            label = {
                                Text(
                                    stringResource(R.string.resolution_width),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
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
                            label = {
                                Text(
                                    stringResource(R.string.resolution_height),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = inputDpi,
                        onValueChange = { if (it.all { char -> char.isDigit() }) inputDpi = it },
                        label = {
                            Text(
                                stringResource(R.string.resolution_dpi),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            val isShizukuReady =
                Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (applyResolution(context, inputWidth, inputHeight, inputDpi)) {
                                countdown = 10; showConfirmationDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = isShizukuReady
                    ) {
                        Text(
                            stringResource(R.string.action_apply).uppercase(),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        onClick = { showSavePlanDialog = true },
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                OutlinedButton(
                    onClick = { resetResolution(context); refreshDisplayInfo() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = isShizukuReady,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        stringResource(R.string.action_reset).uppercase(),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Plans Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.resolution_plans).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (savedPlans.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.no_plans),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    savedPlans.forEach { planJson ->
                        val plan = JSONObject(planJson)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            ),
                            onClick = {
                                inputWidth = plan.getString("width")
                                inputHeight = plan.getString("height")
                                inputDpi = plan.getString("dpi")
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = plan.getString("name"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${plan.getString("width")}x${plan.getString("height")} • ${
                                            plan.getString(
                                                "dpi"
                                            )
                                        } DPI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isShizukuReady) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = stringResource(R.string.shizuku_required_desc),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun applyResolution(context: Context, width: String, height: String, dpi: String): Boolean {
    if (width.isEmpty() || height.isEmpty() || dpi.isEmpty()) {
        Toast.makeText(context, R.string.toast_invalid_input, Toast.LENGTH_SHORT).show()
        return false
    }
    val sizeResult = ShizukuShellRunner.runCommand("wm size ${width}x${height}")
    val densityResult = ShizukuShellRunner.runCommand("wm density $dpi")
    return sizeResult.isSuccess && densityResult.isSuccess
}

private fun resetResolution(context: Context) {
    ShizukuShellRunner.runCommand("wm size reset")
    ShizukuShellRunner.runCommand("wm density reset")
}
