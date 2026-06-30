package io.github.itsmelissadev.swiftsense.feature.alwaysondisplay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AodOverlayView(context: Context) : View(context) {

    var isAodVisible = false
        set(value) {
            field = value
            if (value) {
                visibility = VISIBLE
                startTicking()
            } else {
                visibility = GONE
                stopTicking()
            }
        }

    var batteryLevel = 100
        set(value) { field = value; invalidate() }
    var batteryIsCharging = false
        set(value) { field = value; invalidate() }
    var notificationCount = 0
        set(value) { field = value; invalidate() }

    var showClock = true
        set(value) { field = value; invalidate() }
    var showDate = true
        set(value) { field = value; invalidate() }
    var showBattery = true
        set(value) { field = value; invalidate() }
    var showNotifications = true
        set(value) { field = value; invalidate() }
    var clockStyle = "digital"
        set(value) { field = value; invalidate() }
    var brightness = 0.5f
        set(value) { field = value; invalidate() }
    var clockColor = Color.WHITE
        set(value) { field = value; invalidate() }
    var batteryColor = Color.WHITE
        set(value) { field = value; invalidate() }
    var batteryStyle = "horizontal_classic"
        set(value) { field = value; invalidate() }
    var showWattage = false
        set(value) { field = value; invalidate() }
    var chargingWatts = 0f
        set(value) { field = value; invalidate() }
    var chargingCurrentMa = 0
        set(value) { field = value; invalidate() }
    var fontFamily = "monospace"
        set(value) {
            field = value
            updateTypefaces()
            invalidate()
        }
    var burnInProtection = true
    var burnInInterval = 20
    var burnInMode = "jump"
    var burnInRgbShift = false
    
    private var activeClockColor = Color.WHITE
    private var activeBatteryColor = Color.WHITE
    
    private var xOffset = 0f
    private var yOffset = 0f
    private var dx = 2f
    private var dy = 2f
    private var lastBurnInShiftTime = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            invalidate()
            val delay = if (burnInProtection && burnInMode == "bounce") 33L else 1000L
            handler.postDelayed(this, delay)
        }
    }

    private val backgroundPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
    }

    private val timePaint = Paint().apply {
        isAntiAlias = true
    }

    private val datePaint = Paint().apply {
        isAntiAlias = true
    }

    private val subPaint = Paint().apply {
        isAntiAlias = true
    }
    
    private fun updateTypefaces() {
        val typeface = when (fontFamily) {
            "monospace" -> Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            "serif" -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            else -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        timePaint.typeface = typeface
        datePaint.typeface = typeface
        subPaint.typeface = typeface
    }

    private val clockHandPaint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    private val circlePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    init {
        visibility = GONE
        setBackgroundColor(Color.BLACK)
        updateTypefaces()
    }

    private fun updateColors() {
        if (!burnInRgbShift) {
            activeClockColor = clockColor
            activeBatteryColor = batteryColor
            return
        }

        val t = System.currentTimeMillis() / 2000.0
        val cr = ((sin(t) * 0.5 + 0.5) * 50).toInt()
        val cg = ((sin(t + 2) * 0.5 + 0.5) * 50).toInt()
        val cb = ((sin(t + 4) * 0.5 + 0.5) * 50).toInt()

        activeClockColor = Color.argb(
            255,
            min(255, Color.red(clockColor) + cr),
            min(255, Color.green(clockColor) + cg),
            min(255, Color.blue(clockColor) + cb)
        )
        
        activeBatteryColor = Color.argb(
            255,
            min(255, Color.red(batteryColor) + cr),
            min(255, Color.green(batteryColor) + cg),
            min(255, Color.blue(batteryColor) + cb)
        )
    }

    private fun startTicking() {
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
    }

    private fun stopTicking() {
        handler.removeCallbacks(tickRunnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPaint(backgroundPaint)

        if (burnInProtection) {
            val maxShiftX = Math.max(0f, width / 2f - 120.dp)
            val maxShiftY = Math.max(0f, height / 2f - 180.dp)
            
            if (burnInMode == "bounce") {
                xOffset += dx
                yOffset += dy
                
                if (xOffset > maxShiftX) {
                    xOffset = maxShiftX
                    dx = -Math.abs(dx)
                } else if (xOffset < -maxShiftX) {
                    xOffset = -maxShiftX
                    dx = Math.abs(dx)
                }
                
                if (yOffset > maxShiftY) {
                    yOffset = maxShiftY
                    dy = -Math.abs(dy)
                } else if (yOffset < -maxShiftY) {
                    yOffset = -maxShiftY
                    dy = Math.abs(dy)
                }
            } else {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBurnInShiftTime > burnInInterval * 1000L) {
                    lastBurnInShiftTime = currentTime
                    xOffset = (Math.random() * maxShiftX * 2 - maxShiftX).toFloat()
                    yOffset = (Math.random() * maxShiftY * 2 - maxShiftY).toFloat()
                }
            }
        } else {
            xOffset = 0f
            yOffset = 0f
        }

        activeClockColor = if (burnInProtection && burnInRgbShift) {
            val hue = ((System.currentTimeMillis() / 40L) % 360L).toFloat()
            Color.HSVToColor(floatArrayOf(hue, 0.8f, 1f))
        } else {
            clockColor
        }

        activeBatteryColor = if (burnInProtection && burnInRgbShift) {
            val hue = ((System.currentTimeMillis() / 40L) % 360L).toFloat()
            Color.HSVToColor(floatArrayOf(hue, 0.8f, 1f))
        } else {
            batteryColor
        }

        val cx = width / 2f + xOffset
        var cy = height / 2f + yOffset

        if (showClock) {
            when (clockStyle) {
                "digital"           -> cy = drawDigitalClock(canvas, cx, cy)
                "digital_glow_cyan" -> cy = drawGlowCyanClock(canvas, cx, cy)
                "modern_stacked"    -> cy = drawStackedClock(canvas, cx, cy)
                "modern_glow"       -> cy = drawGlowClock(canvas, cx, cy)
                "analog"            -> cy = drawAnalogClock(canvas, cx, cy)
                "glow_rose"         -> cy = drawGlowRoseClock(canvas, cx, cy)
                "digital_amber"     -> cy = drawAmberClock(canvas, cx, cy)
                "matrix"            -> cy = drawMatrixClock(canvas, cx, cy)
                "minimal_dot"       -> cy = drawMinimalDotClock(canvas, cx, cy)
                "dual_tone"         -> cy = drawDualToneClock(canvas, cx, cy)
                else                -> cy = drawDigitalClock(canvas, cx, cy)
            }
            cy += 8.dp
        }

        if (showDate) {
            cy = drawDate(canvas, cx, cy)
            cy += 16.dp
        }

        drawStatusRow(canvas, cx, cy)
    }

    private fun drawStackedClock(canvas: Canvas, cx: Float, cy: Float): Float {
        val cal = Calendar.getInstance()
        val hours = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.HOUR_OF_DAY))
        val minutes = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MINUTE))
        
        timePaint.textSize = 96.sp
        timePaint.textAlign = Paint.Align.CENTER
        timePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        
        val alpha = (255 * brightness).toInt()
        val r = Color.red(activeClockColor)
        val g = Color.green(activeClockColor)
        val b = Color.blue(activeClockColor)
        timePaint.color = Color.argb(alpha, r, g, b)
        
        val hourY = cy - timePaint.ascent() / 2f
        canvas.drawText(hours, cx, hourY, timePaint)
        
        val minuteY = hourY + timePaint.descent() - timePaint.ascent() - 16.dp
        canvas.drawText(minutes, cx, minuteY, timePaint)
        
        return minuteY + timePaint.descent() + 8.dp
    }
    
    private fun drawGlowClock(canvas: Canvas, cx: Float, cy: Float): Float {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val text = format.format(Date())
        timePaint.textSize = 80.sp
        timePaint.textAlign = Paint.Align.CENTER
        
        val alpha = (255 * brightness).toInt()
        val r = Color.red(activeClockColor)
        val g = Color.green(activeClockColor)
        val b = Color.blue(activeClockColor)
        timePaint.color = Color.argb(alpha, r, g, b)
        timePaint.setShadowLayer(15f, 0f, 0f, Color.argb(alpha, r, g, b))
        
        val textY = cy - (timePaint.descent() + timePaint.ascent()) / 2f
        canvas.drawText(text, cx, textY, timePaint)
        timePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        return textY + timePaint.descent() + 8.dp
    }

    private fun drawGlowCyanClock(canvas: Canvas, cx: Float, cy: Float): Float {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val text = format.format(Date())
        timePaint.textSize = 72.sp
        timePaint.textAlign = Paint.Align.CENTER
        val alpha = (255 * brightness).toInt()
        val r = Color.red(activeClockColor)
        val g = Color.green(activeClockColor)
        val b = Color.blue(activeClockColor)
        timePaint.color = Color.argb(alpha, r, g, b)
        timePaint.setShadowLayer(20f, 0f, 0f, Color.argb((alpha * 0.8f).toInt(), r, g, b))
        val textY = cy - (timePaint.descent() + timePaint.ascent()) / 2f
        canvas.drawText(text, cx, textY, timePaint)
        timePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        return textY + timePaint.descent() + 8.dp
    }

    private fun drawGlowRoseClock(canvas: Canvas, cx: Float, cy: Float): Float {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val text = format.format(Date())
        timePaint.textSize = 72.sp
        timePaint.textAlign = Paint.Align.CENTER
        val alpha = (255 * brightness).toInt()
        val r = Color.red(activeClockColor)
        val g = Color.green(activeClockColor)
        val b = Color.blue(activeClockColor)
        timePaint.color = Color.argb(alpha, r, g, b)
        timePaint.setShadowLayer(18f, 0f, 0f, Color.argb((alpha * 0.7f).toInt(), r, g, b))
        val textY = cy - (timePaint.descent() + timePaint.ascent()) / 2f
        canvas.drawText(text, cx, textY, timePaint)
        timePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        val lineAlpha = (120 * brightness).toInt()
        val linePaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(lineAlpha, r, g, b)
            strokeWidth = 1.5f.dp
        }
        canvas.drawLine(cx - 60.dp, textY + timePaint.descent() + 6.dp, cx + 60.dp, textY + timePaint.descent() + 6.dp, linePaint)
        return textY + timePaint.descent() + 16.dp
    }

    private fun drawAmberClock(canvas: Canvas, cx: Float, cy: Float): Float {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val text = format.format(Date())
        timePaint.textSize = 68.sp
        timePaint.textAlign = Paint.Align.CENTER
        val alpha = (255 * brightness).toInt()
        val r = Color.red(activeClockColor)
        val g = Color.green(activeClockColor)
        val b = Color.blue(activeClockColor)
        timePaint.color = Color.argb(alpha, r, g, b)
        timePaint.setShadowLayer(8f, 0f, 0f, Color.argb((alpha * 0.6f).toInt(), r, g, b))
        val textY = cy - (timePaint.descent() + timePaint.ascent()) / 2f
        canvas.drawText(text, cx, textY, timePaint)
        timePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        return textY + timePaint.descent() + 8.dp
    }

    private fun drawMatrixClock(canvas: Canvas, cx: Float, cy: Float): Float {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val text = format.format(Date())
        timePaint.textSize = 72.sp
        timePaint.textAlign = Paint.Align.CENTER
        val alpha = (255 * brightness).toInt()
        val r = Color.red(activeClockColor)
        val g = Color.green(activeClockColor)
        val b = Color.blue(activeClockColor)
        timePaint.color = Color.argb(alpha, r, g, b)
        timePaint.setShadowLayer(25f, 0f, 0f, Color.argb(alpha, r, g, b))
        val textY = cy - (timePaint.descent() + timePaint.ascent()) / 2f
        canvas.drawText(text, cx, textY, timePaint)
        timePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        val dimAlpha = (80 * brightness).toInt()
        val smallPaint = Paint(subPaint).apply {
            textSize = 12.sp
            textAlign = Paint.Align.CENTER
            color = Color.argb(dimAlpha, r, g, b)
        }
        val chars = listOf("0","1","0","1","0","1","0","1","0","1","0","1")
        chars.forEachIndexed { i, c ->
            val col = i % 4
            val row = i / 4
            canvas.drawText(c, cx - 18.dp + col * 12.dp, textY + timePaint.descent() + 10.dp + row * 14.dp, smallPaint)
        }
        return textY + timePaint.descent() + 10.dp + 3 * 14.dp
    }

    private fun drawMinimalDotClock(canvas: Canvas, cx: Float, cy: Float): Float {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val text = format.format(Date())
        timePaint.textSize = 72.sp
        timePaint.textAlign = Paint.Align.CENTER
        val alpha = (255 * brightness).toInt()
        val r = Color.red(activeClockColor)
        val g = Color.green(activeClockColor)
        val b = Color.blue(activeClockColor)
        timePaint.color = Color.argb(alpha, r, g, b)
        timePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        val textY = cy - (timePaint.descent() + timePaint.ascent()) / 2f
        canvas.drawText(text, cx, textY, timePaint)
        val dotAlpha = (100 * brightness).toInt()
        val dotPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.argb(dotAlpha, r, g, b)
        }
        val dotY = textY + timePaint.descent() + 10.dp
        canvas.drawCircle(cx - 20.dp, dotY, 3.dp, dotPaint)
        canvas.drawCircle(cx, dotY, 3.dp, dotPaint)
        canvas.drawCircle(cx + 20.dp, dotY, 3.dp, dotPaint)
        return dotY + 3.dp + 8.dp
    }

    private fun drawDualToneClock(canvas: Canvas, cx: Float, cy: Float): Float {
        val cal = Calendar.getInstance()
        val hours = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.HOUR_OF_DAY))
        val minutes = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MINUTE))
        timePaint.textSize = 72.sp
        timePaint.textAlign = Paint.Align.CENTER
        timePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        val alpha = (255 * brightness).toInt()
        val r = Color.red(activeClockColor)
        val g = Color.green(activeClockColor)
        val b = Color.blue(activeClockColor)
        timePaint.color = Color.argb(alpha, r, g, b)
        val charW = timePaint.measureText("00") / 2f
        val totalW = timePaint.measureText("00:00")
        val textY = cy - (timePaint.descent() + timePaint.ascent()) / 2f
        canvas.drawText(hours, cx - totalW / 2f + charW, textY, timePaint)
        val dimAlpha = (alpha * 0.45f).toInt()
        timePaint.color = Color.argb(dimAlpha, r, g, b)
        canvas.drawText(":", cx, textY, timePaint)
        canvas.drawText(minutes, cx + totalW / 2f - charW, textY, timePaint)
        return textY + timePaint.descent() + 8.dp
    }

    private fun drawDigitalClock(canvas: Canvas, cx: Float, cy: Float): Float {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val text = format.format(Date())
        timePaint.textSize = 72.sp
        timePaint.textAlign = Paint.Align.CENTER
        
        val alpha = (255 * brightness).toInt()
        val r = Color.red(activeClockColor)
        val g = Color.green(activeClockColor)
        val b = Color.blue(activeClockColor)
        timePaint.color = Color.argb(alpha, r, g, b)
        timePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        
        val textY = cy - (timePaint.descent() + timePaint.ascent()) / 2f
        canvas.drawText(text, cx, textY, timePaint)
        return textY + timePaint.descent() + 8.dp
    }

    private fun drawAnalogClock(canvas: Canvas, cx: Float, cy: Float): Float {
        val radius = 80.dp
        
        val r = Color.red(activeClockColor)
        val g = Color.green(activeClockColor)
        val b = Color.blue(activeClockColor)
        
        val baseAlpha = (255 * brightness).toInt()
        val lowAlpha = (50 * brightness).toInt()
        val medAlpha = (180 * brightness).toInt()
        
        circlePaint.color = Color.argb(lowAlpha, r, g, b)
        circlePaint.strokeWidth = 2.dp
        canvas.drawCircle(cx, cy, radius, circlePaint)

        val cal = Calendar.getInstance()
        val hours = cal.get(Calendar.HOUR)
        val minutes = cal.get(Calendar.MINUTE)

        val hourAngle = Math.toRadians(hours * 30 + minutes * 0.5 - 90)
        clockHandPaint.color = Color.argb(baseAlpha, r, g, b)
        clockHandPaint.strokeWidth = 4.dp
        canvas.drawLine(cx, cy,
            cx + (radius * 0.5f * cos(hourAngle)).toFloat(),
            cy + (radius * 0.5f * sin(hourAngle)).toFloat(), clockHandPaint)

        val minuteAngle = Math.toRadians((minutes * 6 - 90).toDouble())
        clockHandPaint.color = Color.argb(medAlpha, r, g, b)
        clockHandPaint.strokeWidth = 3.dp
        canvas.drawLine(cx, cy,
            cx + (radius * 0.8f * cos(minuteAngle)).toFloat(),
            cy + (radius * 0.8f * sin(minuteAngle)).toFloat(), clockHandPaint)

        return cy + radius + 8.dp
    }

    private fun drawDate(canvas: Canvas, cx: Float, cy: Float): Float {
        val format = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        val text = format.format(Date())
        datePaint.textSize = 18.sp
        datePaint.textAlign = Paint.Align.CENTER
        
        val r = Color.red(activeClockColor)
        val g = Color.green(activeClockColor)
        val b = Color.blue(activeClockColor)
        val medAlpha = (180 * brightness).toInt()
        datePaint.color = Color.argb(medAlpha, r, g, b)
        
        canvas.drawText(text, cx, cy, datePaint)
        return cy + datePaint.descent() - datePaint.ascent()
    }

    private fun measureBatteryIconWidth(): Float {
        return when (batteryStyle) {
            "horizontal_classic" -> 20f.dp
            "vertical_classic" -> 10f.dp
            "minimal_bar" -> 24f.dp
            "circular" -> 14f.dp
            "dotted" -> 24f.dp
            "pill" -> 22f.dp
            "neon_outline" -> 20f.dp
            "segmented" -> 20f.dp
            "leaf" -> 16f.dp
            "text_only" -> 0f
            else -> 20f.dp
        }
    }

    private fun drawBatteryIcon(canvas: Canvas, cx: Float, cy: Float, paint: Paint) {
        val r = Color.red(activeBatteryColor)
        val g = Color.green(activeBatteryColor)
        val b = Color.blue(activeBatteryColor)
        val medAlpha = (180 * brightness).toInt()
        paint.color = Color.argb(medAlpha, r, g, b)
        val level = batteryLevel

        when (batteryStyle) {
            "horizontal_classic" -> {
                val bw = 18f.dp
                val bh = 10f.dp
                val cw = 2f.dp
                val ch = 4f.dp
                val top = cy - bh / 2f
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f.dp
                canvas.drawRoundRect(cx, top, cx + bw, top + bh, 2f.dp, 2f.dp, paint)
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(cx + bw, cy - ch / 2f, cx + bw + cw, cy + ch / 2f, 1f.dp, 1f.dp, paint)
                val lw = (bw - 3f.dp) * (level / 100f)
                canvas.drawRoundRect(cx + 1.5f.dp, top + 1.5f.dp, cx + 1.5f.dp + lw, top + bh - 1.5f.dp, 1f.dp, 1f.dp, paint)
            }
            "vertical_classic" -> {
                val bw = 10f.dp
                val bh = 16f.dp
                val cw = 4f.dp
                val ch = 2f.dp
                val top = cy - bh / 2f
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f.dp
                canvas.drawRoundRect(cx, top, cx + bw, top + bh, 2f.dp, 2f.dp, paint)
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(cx + bw / 2f - cw / 2f, top - ch, cx + bw / 2f + cw / 2f, top, 1f.dp, 1f.dp, paint)
                val lh = (bh - 3f.dp) * (level / 100f)
                canvas.drawRoundRect(cx + 1.5f.dp, top + bh - 1.5f.dp - lh, cx + bw - 1.5f.dp, top + bh - 1.5f.dp, 1f.dp, 1f.dp, paint)
            }
            "minimal_bar" -> {
                val bw = 24f.dp
                val bh = 4f.dp
                val top = cy - bh / 2f
                paint.style = Paint.Style.FILL
                paint.color = Color.argb((80 * brightness).toInt(), r, g, b)
                canvas.drawRoundRect(cx, top, cx + bw, top + bh, 2f.dp, 2f.dp, paint)
                paint.color = Color.argb(medAlpha, r, g, b)
                val lw = bw * (level / 100f)
                canvas.drawRoundRect(cx, top, cx + lw, top + bh, 2f.dp, 2f.dp, paint)
            }
            "circular" -> {
                val radius = 7f.dp
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f.dp
                paint.color = Color.argb((80 * brightness).toInt(), r, g, b)
                canvas.drawCircle(cx + radius, cy, radius, paint)
                paint.color = Color.argb(medAlpha, r, g, b)
                val sweep = 360f * (level / 100f)
                canvas.drawArc(cx, cy - radius, cx + radius * 2f, cy + radius, -90f, sweep, false, paint)
            }
            "dotted" -> {
                val dots = 5
                val activeDots = Math.ceil(((level / 100f) * dots).toDouble()).toInt()
                val dotRadius = 1.5f.dp
                val spacing = 4.5f.dp
                paint.style = Paint.Style.FILL
                for (i in 0 until dots) {
                    paint.color = Color.argb(if (i < activeDots) medAlpha else (80 * brightness).toInt(), r, g, b)
                    canvas.drawCircle(cx + dotRadius + i * spacing, cy, dotRadius, paint)
                }
            }
            "pill" -> {
                val bw = 22f.dp
                val bh = 10f.dp
                val top = cy - bh / 2f
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f.dp
                canvas.drawRoundRect(cx, top, cx + bw, top + bh, bh / 2f, bh / 2f, paint)
                paint.style = Paint.Style.FILL
                val lw = (bw - 4f.dp) * (level / 100f)
                if (lw > 0) {
                    canvas.drawRoundRect(cx + 2f.dp, top + 2f.dp, cx + 2f.dp + lw, top + bh - 2f.dp, (bh - 4f.dp) / 2f, (bh - 4f.dp) / 2f, paint)
                }
            }
            "neon_outline" -> {
                val bw = 20f.dp
                val bh = 12f.dp
                val top = cy - bh / 2f
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f.dp
                paint.setShadowLayer(8f.dp, 0f, 0f, paint.color)
                canvas.drawRoundRect(cx, top, cx + bw, top + bh, 3f.dp, 3f.dp, paint)
                paint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                paint.style = Paint.Style.FILL
                val lw = (bw - 4f.dp) * (level / 100f)
                canvas.drawRoundRect(cx + 2f.dp, top + 2f.dp, cx + 2f.dp + lw, top + bh - 2f.dp, 1.5f.dp, 1.5f.dp, paint)
            }
            "segmented" -> {
                val bw = 20f.dp
                val bh = 10f.dp
                val segments = 4
                val top = cy - bh / 2f
                val activeSegments = Math.ceil(((level / 100f) * segments).toDouble()).toInt()
                val segW = (bw - (segments - 1) * 2f.dp) / segments
                paint.style = Paint.Style.FILL
                for (i in 0 until segments) {
                    paint.color = Color.argb(if (i < activeSegments) medAlpha else (80 * brightness).toInt(), r, g, b)
                    val sx = cx + i * (segW + 2f.dp)
                    canvas.drawRect(sx, top, sx + segW, top + bh, paint)
                }
            }
            "leaf" -> {
                val bw = 16f.dp
                val bh = 10f.dp
                val top = cy - bh / 2f
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f.dp
                val path = android.graphics.Path()
                path.moveTo(cx, top + bh)
                path.cubicTo(cx, top, cx + bw / 2f, top, cx + bw, top)
                path.cubicTo(cx + bw, top + bh, cx + bw / 2f, top + bh, cx, top + bh)
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL
                val lw = bw * (level / 100f)
                canvas.save()
                canvas.clipRect(cx, top, cx + lw, top + bh)
                canvas.drawPath(path, paint)
                canvas.restore()
            }
            "text_only" -> {}
            else -> {
                val bw = 18f.dp
                val bh = 10f.dp
                val cw = 2f.dp
                val ch = 4f.dp
                val top = cy - bh / 2f
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f.dp
                canvas.drawRoundRect(cx, top, cx + bw, top + bh, 2f.dp, 2f.dp, paint)
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(cx + bw, cy - ch / 2f, cx + bw + cw, cy + ch / 2f, 1f.dp, 1f.dp, paint)
                val lw = (bw - 3f.dp) * (level / 100f)
                canvas.drawRoundRect(cx + 1.5f.dp, top + 1.5f.dp, cx + 1.5f.dp + lw, top + bh - 1.5f.dp, 1f.dp, 1f.dp, paint)
            }
        }
    }

    private fun drawStatusRow(canvas: Canvas, cx: Float, cy: Float) {
        subPaint.textSize = 14f.sp
        subPaint.textAlign = Paint.Align.LEFT

        val r = Color.red(activeClockColor)
        val g = Color.green(activeClockColor)
        val b = Color.blue(activeClockColor)
        val medAlpha = (180 * brightness).toInt()

        val items = mutableListOf<String>()
        if (showNotifications && notificationCount > 0) {
            items.add("🔔 $notificationCount")
        }

        val boltSize = 10f.dp
        val textGap = 5f.dp
        val percentText = "$batteryLevel%"
        subPaint.textSize = 13f.sp
        val percentW = subPaint.measureText(percentText)

        val chargingInfoText: String? = if (showWattage && batteryIsCharging) {
            val hasMa = chargingCurrentMa > 0
            val hasW = chargingWatts > 0.05f
            when {
                hasMa && hasW -> {
                    val wStr = if (chargingWatts >= 10f) "${chargingWatts.toInt()}W"
                               else String.format("%.1fW", chargingWatts)
                    "${chargingCurrentMa}mA · $wStr"
                }
                hasMa -> "${chargingCurrentMa}mA"
                hasW -> {
                    val wStr = if (chargingWatts >= 10f) "${chargingWatts.toInt()}W"
                               else String.format("%.1fW", chargingWatts)
                    wStr
                }
                else -> null
            }
        } else null
        val chargingInfoW = if (chargingInfoText != null) subPaint.measureText(chargingInfoText) + 8f.dp else 0f

        var totalWidth = 0f
        if (showBattery) {
            val iconW = measureBatteryIconWidth()
            val extraGap = if (batteryIsCharging) boltSize + 4f.dp else 0f
            totalWidth += iconW + (if (iconW > 0) textGap else 0f) + percentW + chargingInfoW + extraGap
            if (items.isNotEmpty()) totalWidth += 16f.dp
        }
        if (items.isNotEmpty()) {
            totalWidth += subPaint.measureText(items.joinToString("   "))
        }

        var currentX = cx - totalWidth / 2f
        val midY = cy + 4f.dp

        if (showBattery) {
            val batPaint = Paint(subPaint)
            val iconW = measureBatteryIconWidth()
            
            drawBatteryIcon(canvas, currentX, midY, batPaint)
            currentX += iconW

            if (batteryIsCharging) {
                val boltPaint = Paint()
                boltPaint.isAntiAlias = true
                boltPaint.style = Paint.Style.FILL
                val br = Color.red(activeBatteryColor)
                val bg = Color.green(activeBatteryColor)
                val bb = Color.blue(activeBatteryColor)
                boltPaint.color = Color.argb(medAlpha, br, bg, bb)

                val boltCx = currentX + 3f.dp + boltSize / 2f
                val boltHalfH = boltSize / 2f
                val boltPath = android.graphics.Path()
                boltPath.moveTo(boltCx + boltSize * 0.30f, midY - boltHalfH)
                boltPath.lineTo(boltCx - boltSize * 0.20f, midY)
                boltPath.lineTo(boltCx + boltSize * 0.05f, midY)
                boltPath.lineTo(boltCx - boltSize * 0.30f, midY + boltHalfH)
                boltPath.lineTo(boltCx + boltSize * 0.20f, midY - 0.06f * boltSize)
                boltPath.lineTo(boltCx - boltSize * 0.05f, midY - 0.06f * boltSize)
                boltPath.close()
                canvas.drawPath(boltPath, boltPaint)
                currentX += boltSize + 4f.dp
            }

            if (iconW > 0) currentX += textGap
            
            val textR = Color.red(activeBatteryColor)
            val textG = Color.green(activeBatteryColor)
            val textB = Color.blue(activeBatteryColor)
            subPaint.color = Color.argb(medAlpha, textR, textG, textB)
            subPaint.textAlign = Paint.Align.LEFT
            val textY = midY - (subPaint.descent() + subPaint.ascent()) / 2f
            canvas.drawText(percentText, currentX, textY, subPaint)
            currentX += percentW

            if (chargingInfoText != null) {
                subPaint.color = Color.argb((medAlpha * 0.80f).toInt(), textR, textG, textB)
                canvas.drawText(chargingInfoText, currentX + 6f.dp, textY, subPaint)
                currentX += chargingInfoW
            }

            currentX += 16f.dp
        }

        if (items.isNotEmpty()) {
            subPaint.color = Color.argb(medAlpha, r, g, b)
            val notifText = items.joinToString("   ")
            val textY = midY - (subPaint.descent() + subPaint.ascent()) / 2f
            canvas.drawText(notifText, currentX, textY, subPaint)
        }
    }

    private val Float.dp: Float get() = this * resources.displayMetrics.density
    private val Int.dp: Float get() = this * resources.displayMetrics.density
    @Suppress("DEPRECATION")
    private val Float.sp: Float get() = this * resources.displayMetrics.scaledDensity
    @Suppress("DEPRECATION")
    private val Int.sp: Float get() = this * resources.displayMetrics.scaledDensity

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopTicking()
    }
}
