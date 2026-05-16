package io.github.itsmelissadev.swiftsense.feature.screenrecorder

import android.Manifest
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import io.github.itsmelissadev.swiftsense.R

class ScreenRecorderPromptActivity : ComponentActivity() {

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val prefs = getSharedPreferences("ScreenRecorderPrefs", MODE_PRIVATE)
            val resolution = prefs.getString("resolution", "1080p") ?: "1080p"
            val fps = prefs.getInt("fps", 60)
            val bitrate = prefs.getInt("bitrate", 15)
            val audio = prefs.getInt("audio", 0)

            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val bounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wm.currentWindowMetrics.bounds
            } else {
                val dm = DisplayMetrics()
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getRealMetrics(dm)
                android.graphics.Rect(0, 0, dm.widthPixels, dm.heightPixels)
            }

            val physicalWidth = bounds.width()
            val physicalHeight = bounds.height()

            val orientationAuto = getString(R.string.orientation_auto)
            val orientationPortrait = getString(R.string.orientation_portrait)
            val orientationLandscape = getString(R.string.orientation_landscape)

            val orientationOption =
                prefs.getString("orientation", orientationAuto) ?: orientationAuto

            val codecOption = prefs.getString("codec", getString(R.string.codec_auto)) ?: getString(
                R.string.codec_auto
            )
            val audioQualityOption =
                prefs.getString("audio_quality", getString(R.string.audio_quality_medium))
                    ?: getString(R.string.audio_quality_medium)

            val codecChoice = when (codecOption) {
                getString(R.string.codec_avc) -> 1
                getString(R.string.codec_hevc) -> 2
                else -> 0
            }

            val audioQualityChoice = when (audioQualityOption) {
                getString(R.string.audio_quality_low) -> 64000
                getString(R.string.audio_quality_high) -> 256000
                else -> 128000
            }

            startRecorderService(
                context = this,
                resolution = resolution,
                physicalWidth = physicalWidth,
                physicalHeight = physicalHeight,
                fps = fps,
                bitrate = bitrate,
                orientationOption = orientationOption,
                audioOption = audio,
                codecChoice = codecChoice,
                audioQualityChoice = audioQualityChoice,
                portraitString = orientationPortrait,
                landscapeString = orientationLandscape,
                resultCode = result.resultCode,
                data = result.data!!
            )
        }
        finish()
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchProjectionRequest()
        } else {
            finish()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkAudioPermissionAndLaunch()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        checkAudioPermissionAndLaunch()
    }

    private fun checkAudioPermissionAndLaunch() {
        val prefs = getSharedPreferences("ScreenRecorderPrefs", MODE_PRIVATE)
        val audioOption = prefs.getInt("audio", 0)

        if (audioOption != 0) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return
            }
        }

        launchProjectionRequest()
    }

    private fun launchProjectionRequest() {
        val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mpManager.createScreenCaptureIntent())
    }
}
