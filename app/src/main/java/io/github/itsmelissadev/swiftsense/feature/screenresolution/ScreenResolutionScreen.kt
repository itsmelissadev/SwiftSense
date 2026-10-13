package io.github.itsmelissadev.swiftsense.feature.screenresolution

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material3.ButtonDefaults
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
import io.github.itsmelissadev.swiftsense.service.shell.ShellBridge
import io.github.itsmelissadev.swiftsense.ui.components.ExecutionStatusWidget
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveCardGroup
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveDialog
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveDialogButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveHeader
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveOutlinedButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSwitchPreference
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveTextField
import io.github.itsmelissadev.swiftsense.ui.theme.ExpressivePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
            val sizeResult = io.github.itsmelissadev.swiftsense.service.shell.ShellBridge.run("wm size")
            var pWidth = currentWidth
            var pHeight = currentHeight
            sizeResult.onSuccess { output ->
                if (output.contains("Physical size:")) {
                    val size = output.substringAfter("Physical size:").trim().split("\n")[0].split("x")
                    pWidth = size[0].toInt()
                    pHeight = size[1].toInt()
                }
            }

            val densityResult = io.github.itsmelissadev.swiftsense.service.shell.ShellBridge.run("wm density")
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
        ExpressiveDialog(
            onDismissRequest = { showSavePlanDialog = false },
            title = stringResource(R.string.save_plan),
            content = {
                ExpressiveTextField(
                    value = planNameInput,
                    onValueChange = { planNameInput = it },
                    label = stringResource(R.string.plan_name)
                )
            },
            confirmButton = {
                ExpressiveDialogButton(
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
                            Toast.makeText(context, R.string.toast_plan_saved, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            dismissButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { showSavePlanDialog = false }
                )
            }
        )
    }

    if (showConfirmationDialog) {
        ExpressiveDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = stringResource(R.string.confirm_resolution_title),
            description = stringResource(R.string.confirm_resolution_desc, countdown),
            confirmButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_keep_changes),
                    onClick = { showConfirmationDialog = false }
                )
            },
            dismissButton = {
                ExpressiveDialogButton(
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ExpressiveHeader(
                title = stringResource(R.string.feature_screen_resolution),
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { refreshDisplayInfo() }) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ExecutionStatusWidget()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.display_info),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )

                ExpressiveCardGroup {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
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
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.resolution_settings),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )

                ExpressiveCardGroup {
                    item {
                        ExpressiveSwitchPreference(
                            title = stringResource(R.string.maintain_aspect_ratio),
                            description = if (maintainAspectRatio) stringResource(R.string.ratio_locked)
                            else stringResource(R.string.ratio_unlocked),
                            icon = Icons.Rounded.AspectRatio,
                            badgeColors = ExpressivePalette.TealLight,
                            checked = maintainAspectRatio,
                            onCheckedChange = { maintainAspectRatio = it }
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ExpressiveTextField(
                                    value = inputWidth,
                                    onValueChange = { newValue ->
                                        if (newValue.all { it.isDigit() }) {
                                            inputWidth = newValue
                                            if (maintainAspectRatio && newValue.isNotEmpty() && physicalWidth > 0) {
                                                val ratio = physicalHeight.toFloat() / physicalWidth.toFloat()
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

                                ExpressiveTextField(
                                    value = inputHeight,
                                    onValueChange = { newValue ->
                                        if (newValue.all { it.isDigit() }) {
                                            inputHeight = newValue
                                            if (maintainAspectRatio && newValue.isNotEmpty() && physicalHeight > 0) {
                                                val ratio = physicalWidth.toFloat() / physicalHeight.toFloat()
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

                            ExpressiveTextField(
                                value = inputDpi,
                                onValueChange = { if (it.all { char -> char.isDigit() }) inputDpi = it },
                                label = stringResource(R.string.resolution_dpi),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
            }

            val isReady by ShellBridge.ready.collectAsState()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExpressiveButton(
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
                        enabled = isReady
                    )

                    ExpressiveOutlinedButton(
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = CircleShape,
                    enabled = isReady,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        stringResource(R.string.action_reset),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.resolution_plans),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )

                ExpressiveCardGroup {
                    if (savedPlans.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.no_plans),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        savedPlans.forEach { planJson ->
                            val plan = JSONObject(planJson)
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            inputWidth = plan.getString("width")
                                            inputHeight = plan.getString("height")
                                            inputDpi = plan.getString("dpi")
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = plan.getString("name"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${plan.getString("width")} x ${plan.getString("height")} @ ${plan.getString("dpi")} DPI",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                preferenceManager.deleteResolutionPlan(planJson)
                                                Toast.makeText(context, R.string.toast_plan_deleted, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
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
        val sizeCmd = "wm size ${width}x${height}"
        val dpiCmd = "wm density $dpi"

        val sizeRes = ShellBridge.run(sizeCmd)
        val dpiRes = ShellBridge.run(dpiCmd)

        sizeRes.isSuccess && dpiRes.isSuccess
    }
}

private suspend fun resetResolution() {
    withContext(Dispatchers.IO) {
        ShellBridge.run("wm size reset")
        ShellBridge.run("wm density reset")
    }
}
