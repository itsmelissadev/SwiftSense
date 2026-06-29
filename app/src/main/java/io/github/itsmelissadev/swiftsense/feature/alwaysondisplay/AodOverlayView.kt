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
    var textColor = Color.WHITE
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
    
    private var activeTextColor = Color.WHITE
    
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

        activeTextColor = if (burnInProtection && burnInRgbShift) {
            val hue = ((System.currentTimeMillis() / 40L) % 360L).toFloat()
            Color.HSVToColor(floatArrayOf(hue, 0.8f, 1f))
        } else {
            textColor
        }

        val cx = width / 2f + xOffset
        var cy = height / 2f + yOffset

        if (showClock) {
            when (clockStyle) {
                "digital" -> cy = drawDigitalClock(canvas, cx, cy)
                "analog" -> cy = drawAnalogClock(canvas, cx, cy)
                "modern_stacked" -> cy = drawStackedClock(canvas, cx, cy)
                "modern_glow" -> cy = drawGlowClock(canvas, cx, cy)
                else -> cy = drawDigitalClock(canvas, cx, cy)
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
        val r = Color.red(activeTextColor)
        val g = Color.green(activeTextColor)
        val b = Color.blue(activeTextColor)
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
        val r = Color.red(activeTextColor)
        val g = Color.green(activeTextColor)
        val b = Color.blue(activeTextColor)
        timePaint.color = Color.argb(alpha, r, g, b)
        timePaint.setShadowLayer(15f, 0f, 0f, Color.argb(alpha, r, g, b))
        
        val textY = cy - (timePaint.descent() + timePaint.ascent()) / 2f
        canvas.drawText(text, cx, textY, timePaint)
        timePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        return textY + timePaint.descent() + 8.dp
    }

    private fun drawDigitalClock(canvas: Canvas, cx: Float, cy: Float): Float {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val text = format.format(Date())
        timePaint.textSize = 72.sp
        timePaint.textAlign = Paint.Align.CENTER
        
        val alpha = (255 * brightness).toInt()
        val r = Color.red(activeTextColor)
        val g = Color.green(activeTextColor)
        val b = Color.blue(activeTextColor)
        timePaint.color = Color.argb(alpha, r, g, b)
        timePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        
        val textY = cy - (timePaint.descent() + timePaint.ascent()) / 2f
        canvas.drawText(text, cx, textY, timePaint)
        return textY + timePaint.descent() + 8.dp
    }

    private fun drawAnalogClock(canvas: Canvas, cx: Float, cy: Float): Float {
        val radius = 80.dp
        
        val r = Color.red(activeTextColor)
        val g = Color.green(activeTextColor)
        val b = Color.blue(activeTextColor)
        
        val baseAlpha = (255 * brightness).toInt()
        val lowAlpha = (50 * brightness).toInt()
        val medAlpha = (180 * brightness).toInt()
        
        circlePaint.color = Color.argb(lowAlpha, r, g, b)
        circlePaint.strokeWidth = 2.dp
        canvas.drawCircle(cx, cy, radius, circlePaint)

        val cal = Calendar.getInstance()
        val hours = cal.get(Calendar.HOUR)
        val minutes = cal.get(Calendar.MINUTE)

        val hourAngle = Math.toRadians((hours * 30 + minutes * 0.5 - 90).toDouble())
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
        
        val r = Color.red(activeTextColor)
        val g = Color.green(activeTextColor)
        val b = Color.blue(activeTextColor)
        val medAlpha = (180 * brightness).toInt()
        datePaint.color = Color.argb(medAlpha, r, g, b)
        
        canvas.drawText(text, cx, cy, datePaint)
        return cy + datePaint.descent() - datePaint.ascent()
    }

    private fun drawStatusRow(canvas: Canvas, cx: Float, cy: Float) {
        subPaint.textSize = 14.sp
        subPaint.textAlign = Paint.Align.LEFT

        val r = Color.red(activeTextColor)
        val g = Color.green(activeTextColor)
        val b = Color.blue(activeTextColor)
        val medAlpha = (180 * brightness).toInt()
        val chargeAlpha = (200 * brightness).toInt()
        
        val items = mutableListOf<String>()
        if (showNotifications && notificationCount > 0) {
            items.add("🔔 $notificationCount")
        }

        var totalWidth = 0f
        if (showBattery) {
            totalWidth += 24.dp
            totalWidth += subPaint.measureText(" $batteryLevel%")
            if (items.isNotEmpty()) totalWidth += 16.dp
        }
        
        if (items.isNotEmpty()) {
            val notifText = items.joinToString("   ")
            totalWidth += subPaint.measureText(notifText)
        }

        var currentX = cx - totalWidth / 2f
        val startY = cy + 4.dp

        if (showBattery) {
            val batPaint = Paint(subPaint)
            batPaint.color = if (batteryIsCharging) Color.argb(chargeAlpha, 100, 255, 100) else Color.argb(medAlpha, r, g, b)
            
            batPaint.style = Paint.Style.STROKE
            batPaint.strokeWidth = 1.5f.dp
            val bodyWidth = 18.dp
            val bodyHeight = 10.dp
            val iconY = startY - bodyHeight
            canvas.drawRoundRect(currentX, iconY, currentX + bodyWidth, iconY + bodyHeight, 2.dp, 2.dp, batPaint)
            
            batPaint.style = Paint.Style.FILL
            canvas.drawRect(currentX + bodyWidth, iconY + 3.dp, currentX + bodyWidth + 2.dp, iconY + 7.dp, batPaint)
            
            val levelWidth = (bodyWidth - 4.dp) * (batteryLevel / 100f)
            canvas.drawRoundRect(currentX + 2.dp, iconY + 2.dp, currentX + 2.dp + levelWidth, iconY + bodyHeight - 2.dp, 1.dp, 1.dp, batPaint)
            
            if (batteryIsCharging) {
                val path = android.graphics.Path()
                val cxBolt = currentX + bodyWidth / 2f
                val cyBolt = iconY + bodyHeight / 2f
                
                val boltW = 5.dp
                val boltH = 10.dp
                
                path.moveTo(cxBolt + 2.dp, cyBolt - boltH/2)
                path.lineTo(cxBolt - boltW/2, cyBolt + 1.dp)
                path.lineTo(cxBolt, cyBolt + 1.dp)
                path.lineTo(cxBolt - 2.dp, cyBolt + boltH/2)
                path.lineTo(cxBolt + boltW/2, cyBolt - 1.dp)
                path.lineTo(cxBolt, cyBolt - 1.dp)
                path.close()
                
                val boltPaint = Paint(batPaint)
                boltPaint.style = Paint.Style.FILL
                boltPaint.color = Color.WHITE
                boltPaint.setShadowLayer(2f, 0f, 0f, Color.argb(100, 0, 0, 0))
                
                canvas.save()
                canvas.rotate(90f, cxBolt, cyBolt)
                canvas.drawPath(path, boltPaint)
                canvas.restore()
            }
            
            currentX += bodyWidth + 6.dp
            
            subPaint.color = batPaint.color
            canvas.drawText("$batteryLevel%", currentX, startY, subPaint)
            currentX += subPaint.measureText("$batteryLevel%") + 16.dp
        }
        
        if (items.isNotEmpty()) {
            subPaint.color = Color.argb(medAlpha, r, g, b)
            val notifText = items.joinToString("   ")
            canvas.drawText(notifText, currentX, startY, subPaint)
        }
    }

    private val Float.dp: Float get() = this * resources.displayMetrics.density
    private val Int.dp: Float get() = this * resources.displayMetrics.density
    private val Float.sp: Float get() = this * resources.displayMetrics.scaledDensity
    private val Int.sp: Float get() = this * resources.displayMetrics.scaledDensity

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopTicking()
    }
}
