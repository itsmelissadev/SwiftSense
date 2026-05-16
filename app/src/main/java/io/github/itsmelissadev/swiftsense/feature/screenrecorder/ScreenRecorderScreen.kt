@file:Suppress("ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE")

package io.github.itsmelissadev.swiftsense.feature.screenrecorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import io.github.itsmelissadev.swiftsense.R
import kotlin.math.roundToInt

@SuppressLint("UseKtx")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecorderScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var physicalWidth by rememberSaveable { mutableIntStateOf(1080) }
    var physicalHeight by rememberSaveable { mutableIntStateOf(1920) }
    var isServiceRunning by remember { mutableStateOf(ScreenRecorderService.isRunning) }
    var recorderState by remember { mutableStateOf(ScreenRecorderService.currentRecorderState) }

    LaunchedEffect(Unit) {
        while (true) {
            isServiceRunning = ScreenRecorderService.isRunning
            recorderState = ScreenRecorderService.currentRecorderState
            kotlinx.coroutines.delay(1000)
        }
    }

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

    var isHevcSupported by remember { mutableStateOf(false) }

    val codecAuto = stringResource(R.string.codec_auto)
    val codecAvc = stringResource(R.string.codec_avc)
    val codecHevc = stringResource(R.string.codec_hevc)

    val codecOptions =
            remember(isHevcSupported) {
                val list = mutableListOf(codecAuto, codecAvc)
                if (isHevcSupported) {
                    list.add(codecHevc)
                }
                list
            }

    var selectedCodecOption by
            rememberSaveable(codecOptions) {
                mutableStateOf(
                        prefs.getString("codec", codecAuto)?.takeIf { it in codecOptions }
                                ?: codecAuto
                )
            }

    val codecDescriptions =
            mapOf(
                    codecAuto to stringResource(R.string.codec_desc_auto),
                    codecAvc to stringResource(R.string.codec_desc_avc),
                    codecHevc to stringResource(R.string.codec_desc_hevc)
            )

    val audioQualityLow = stringResource(R.string.audio_quality_low)
    val audioQualityMedium = stringResource(R.string.audio_quality_medium)
    val audioQualityHigh = stringResource(R.string.audio_quality_high)

    val audioQualityOptions = listOf(audioQualityLow, audioQualityMedium, audioQualityHigh)
    var selectedAudioQualityOption by rememberSaveable {
        mutableStateOf(prefs.getString("audio_quality", audioQualityMedium) ?: audioQualityMedium)
    }

    val audioQualityDescriptions =
            mapOf(
                    audioQualityLow to stringResource(R.string.audio_quality_desc_low),
                    audioQualityMedium to stringResource(R.string.audio_quality_desc_medium),
                    audioQualityHigh to stringResource(R.string.audio_quality_desc_high)
            )

    LaunchedEffect(Unit) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            physicalWidth = bounds.width()
            physicalHeight = bounds.height()
        } else {
            @Suppress("DEPRECATION") val display = wm.defaultDisplay
            @Suppress("DEPRECATION")
            physicalWidth = display.width
            @Suppress("DEPRECATION")
            physicalHeight = display.height
        }

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (info in codecList.codecInfos) {
                if (info.isEncoder &&
                                info.supportedTypes.any {
                                    it.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true)
                                }
                ) {
                    isHevcSupported = true
                    break
                }
            }
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
                            selectedAudioOption,
                            when (selectedCodecOption) {
                                codecAvc -> 1
                                codecHevc -> 2
                                else -> 0
                            },
                            when (selectedAudioQualityOption) {
                                audioQualityLow -> 64000
                                audioQualityHigh -> 256000
                                else -> 128000
                            },
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

    val audioPermissionLauncher =
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

    val notificationPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted ->
                if (isGranted) {
                    if (selectedAudioOption != 0 &&
                                    ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                    ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        val mpManager =
                                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as
                                        android.media.projection.MediaProjectionManager
                        projectionLauncher.launch(mpManager.createScreenCaptureIntent())
                    }
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
            },
            floatingActionButton = {
                FloatingActionButton(
                        onClick = {
                            if (isServiceRunning) {
                                if (recorderState == ScreenRecorderService.RecorderState.RECORDING
                                ) {
                                    context.startService(
                                            Intent(context, ScreenRecorderService::class.java)
                                                    .apply {
                                                        action = ScreenRecorderService.ACTION_STOP
                                                    }
                                    )
                                } else {
                                    context.startService(
                                            Intent(context, ScreenRecorderService::class.java)
                                                    .apply {
                                                        action =
                                                                ScreenRecorderService
                                                                        .ACTION_SHUTDOWN
                                                    }
                                    )
                                }
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.POST_NOTIFICATIONS
                                                ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else if (selectedAudioOption != 0 &&
                                                ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.RECORD_AUDIO
                                                ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    val mpManager =
                                            context.getSystemService(
                                                    Context.MEDIA_PROJECTION_SERVICE
                                            ) as
                                                    android.media.projection.MediaProjectionManager
                                    projectionLauncher.launch(mpManager.createScreenCaptureIntent())
                                }
                            }
                        },
                        containerColor =
                                if (isServiceRunning) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                        contentColor =
                                if (isServiceRunning) MaterialTheme.colorScheme.onError
                                else MaterialTheme.colorScheme.onPrimary,
                        shape = FloatingActionButtonDefaults.shape
                ) {
                    Icon(
                            imageVector =
                                    if (isServiceRunning) Icons.Default.Stop
                                    else Icons.Default.PlayArrow,
                            contentDescription = if (isServiceRunning) "Stop" else "Start",
                            modifier = Modifier.size(24.dp)
                    )
                }
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
            Spacer(modifier = Modifier.height(8.dp))

            // Video Settings Section
            SettingsSection(
                    title = stringResource(R.string.screen_recorder_video_header),
                    icon = Icons.Outlined.Videocam
            ) {
                OptionDropdown(
                        label = stringResource(R.string.screen_recorder_resolution),
                        description = stringResource(R.string.screen_recorder_res_desc),
                        options = resolutionOptions,
                        selectedOption = selectedResolutionOption,
                        onOptionSelected = {
                            selectedResolutionOption = it
                            prefs.edit { putString("resolution", it) }
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
                        label = stringResource(R.string.screen_recorder_codec),
                        description = codecDescriptions[selectedCodecOption] ?: "",
                        options = codecOptions,
                        selectedOption = selectedCodecOption,
                        onOptionSelected = {
                            selectedCodecOption = it
                            prefs.edit().putString("codec", it).apply()
                        }
                )
            }

            // Audio Settings Section
            SettingsSection(
                    title = stringResource(R.string.screen_recorder_audio),
                    icon = Icons.Outlined.Mic
            ) {
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

                if (selectedAudioOption != 0) {
                    OptionDropdown(
                            label = stringResource(R.string.screen_recorder_audio_quality),
                            description = audioQualityDescriptions[selectedAudioQualityOption]
                                            ?: "",
                            options = audioQualityOptions,
                            selectedOption = selectedAudioQualityOption,
                            onOptionSelected = {
                                selectedAudioQualityOption = it
                                prefs.edit().putString("audio_quality", it).apply()
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
        }
    }
}

@Composable
fun SettingsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
            )
            Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
            )
        }

        Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border =
                        androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        ),
                shadowElevation = 0.dp
        ) {
            Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    content = content
            )
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
        audioOption: Int,
        codecChoice: Int,
        audioQualityChoice: Int,
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
                putExtra(ScreenRecorderService.EXTRA_AUDIO, audioOption)
                putExtra(ScreenRecorderService.EXTRA_CODEC, codecChoice)
                putExtra(ScreenRecorderService.EXTRA_AUDIO_QUALITY, audioQualityChoice)
            }

    context.startForegroundService(intent)
}
