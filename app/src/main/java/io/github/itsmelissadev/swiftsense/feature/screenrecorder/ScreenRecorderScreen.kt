package io.github.itsmelissadev.swiftsense.feature.screenrecorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import io.github.itsmelissadev.swiftsense.R
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecorderScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var physicalWidth by rememberSaveable { mutableIntStateOf(1080) }
    var physicalHeight by rememberSaveable { mutableIntStateOf(1920) }

    val prefs = remember {
        context.getSharedPreferences("ScreenRecorderPrefs", Context.MODE_PRIVATE)
    }

    val resolutionOptions =
            remember(physicalWidth, physicalHeight) {
                val maxRes = kotlin.math.min(physicalWidth, physicalHeight)
                val list = mutableListOf<String>()
                if (maxRes >= 1080) list.add("1080p")
                if (maxRes >= 720) list.add("720p")
                list.add("480p")
                list
            }

    var selectedResolutionOption by
            rememberSaveable(resolutionOptions) {
                mutableStateOf(
                        prefs.getString("resolution", "1080p")?.takeIf { it in resolutionOptions }
                                ?: resolutionOptions.firstOrNull() ?: "1080p"
                )
            }

    var selectedFpsOption by rememberSaveable { mutableIntStateOf(prefs.getInt("fps", 60)) }
    val fpsOptions = listOf(60, 45, 30, 25, 15)

    var selectedBitrateOption by rememberSaveable { mutableIntStateOf(prefs.getInt("bitrate", 15)) }
    val bitrateOptions = listOf(15, 12, 10, 8, 6, 4, 2)

    val orientationAuto = stringResource(R.string.orientation_auto)
    val orientationPortrait = stringResource(R.string.orientation_portrait)
    val orientationLandscape = stringResource(R.string.orientation_landscape)
    val orientationOptions = listOf(orientationAuto, orientationPortrait, orientationLandscape)
    var selectedOrientationOption by rememberSaveable {
        mutableStateOf(prefs.getString("orientation", orientationAuto) ?: orientationAuto)
    }

    var selectedAudioOption by rememberSaveable { mutableIntStateOf(prefs.getInt("audio", 0)) }
    val audioOptions =
            listOf(
                    Pair(0, stringResource(R.string.audio_disabled)),
                    Pair(1, stringResource(R.string.audio_internal))
            )

    LaunchedEffect(Unit) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            physicalWidth = bounds.width()
            physicalHeight = bounds.height()
        } else {
            @Suppress("DEPRECATION") val display = wm.defaultDisplay
            @Suppress("DEPRECATION") physicalWidth = display.width
            @Suppress("DEPRECATION") physicalHeight = display.height
        }
    }

    val projectionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result ->
                if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
                    startRecorderService(
                            context,
                            selectedResolutionOption,
                            physicalWidth,
                            physicalHeight,
                            selectedFpsOption,
                            selectedBitrateOption,
                            selectedOrientationOption,
                            orientationAuto,
                            orientationPortrait,
                            orientationLandscape,
                            result.resultCode,
                            result.data!!
                    )
                } else {
                    Toast.makeText(
                                    context,
                                    R.string.screen_recorder_permission_denied,
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
            }

    val permissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted ->
                if (isGranted) {
                    val mpManager =
                            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as
                                    android.media.projection.MediaProjectionManager
                    projectionLauncher.launch(mpManager.createScreenCaptureIntent())
                } else {
                    Toast.makeText(
                                    context,
                                    R.string.screen_recorder_permission_required,
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
            }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Text(
                                    stringResource(R.string.feature_screen_recorder).uppercase(),
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
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background,
                                        scrolledContainerColor =
                                                MaterialTheme.colorScheme.background
                                )
                )
            }
    ) { innerPadding ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 20.dp)
                                .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border =
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OptionDropdown(
                            label = stringResource(R.string.screen_recorder_resolution),
                            description = stringResource(R.string.screen_recorder_res_desc),
                            options = resolutionOptions,
                            selectedOption = selectedResolutionOption,
                            onOptionSelected = {
                                selectedResolutionOption = it
                                prefs.edit().putString("resolution", it).apply()
                            }
                    )

                    OptionDropdown(
                            label = stringResource(R.string.screen_recorder_fps),
                            description = stringResource(R.string.screen_recorder_fps_desc),
                            options = fpsOptions.map { it.toString() },
                            selectedOption = selectedFpsOption.toString(),
                            onOptionSelected = {
                                selectedFpsOption = it.toInt()
                                prefs.edit().putInt("fps", it.toInt()).apply()
                            }
                    )

                    OptionDropdown(
                            label = stringResource(R.string.screen_recorder_bitrate),
                            description = stringResource(R.string.screen_recorder_bitrate_desc),
                            options = bitrateOptions.map { "$it Mbps" },
                            selectedOption = "$selectedBitrateOption Mbps",
                            onOptionSelected = {
                                val num = it.replace(" Mbps", "").toInt()
                                selectedBitrateOption = num
                                prefs.edit().putInt("bitrate", num).apply()
                            }
                    )

                    OptionDropdown(
                            label = stringResource(R.string.screen_recorder_orientation),
                            description = stringResource(R.string.screen_recorder_orientation_desc),
                            options = orientationOptions,
                            selectedOption = selectedOrientationOption,
                            onOptionSelected = {
                                selectedOrientationOption = it
                                prefs.edit().putString("orientation", it).apply()
                            }
                    )

                    OptionDropdown(
                            label = stringResource(R.string.screen_recorder_audio),
                            description = stringResource(R.string.screen_recorder_audio_desc),
                            options = audioOptions.map { it.second },
                            selectedOption =
                                    audioOptions.find { it.first == selectedAudioOption }?.second
                                            ?: audioOptions.first().second,
                            onOptionSelected = { selectedName ->
                                val option = audioOptions.find { it.second == selectedName }
                                if (option != null) {
                                    selectedAudioOption = option.first
                                    prefs.edit().putInt("audio", option.first).apply()
                                }
                            }
                    )
                }
            }

            Button(
                    onClick = {
                        val mpManager =
                                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as
                                        android.media.projection.MediaProjectionManager

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                return@Button
                            }
                        }

                        projectionLauncher.launch(mpManager.createScreenCaptureIntent())
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                        stringResource(R.string.screen_recorder_start_service).uppercase(),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun OptionDropdown(
        label: String,
        description: String,
        options: List<String>,
        selectedOption: String,
        onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                onClick = { expanded = true }
        ) {
            Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = selectedOption,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                )
                Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onOptionSelected(option)
                                expanded = false
                            }
                    )
                }
            }
        }

        Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}

