package io.github.itsmelissadev.swiftsense.feature.screenresolution

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.shizuku.ShizukuShellRunner
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
        AlertDialog(
            onDismissRequest = { showSavePlanDialog = false },
            title = { Text(stringResource(R.string.save_plan), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = planNameInput,
                    onValueChange = { planNameInput = it },
                    label = { Text(stringResource(R.string.plan_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(100)
                )
            },
            shape = RoundedCornerShape(32.dp),
            confirmButton = {
                Button(
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
                    },
                    shape = RoundedCornerShape(100)
                ) {
                    Text(stringResource(R.string.save_plan))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSavePlanDialog = false },
                    shape = RoundedCornerShape(100)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            shape = RoundedCornerShape(32.dp),
            title = {
                Text(
                    stringResource(R.string.confirm_resolution_title),
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Text(
                    stringResource(R.string.confirm_resolution_desc, countdown),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            confirmButton = {
                Button(onClick = {
                    showConfirmationDialog = false
                    refreshDisplayInfo()
                }, shape = RoundedCornerShape(100), modifier = Modifier.height(48.dp)) {
                    Text(stringResource(R.string.action_keep_changes), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                FilledTonalButton(onClick = {
                    resetResolution(context)
                    showConfirmationDialog = false
                    refreshDisplayInfo()
                }, shape = RoundedCornerShape(100), modifier = Modifier.height(48.dp)) {
                    Text(stringResource(R.string.action_revert))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.feature_screen_resolution),
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
                actions = {
                    IconButton(
                        onClick = { refreshDisplayInfo() },
                        modifier = Modifier.padding(end = 8.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            ShizukuStatusWidget()

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(
                            R.string.current_resolution,
                            currentWidth,
                            currentHeight,
                            currentDpi
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.physical_resolution,
                                physicalWidth,
                                physicalHeight,
                                physicalDpi
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (maintainAspectRatio) stringResource(R.string.maintain_aspect_ratio) else stringResource(
                                R.string.free_adjustment
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = maintainAspectRatio,
                            onCheckedChange = { maintainAspectRatio = it }
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
                            label = { Text(stringResource(R.string.resolution_width)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(100),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
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
                            label = { Text(stringResource(R.string.resolution_height)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(100),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                    }

                    OutlinedTextField(
                        value = inputDpi,
                        onValueChange = { if (it.all { char -> char.isDigit() }) inputDpi = it },
                        label = { Text(stringResource(R.string.resolution_dpi)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(100),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            val isShizukuReady =
                Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (applyResolution(context, inputWidth, inputHeight, inputDpi)) {
                            countdown = 10; showConfirmationDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(100),
                    enabled = isShizukuReady
                ) {
                    Text(
                        stringResource(R.string.action_apply),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                IconButton(
                    onClick = { showSavePlanDialog = true },
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            FilledTonalButton(
                onClick = { resetResolution(context); refreshDisplayInfo() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(100),
                enabled = isShizukuReady,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(
                    stringResource(R.string.action_reset),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = stringResource(R.string.resolution_plans),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp)
            )

            if (savedPlans.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_plans),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                savedPlans.forEach { planJson ->
                    val plan = JSONObject(planJson)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${plan.getString("width")}x${plan.getString("height")} - ${
                                        plan.getString(
                                            "dpi"
                                        )
                                    } DPI",
                                    style = MaterialTheme.typography.bodyMedium,
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
                                },
                                modifier = Modifier.clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            if (!isShizukuReady) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = stringResource(R.string.shizuku_required_desc),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
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
