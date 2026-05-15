package io.github.itsmelissadev.swiftsense.feature.screenrecorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
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
            val prefs = getSharedPreferences("ScreenRecorderPrefs", Context.MODE_PRIVATE)
            val resolution = prefs.getString("resolution", "1080p") ?: "1080p"
            val fps = prefs.getInt("fps", 60)
            val bitrate = prefs.getInt("bitrate", 15)
            
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val bounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wm.currentWindowMetrics.bounds
            } else {
                @Suppress("DEPRECATION")
                val display = wm.defaultDisplay
                android.graphics.Rect(0, 0, display.width, display.height)
            }
            
            val physicalWidth = bounds.width()
            val physicalHeight = bounds.height()

            val orientationAuto = getString(R.string.orientation_auto)
            val orientationPortrait = getString(R.string.orientation_portrait)
            val orientationLandscape = getString(R.string.orientation_landscape)
            
            val orientationOption = prefs.getString("orientation", orientationAuto) ?: orientationAuto

            startRecorderService(
                context = this,
                resolution = resolution,
                physicalWidth = physicalWidth,
                physicalHeight = physicalHeight,
                fps = fps,
                bitrate = bitrate,
                orientationOption = orientationOption,
                autoString = orientationAuto,
                portraitString = orientationPortrait,
                landscapeString = orientationLandscape,
                resultCode = result.resultCode,
                data = result.data!!
            )
        }
        finish()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchProjectionRequest()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        launchProjectionRequest()
    }

    private fun launchProjectionRequest() {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mpManager.createScreenCaptureIntent())
    }
}
