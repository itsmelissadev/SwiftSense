package io.github.itsmelissadev.swiftsense.feature.amoledscreenprotect

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.core.graphics.withTranslation
import io.github.itsmelissadev.swiftsense.MainActivity
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("AccessibilityPolicy")
class AmoledProtectService : AccessibilityService() {
    private val overlayViews = mutableMapOf<String, AmoledFilterView>()
    private var windowManager: WindowManager? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var animationJob: Job? = null
    private var preferenceJob: Job? = null
    private lateinit var preferenceManager: PreferenceManager

    private var currentCycleDuration = 30
    private var currentRefreshMode = "smooth"

    companion object {
        var isServiceRunning = false
            private set

        private const val CHANNEL_ID = "amoled_protect_channel"
        private const val NOTIFICATION_ID = 2002

        const val REGION_FULL_SCREEN = "full_screen"
        const val REGION_STATUS_BAR = "status_bar"
        const val REGION_NAVIGATION_BAR = "navigation_bar"
        const val REGION_CUSTOM = "custom"
        const val REGION_CUSTOM_TOP = "custom_top"
        const val REGION_CUSTOM_BOTTOM = "custom_bottom"

        private const val ANIMATION_STEPS = 120
    }

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        preferenceJob = scope.launch {
            launch {
                preferenceManager.amoledRegions.collect { regions ->
                    rebuildOverlays(regions)
                }
            }

            launch {
                preferenceManager.amoledIntensity.collect { intensity ->
                    overlayViews.values.forEach { it.updateDensity(intensity) }
                }
            }

            launch {
                preferenceManager.amoledOpacity.collect { opacity ->
                    overlayViews.values.forEach { it.updateOpacity(opacity) }
                }
            }

            launch {
                preferenceManager.amoledFilterType.collect { filterType ->
                    overlayViews.values.forEach { it.updateFilterType(filterType) }
                }
            }

            launch {
                preferenceManager.amoledShiftSpeed.collect { cycleDuration ->
                    currentCycleDuration = cycleDuration
                    restartAnimation()
                }
            }

            launch {
                preferenceManager.amoledRefreshMode.collect { mode ->
                    currentRefreshMode = mode
                    restartAnimation()
                }
            }

            launch {
                preferenceManager.amoledCustomGapHeight.collect {
                    updateCustomOverlays()
                }
            }

            launch {
                preferenceManager.amoledCustomPosition.collect {
                    updateCustomOverlays()
                }
            }

            launch {
                preferenceManager.amoledTintEnabled.collect { enabled ->
                    val color = preferenceManager.amoledTintColor.first()
                    val hex = preferenceManager.amoledTintCustomHex.first()
                    val intensity = preferenceManager.amoledTintIntensity.first()
                    overlayViews.values.forEach { it.updateTint(enabled, color, hex, intensity) }
                }
            }

            launch {
                preferenceManager.amoledTintColor.collect { color ->
                    val enabled = preferenceManager.amoledTintEnabled.first()
                    val hex = preferenceManager.amoledTintCustomHex.first()
                    val intensity = preferenceManager.amoledTintIntensity.first()
                    overlayViews.values.forEach { it.updateTint(enabled, color, hex, intensity) }
                }
            }

            launch {
                preferenceManager.amoledTintCustomHex.collect { hex ->
                    val enabled = preferenceManager.amoledTintEnabled.first()
                    val color = preferenceManager.amoledTintColor.first()
                    val intensity = preferenceManager.amoledTintIntensity.first()
                    overlayViews.values.forEach { it.updateTint(enabled, color, hex, intensity) }
                }
            }

            launch {
                preferenceManager.amoledTintIntensity.collect { intensity ->
                    val enabled = preferenceManager.amoledTintEnabled.first()
                    val color = preferenceManager.amoledTintColor.first()
                    val hex = preferenceManager.amoledTintCustomHex.first()
                    overlayViews.values.forEach { it.updateTint(enabled, color, hex, intensity) }
                }
            }
        }
    }

    private suspend fun rebuildOverlays(regions: Set<String>) {
        overlayViews.forEach { (_, view) ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {
            }
        }
        overlayViews.clear()

        val density = preferenceManager.amoledIntensity.first()
        val opacity = preferenceManager.amoledOpacity.first()
        val filterType = preferenceManager.amoledFilterType.first()
        val tintEnabled = preferenceManager.amoledTintEnabled.first()
        val tintColor = preferenceManager.amoledTintColor.first()
        val tintHex = preferenceManager.amoledTintCustomHex.first()
        val tintIntensity = preferenceManager.amoledTintIntensity.first()

        for (region in regions) {
            if (region == REGION_CUSTOM) {
                val screenHeight = getScreenHeight()
                val gapRatio = preferenceManager.amoledCustomGapHeight.first()
                val posRatio = preferenceManager.amoledCustomPosition.first()
                val (topHeight, bottomHeight) = calculateCustomHeights(screenHeight, gapRatio, posRatio)

                val topView = AmoledFilterView(this@AmoledProtectService)
                topView.updateDensity(density)
                topView.updateOpacity(opacity)
                topView.updateFilterType(filterType)
                topView.updateTint(tintEnabled, tintColor, tintHex, tintIntensity)
                val topParams = createLayoutParams(REGION_CUSTOM_TOP, topHeight)
                try {
                    windowManager?.addView(topView, topParams)
                    overlayViews[REGION_CUSTOM_TOP] = topView
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val bottomView = AmoledFilterView(this@AmoledProtectService)
                bottomView.updateDensity(density)
                bottomView.updateOpacity(opacity)
                bottomView.updateFilterType(filterType)
                bottomView.updateTint(tintEnabled, tintColor, tintHex, tintIntensity)
                val bottomParams = createLayoutParams(REGION_CUSTOM_BOTTOM, bottomHeight)
                try {
                    windowManager?.addView(bottomView, bottomParams)
                    overlayViews[REGION_CUSTOM_BOTTOM] = bottomView
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                val view = AmoledFilterView(this@AmoledProtectService)
                view.updateDensity(density)
                view.updateOpacity(opacity)
                view.updateFilterType(filterType)
                view.updateTint(tintEnabled, tintColor, tintHex, tintIntensity)

                val params = createLayoutParams(region)
                try {
                    windowManager?.addView(view, params)
                    overlayViews[region] = view
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getScreenHeight(): Int {
        val wm = windowManager ?: getSystemService(WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = wm.currentWindowMetrics
            windowMetrics.bounds.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            dm.heightPixels
        }
    }

    private fun calculateCustomHeights(screenHeight: Int, gapRatio: Float, positionRatio: Float): Pair<Int, Int> {
        val gapPx = (screenHeight * gapRatio).toInt()
        val centerPx = (screenHeight * positionRatio).toInt()
        val gapTop = (centerPx - gapPx / 2).coerceIn(0, screenHeight - gapPx)
        val gapBottom = gapTop + gapPx
        val topHeight = gapTop.coerceAtLeast(0)
        val bottomHeight = (screenHeight - gapBottom).coerceAtLeast(0)
        return Pair(topHeight, bottomHeight)
    }

    private suspend fun updateCustomOverlays() {
        val currentRegions = preferenceManager.amoledRegions.first()
        if (currentRegions.contains(REGION_CUSTOM)) {
            val gapRatio = preferenceManager.amoledCustomGapHeight.first()
            val posRatio = preferenceManager.amoledCustomPosition.first()
            applyLiveCustom(gapRatio, posRatio)
        }
    }

    private fun applyLiveCustom(gapRatio: Float, posRatio: Float) {
        val topView = overlayViews[REGION_CUSTOM_TOP]
        val bottomView = overlayViews[REGION_CUSTOM_BOTTOM]
        if (topView == null && bottomView == null) return

        val screenHeight = getScreenHeight()
        val (topHeight, bottomHeight) = calculateCustomHeights(screenHeight, gapRatio, posRatio)

        topView?.let { view ->
            val params = view.layoutParams as? WindowManager.LayoutParams
            if (params != null && params.height != topHeight) {
                params.height = topHeight
                try {
                    windowManager?.updateViewLayout(view, params)
                } catch (_: Exception) {
                }
            }
        }

        bottomView?.let { view ->
            val params = view.layoutParams as? WindowManager.LayoutParams
            if (params != null && params.height != bottomHeight) {
                params.height = bottomHeight
                try {
                    windowManager?.updateViewLayout(view, params)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun createLayoutParams(region: String, customHeight: Int = 0): WindowManager.LayoutParams {
        return when (region) {
            REGION_STATUS_BAR -> {
                val statusBarHeight = getStatusBarHeight()
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    statusBarHeight,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                            } else {
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                            }
                    }
                }
            }

            REGION_NAVIGATION_BAR -> {
                val navBarHeight = getNavigationBarHeight()
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    navBarHeight,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.START
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                            } else {
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                            }
                    }
                }
            }

            REGION_CUSTOM_TOP -> {
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    customHeight,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                            } else {
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                            }
                    }
                }
            }

            REGION_CUSTOM_BOTTOM -> {
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    customHeight,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.START
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                            } else {
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                            }
                    }
                }
            }

            else -> {
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                            } else {
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                            }
                    }
                }
            }
        }
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else (24 * resources.displayMetrics.density).toInt()
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    private fun getNavigationBarHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else (48 * resources.displayMetrics.density).toInt()
    }

    private fun restartAnimation() {
        animationJob?.cancel()
        val totalMs = currentCycleDuration * 1000L

        animationJob = scope.launch {
            overlayViews.values.forEach { it.rebuildPattern() }

            if (currentRefreshMode == "smooth") {

                val stepMs = (totalMs / ANIMATION_STEPS).coerceAtLeast(16L)
                var step = 0
                while (isActive) {
                    val progress = step.toFloat() / ANIMATION_STEPS
                    val t = if (progress <= 0.5f) progress * 2f else (1f - progress) * 2f
                    overlayViews.values.forEach { it.animateShift(t) }
                    delay(stepMs)
                    step = (step + 1) % ANIMATION_STEPS
                }
            } else {

                var jumpIndex = 0
                while (isActive) {

                    val t = (jumpIndex % 4) * 0.33f
                    overlayViews.values.forEach { it.animateShift(t) }

                    delay(totalMs)
                    jumpIndex++
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun createNotification(): Notification {
        val mainPI = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.feature_amoled_protect))
            .setContentText(getString(R.string.amoled_on))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(mainPI)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.feature_amoled_protect),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        animationJob?.cancel()
        preferenceJob?.cancel()
        overlayViews.forEach { (_, view) ->
            try {
                view.cleanup()
                windowManager?.removeView(view)
            } catch (_: Exception) {
            }
        }
        overlayViews.clear()
    }

    private class AmoledFilterView(context: Context) : View(context) {
        private var shiftX = 0f
        private var shiftY = 0f
        private val paint = Paint()
        private val pixelShiftPaint = Paint()
        private var currentDensity = 0.5f
        private var currentOpacity = 0.5f
        private var currentFilterType = "checker_grid"
        private var frameCount = 0
        private var useShaderDraw = true
        private var maxShiftX = 20f
        private var maxShiftY = 20f

        private var tintEnabled = false
        private var tintColor = "amber"
        private var tintCustomHex = "#FFA500"
        private var tintIntensity = 0.35f
        private val tintPaint = Paint()

        fun updateTint(enabled: Boolean, color: String, customHex: String, intensity: Float) {
            tintEnabled = enabled
            tintColor = color
            tintCustomHex = customHex
            tintIntensity = intensity
            rebuildTint()
            invalidate()
        }

        private fun rebuildTint() {
            if (!tintEnabled || tintIntensity <= 0f) {
                return
            }
            val alpha = (tintIntensity * 255).toInt().coerceIn(0, 255)
            val baseColor = when (tintColor) {
                "amber" -> Color.parseColor("#FFA500")
                "red" -> Color.parseColor("#FF2A2A")
                "sepia" -> Color.parseColor("#E8A858")
                "dimmer" -> Color.parseColor("#151515")
                "custom" -> {
                    try {
                        Color.parseColor(tintCustomHex)
                    } catch (_: Exception) {
                        Color.parseColor("#FFA500")
                    }
                }
                else -> Color.parseColor("#FFA500")
            }
            tintPaint.color = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
        }

        private var patternBitmap: Bitmap? = null
        private val noiseBitmaps = arrayOfNulls<Bitmap>(6)
        private val noiseShaders = arrayOfNulls<BitmapShader>(6)
        private val inversionBitmaps = arrayOfNulls<Bitmap>(2)
        private val inversionShaders = arrayOfNulls<BitmapShader>(2)
        private val pixelShiftFilters = arrayOfNulls<ColorMatrixColorFilter>(6)

        private fun clearPatterns() {
            patternBitmap?.recycle()
            patternBitmap = null
            paint.shader = null

            for (i in 0 until 6) {
                noiseBitmaps[i]?.recycle()
                noiseBitmaps[i] = null
                noiseShaders[i] = null
                pixelShiftFilters[i] = null
            }
            for (i in 0 until 2) {
                inversionBitmaps[i]?.recycle()
                inversionBitmaps[i] = null
                inversionShaders[i] = null
            }
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            clearPatterns()
        }

        fun cleanup() {
            clearPatterns()
        }

        fun updateDensity(density: Float) {
            currentDensity = density
            rebuildPattern()
        }

        fun updateOpacity(opacity: Float) {
            currentOpacity = opacity
            rebuildPattern()
        }

        fun updateFilterType(type: String) {
            currentFilterType = type
            rebuildPattern()
        }

        fun animateShift(t: Float) {
            shiftX = t * maxShiftX
            shiftY = t * maxShiftY
            frameCount++

            if (currentFilterType == "noise") {
                val index = (frameCount % 6)
                paint.shader = noiseShaders[index]
            } else if (currentFilterType == "dynamic_inversion") {
                val phase = (frameCount / 12) % 2
                paint.shader = inversionShaders[phase]
            } else if (currentFilterType == "pixel_shift") {
                val index = (frameCount % 6)
                pixelShiftPaint.colorFilter = pixelShiftFilters[index]
            }

            invalidate()
        }

        @SuppressLint("UseKtx")
        fun rebuildPattern() {
            clearPatterns()

            val alpha = (currentOpacity * 255).toInt().coerceIn(0, 255)
            val dm: DisplayMetrics = resources.displayMetrics
            val screenDensity = dm.density

            val maxSizePx = (12f * screenDensity).toInt().coerceAtLeast(4)
            val sizePx = Math.round((1f - currentDensity) * (maxSizePx - 1) + 1).toInt().coerceAtLeast(1)

            maxShiftX = (sizePx * 2f).coerceAtLeast(2f)
            maxShiftY = (sizePx * 2f).coerceAtLeast(2f)

            if (currentFilterType == "pixel_shift") {
                useShaderDraw = false
                paint.shader = null
                val strength = (currentDensity * 0.25f).coerceIn(0.02f, 0.45f)

                for (phase in 0 until 6) {
                    val matrix = when (phase) {
                        0 -> ColorMatrix(
                            floatArrayOf(
                                1f - strength, strength, 0f, 0f, 0f,
                                0f, 1f - strength, strength, 0f, 0f,
                                strength, 0f, 1f - strength, 0f, 0f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                        1 -> ColorMatrix(
                            floatArrayOf(
                                1f - strength, 0f, strength, 0f, 0f,
                                strength, 1f - strength, 0f, 0f, 0f,
                                0f, strength, 1f - strength, 0f, 0f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                        2 -> ColorMatrix(
                            floatArrayOf(
                                1f, 0f, 0f, 0f, 0f,
                                0f, 1f - strength, strength, 0f, 0f,
                                0f, strength, 1f - strength, 0f, 0f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                        3 -> ColorMatrix(
                            floatArrayOf(
                                1f - strength, strength, 0f, 0f, 0f,
                                strength, 1f, 0f, 0f, 0f,
                                0f, 0f, 1f - strength, 0f, 0f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                        4 -> ColorMatrix(
                            floatArrayOf(
                                1f - strength * 0.5f, 0f, strength * 0.5f, 0f, 0f,
                                0f, 1f, 0f, 0f, 0f,
                                strength * 0.5f, 0f, 1f - strength * 0.5f, 0f, 0f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                        else -> ColorMatrix(
                            floatArrayOf(
                                1f, 0f, 0f, 0f, 0f,
                                0f, 1f, 0f, 0f, 0f,
                                0f, 0f, 1f, 0f, 0f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                    }
                    pixelShiftFilters[phase] = ColorMatrixColorFilter(matrix)
                }
                pixelShiftPaint.color =
                    Color.argb((alpha * 0.15f).toInt().coerceIn(1, 40), 128, 128, 128)
                pixelShiftPaint.colorFilter = pixelShiftFilters[0]
                invalidate()
                return
            } else {
                useShaderDraw = true
                pixelShiftPaint.colorFilter = null
            }

            if (currentFilterType == "noise") {
                val ts = (sizePx * 4).coerceAtLeast(4)
                val noiseDensity = (0.15f + currentDensity * 0.75f).coerceIn(0.1f, 0.95f)

                for (i in 0 until 6) {
                    val bmp = Bitmap.createBitmap(ts, ts, Bitmap.Config.ARGB_8888)

                    val rand = java.util.Random((i * 12345L) + 1L)
                    for (x in 0 until ts) {
                        for (y in 0 until ts) {
                            if (rand.nextFloat() < noiseDensity) {
                                val a = (alpha * (0.5f + rand.nextFloat() * 0.5f)).toInt()
                                    .coerceIn(0, 255)
                                bmp.setPixel(x, y, Color.argb(a, 0, 0, 0))
                            }
                        }
                    }
                    noiseBitmaps[i] = bmp
                    noiseShaders[i] =
                        BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                }
                paint.shader = noiseShaders[0]
                invalidate()
                return
            }

            if (currentFilterType == "dynamic_inversion") {
                val squareSize = sizePx.coerceAtLeast(1)
                val tileSize = squareSize * 2
                for (phase in 0..1) {
                    val bmp = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
                    for (x in 0 until tileSize) {
                        for (y in 0 until tileSize) {
                            if (((x / squareSize) + (y / squareSize)) % 2 == phase) {
                                bmp.setPixel(x, y, Color.argb(alpha, 0, 0, 0))
                            }
                        }
                    }
                    inversionBitmaps[phase] = bmp
                    inversionShaders[phase] = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                }
                paint.shader = inversionShaders[0]
                invalidate()
                return
            }

            val bitmap: Bitmap = when (currentFilterType) {
                "dots" -> {
                    val ts = (sizePx * 2).coerceAtLeast(2)
                    Bitmap.createBitmap(ts, ts, Bitmap.Config.ARGB_8888).apply {
                        for (x in 0 until ts) {
                            for (y in 0 until ts) {
                                if ((x + y) % 2 == 0) {
                                    setPixel(x, y, Color.argb(alpha, 0, 0, 0))
                                }
                            }
                        }
                    }
                }

                "pentile_matrix" -> {
                    val squareSize = sizePx.coerceAtLeast(1)
                    val tileSize = squareSize * 2
                    Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888).apply {
                        for (x in 0 until tileSize) {
                            for (y in 0 until tileSize) {
                                val dx = Math.abs(x - squareSize + 0.5f)
                                val dy = Math.abs(y - squareSize + 0.5f)
                                if ((dx + dy) <= squareSize) {
                                    setPixel(x, y, Color.argb(alpha, 0, 0, 0))
                                }
                            }
                        }
                    }
                }

                "blue_shield" -> {
                    val squareSize = sizePx.coerceAtLeast(1)
                    val tileSize = (squareSize * 2).coerceAtLeast(2)
                    val blueCutAlpha = (alpha * 0.95f).toInt().coerceIn(0, 255)
                    val warmAlpha = (alpha * 0.5f).toInt().coerceIn(0, 255)
                    Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888).apply {
                        for (x in 0 until tileSize) {
                            for (y in 0 until tileSize) {
                                if ((x + y) % 2 == 0) {
                                    setPixel(x, y, Color.argb(blueCutAlpha, 0, 0, 0))
                                } else {
                                    setPixel(x, y, Color.argb(warmAlpha, 255, 175, 0))
                                }
                            }
                        }
                    }
                }

                "checker_grid" -> {
                    val squareSize = sizePx.coerceAtLeast(1)
                    val tileSize = squareSize * 2
                    Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888).apply {
                        for (x in 0 until tileSize) {
                            for (y in 0 until tileSize) {
                                if (((x / squareSize) + (y / squareSize)) % 2 == 0) {
                                    setPixel(x, y, Color.argb(alpha, 0, 0, 0))
                                }
                            }
                        }
                    }
                }

                else -> {
                    val squareSize = sizePx.coerceAtLeast(1)
                    val tileSize = squareSize * 2
                    Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888).apply {
                        for (x in 0 until tileSize) {
                            for (y in 0 until tileSize) {
                                if (((x / squareSize) + (y / squareSize)) % 2 == 0) {
                                    setPixel(x, y, Color.argb(alpha, 0, 0, 0))
                                }
                            }
                        }
                    }
                }
            }

            patternBitmap = bitmap
            val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            paint.shader = shader
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (tintEnabled && tintIntensity > 0f) {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), tintPaint)
            }
            if (useShaderDraw) {
                canvas.withTranslation(shiftX, shiftY) {
                    drawRect(-shiftX, -shiftY, width.toFloat(), height.toFloat(), paint)
                }
            } else {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), pixelShiftPaint)
            }
        }
    }
}
