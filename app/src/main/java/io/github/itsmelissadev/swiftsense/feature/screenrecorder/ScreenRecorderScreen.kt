package io.github.itsmelissadev.swiftsense.feature.screenrecorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSection
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSelectionDialog
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTextField
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTopAppBar
import kotlin.math.roundToInt

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
    var showFpsDialog by remember { mutableStateOf(false) }

    var selectedBitrateOption by rememberSaveable { mutableIntStateOf(prefs.getInt("bitrate", 15)) }
    val bitrateOptions = listOf(15, 12, 10, 8, 6, 4, 2)
    var showBitrateDialog by remember { mutableStateOf(false) }

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
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showOrientationDialog by remember { mutableStateOf(false) }
    var showCodecDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showAudioQualityDialog by remember { mutableStateOf(false) }

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
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            physicalWidth = dm.widthPixels
            physicalHeight = dm.heightPixels
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
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
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
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
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
            SwiftSenseTopAppBar(
                title = stringResource(R.string.feature_screen_recorder),
                onNavigateBack = onNavigateBack
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
                    contentDescription = if (isServiceRunning) {
                        stringResource(R.string.screen_recorder_action_stop_desc)
                    } else {
                        stringResource(R.string.screen_recorder_action_start_desc)
                    },
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

            SwiftSenseSection(
                title = stringResource(R.string.screen_recorder_video_header)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SwiftSenseTextField(
                            value = selectedResolutionOption,
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.screen_recorder_resolution),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showResolutionDialog = true }
                        )
                    }
                    Text(
                        text = stringResource(R.string.screen_recorder_res_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SwiftSenseTextField(
                            value = selectedFpsOption.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.screen_recorder_fps),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showFpsDialog = true }
                        )
                    }
                    Text(
                        text = stringResource(R.string.screen_recorder_fps_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SwiftSenseTextField(
                            value = stringResource(
                                R.string.screen_recorder_mbps_unit,
                                selectedBitrateOption
                            ),
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.screen_recorder_bitrate),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showBitrateDialog = true }
                        )
                    }
                    Text(
                        text = stringResource(R.string.screen_recorder_bitrate_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SwiftSenseTextField(
                            value = selectedOrientationOption,
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.screen_recorder_orientation),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showOrientationDialog = true }
                        )
                    }
                    Text(
                        text = stringResource(R.string.screen_recorder_orientation_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SwiftSenseTextField(
                            value = selectedCodecOption,
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.screen_recorder_codec),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showCodecDialog = true }
                        )
                    }
                    Text(
                        text = codecDescriptions[selectedCodecOption] ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            SwiftSenseSection(
                title = stringResource(R.string.screen_recorder_audio)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SwiftSenseTextField(
                            value = audioOptions.find { it.first == selectedAudioOption }?.second
                                ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.screen_recorder_audio),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showAudioDialog = true }
                        )
                    }
                    Text(
                        text = stringResource(R.string.screen_recorder_audio_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                if (selectedAudioOption != 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            SwiftSenseTextField(
                                value = selectedAudioQualityOption,
                                onValueChange = {},
                                readOnly = true,
                                label = stringResource(R.string.screen_recorder_audio_quality),
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showAudioQualityDialog = true }
                            )
                        }
                        Text(
                            text = audioQualityDescriptions[selectedAudioQualityOption] ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showResolutionDialog) {
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.screen_recorder_resolution),
            options = resolutionOptions.map { it to it },
            selected = selectedResolutionOption,
            onSelectedChange = { res ->
                selectedResolutionOption = res
                prefs.edit { putString("resolution", res) }
            },
            onDismissRequest = { showResolutionDialog = false }
        )
    }

    if (showFpsDialog) {
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.screen_recorder_fps),
            options = fpsOptions.map { it to it.toString() },
            selected = selectedFpsOption,
            onSelectedChange = { fps ->
                selectedFpsOption = fps
                prefs.edit { putInt("fps", fps) }
            },
            onDismissRequest = { showFpsDialog = false }
        )
    }

    if (showBitrateDialog) {
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.screen_recorder_bitrate),
            options = bitrateOptions.map {
                it to stringResource(
                    R.string.screen_recorder_mbps_unit,
                    it
                )
            },
            selected = selectedBitrateOption,
            onSelectedChange = { bitrate ->
                selectedBitrateOption = bitrate
                prefs.edit { putInt("bitrate", bitrate) }
            },
            onDismissRequest = { showBitrateDialog = false }
        )
    }

    if (showOrientationDialog) {
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.screen_recorder_orientation),
            options = orientationOptions.map { it to it },
            selected = selectedOrientationOption,
            onSelectedChange = { orientation ->
                selectedOrientationOption = orientation
                prefs.edit { putString("orientation", orientation) }
            },
            onDismissRequest = { showOrientationDialog = false }
        )
    }

    if (showCodecDialog) {
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.screen_recorder_codec),
            options = codecOptions.map { it to it },
            selected = selectedCodecOption,
            onSelectedChange = { codec ->
                selectedCodecOption = codec
                prefs.edit { putString("codec", codec) }
            },
            onDismissRequest = { showCodecDialog = false }
        )
    }

    if (showAudioDialog) {
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.screen_recorder_audio),
            options = audioOptions,
            selected = selectedAudioOption,
            onSelectedChange = { audio ->
                selectedAudioOption = audio
                prefs.edit { putInt("audio", audio) }
            },
            onDismissRequest = { showAudioDialog = false }
        )
    }

    if (showAudioQualityDialog) {
        SwiftSenseSelectionDialog(
            title = stringResource(R.string.screen_recorder_audio_quality),
            options = audioQualityOptions.map { it to it },
            selected = selectedAudioQualityOption,
            onSelectedChange = { quality ->
                selectedAudioQualityOption = quality
                prefs.edit { putString("audio_quality", quality) }
            },
            onDismissRequest = { showAudioQualityDialog = false }
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