internal fun startRecorderService(
        context: Context,
        resolution: String,
        physicalWidth: Int,
        physicalHeight: Int,
        fps: Int,
        bitrate: Int,
        orientationOption: String,
        autoString: String,
        portraitString: String,
        landscapeString: String,
        resultCode: Int,
        data: Intent
) {
    val isLandscape =
            when (orientationOption) {
                landscapeString -> true
                portraitString -> false
                else -> physicalWidth > physicalHeight
            }

    val base = if (physicalWidth > physicalHeight) physicalHeight else physicalWidth
    val ratio =
            if (physicalWidth > physicalHeight) physicalWidth.toFloat() / physicalHeight.toFloat()
            else physicalHeight.toFloat() / physicalWidth.toFloat()

    val targetBase =
            when (resolution) {
                "1080p" -> 1080
                "720p" -> 720
                "480p" -> 480
                else -> 1080
            }

    var targetWidth = if (isLandscape) (targetBase * ratio).roundToInt() else targetBase
    var targetHeight = if (isLandscape) targetBase else (targetBase * ratio).roundToInt()

    // screenrecord command requires even dimensions
    if (targetWidth % 2 != 0) targetWidth++
    if (targetHeight % 2 != 0) targetHeight++

    val intent =
            Intent(context, ScreenRecorderService::class.java).apply {
                action = ScreenRecorderService.ACTION_START
                putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode)
                putExtra(ScreenRecorderService.EXTRA_DATA, data)
                putExtra(ScreenRecorderService.EXTRA_WIDTH, targetWidth)
                putExtra(ScreenRecorderService.EXTRA_HEIGHT, targetHeight)
                putExtra(ScreenRecorderService.EXTRA_FPS, fps)
                putExtra(ScreenRecorderService.EXTRA_BITRATE, bitrate)
            }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
