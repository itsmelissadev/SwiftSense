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

        for (region in regions) {
            val view = AmoledFilterView(this@AmoledProtectService)
            view.updateDensity(density)
            view.updateOpacity(opacity)
            view.updateFilterType(filterType)

            val params = createLayoutParams(region)
            try {
                windowManager?.addView(view, params)
                overlayViews[region] = view
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createLayoutParams(region: String): WindowManager.LayoutParams {
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

            else -> { // REGION_FULL_SCREEN
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
                // Smooth mode: Continuous movement
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
                // Jump mode: Stay still, then jump
                var jumpIndex = 0
                while (isActive) {
                    // Calculate a sequence of 4 diagonal positions to cycle through
                    // t = 0.0, 0.33, 0.66, 1.0 (various diagonal points)
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
            if (currentFilterType == "noise" || currentFilterType == "pixel_shift") {
                rebuildPattern()
            }
            invalidate()
        }

        @SuppressLint("UseKtx")
        fun rebuildPattern() {
            val alpha = (currentOpacity * 255).toInt().coerceIn(0, 255)
            val dm: DisplayMetrics = resources.displayMetrics
            val screenDensity = dm.density

            val baseSizePx = ((1f - currentDensity) * 11f + 1f).toInt().coerceIn(1, 12)
            val sizePx = (baseSizePx * screenDensity).toInt().coerceAtLeast(1)

            maxShiftX = sizePx.toFloat() * 2f
            maxShiftY = sizePx.toFloat() * 2f

            when (currentFilterType) {
                "pixel_shift" -> {
                    useShaderDraw = false
                    paint.shader = null
                    val phase = frameCount % 6
                    val strength = currentDensity * 0.12f
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
                    pixelShiftPaint.colorFilter = ColorMatrixColorFilter(matrix)
                    pixelShiftPaint.color =
                        Color.argb((alpha * 0.15f).toInt().coerceIn(1, 40), 128, 128, 128)
                    invalidate()
                    return
                }

                else -> {
                    useShaderDraw = true
                    pixelShiftPaint.colorFilter = null
                }
            }

            val bitmap: Bitmap
            when (currentFilterType) {
                "dots" -> {
                    val ts = (sizePx * 2).coerceAtLeast(2)
                    bitmap = Bitmap.createBitmap(ts, ts, Bitmap.Config.ARGB_8888)
                    for (x in 0 until ts) {
                        for (y in 0 until ts) {
                            if ((x + y) % 2 == 0) {
                                bitmap.setPixel(x, y, Color.argb(alpha, 0, 0, 0))
                            }
                        }
                    }
                }

                "horizontal_lines" -> {
                    val ts = (sizePx * 2).coerceAtLeast(2)
                    bitmap = Bitmap.createBitmap(1, ts, Bitmap.Config.ARGB_8888)
                    for (y in 0 until ts) {
                        if (y < sizePx) bitmap.setPixel(0, y, Color.argb(alpha, 0, 0, 0))
                    }
                }

                "vertical_lines" -> {
                    val ts = (sizePx * 2).coerceAtLeast(2)
                    bitmap = createBitmap(ts, 1)
                    for (x in 0 until ts) {
                        if (x < sizePx) bitmap[x, 0] = Color.argb(alpha, 0, 0, 0)
                    }
                }

                "grid" -> {
                    val ts = (sizePx * 3).coerceAtLeast(3)
                    bitmap = createBitmap(ts, ts)
                    for (i in 0 until ts) {
                        for (j in 0 until sizePx.coerceAtMost(ts)) {
                            bitmap[i, j] = Color.argb(alpha, 0, 0, 0)
                            bitmap.setPixel(j, i, Color.argb(alpha, 0, 0, 0))
                        }
                    }
                }

                "diagonal" -> {
                    val ts = (sizePx * 4).coerceAtLeast(4)
                    bitmap = Bitmap.createBitmap(ts, ts, Bitmap.Config.ARGB_8888)
                    for (i in 0 until ts) {
                        for (w in 0 until sizePx.coerceAtMost(ts)) {
                            bitmap.setPixel(i, (i + w) % ts, Color.argb(alpha, 0, 0, 0))
                        }
                    }
                }

                "noise" -> {
                    val ts = (sizePx * 4).coerceAtLeast(4)
                    bitmap = Bitmap.createBitmap(ts, ts, Bitmap.Config.ARGB_8888)
                    val rand = java.util.Random(frameCount.toLong())
                    val noiseDensity = 0.2f + currentDensity * 0.5f
                    for (x in 0 until ts) {
                        for (y in 0 until ts) {
                            if (rand.nextFloat() < noiseDensity) {
                                val a = (alpha * (0.5f + rand.nextFloat() * 0.5f)).toInt()
                                    .coerceIn(0, 255)
                                bitmap.setPixel(x, y, Color.argb(a, 0, 0, 0))
                            }
                        }
                    }
                }

                "checker_grid" -> {
                    val squareSize = sizePx.coerceAtLeast(1)
                    val tileSize = squareSize * 2
                    bitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
                    for (x in 0 until tileSize) {
                        for (y in 0 until tileSize) {
                            if (((x / squareSize) + (y / squareSize)) % 2 == 0) {
                                bitmap.setPixel(x, y, Color.argb(alpha, 0, 0, 0))
                            }
                        }
                    }
                }

                else -> {
                    bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                    bitmap.setPixel(0, 0, Color.argb(alpha, 0, 0, 0))
                    bitmap.setPixel(2, 2, Color.argb(alpha, 0, 0, 0))
                }
            }

            val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            paint.shader = shader
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
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
