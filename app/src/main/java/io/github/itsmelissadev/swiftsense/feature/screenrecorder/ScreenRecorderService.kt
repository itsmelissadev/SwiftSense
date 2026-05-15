package io.github.itsmelissadev.swiftsense.feature.screenrecorder

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import io.github.itsmelissadev.swiftsense.MainActivity
import io.github.itsmelissadev.swiftsense.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class ScreenRecorderService : Service() {

    private val notificationId = 8472
    private val channelId = "screen_recorder_channel"
    private val TAG = "ScreenRecorderService"

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var videoTrackIndex = -1
    @Volatile
    private var isMuxerStarted = false
    private var encoderInputSurface: Surface? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var outputPfd: ParcelFileDescriptor? = null
    private var outputUri: Uri? = null

    enum class RecorderState {
        IDLE,
        RECORDING,
        PAUSED
    }
    @Volatile
    private var currentState = RecorderState.IDLE

    private val durationSeconds = AtomicInteger(0)
    private var durationJob: Job? = null
    private var recorderJob: Job? = null

    // Configuration parameters
    private var configResultCode = 0
    private var configData: Intent? = null
    private var configWidth = 1080
    private var configHeight = 1920
    private var configDpi = DisplayMetrics.DENSITY_DEFAULT
    private var configFps = 60
    private var configBitrate = 15

    // Dedicated single-thread dispatcher for encoder operations — keeps encoding off the shared
    // pool
    private val recorderDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val serviceJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.w(TAG, "MediaProjection stopped by system")
            stopService()
        }
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_RECORD = "ACTION_RECORD"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE_RESUME = "ACTION_PAUSE_RESUME"

        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        const val EXTRA_WIDTH = "EXTRA_WIDTH"
        const val EXTRA_HEIGHT = "EXTRA_HEIGHT"
        const val EXTRA_FPS = "EXTRA_FPS"
        const val EXTRA_BITRATE = "EXTRA_BITRATE"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        configDpi = getDeviceDpi()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // ALWAYS call startForeground FIRST to prevent SecurityException in
                // getMediaProjection
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                            notificationId,
                            buildNotification(),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                } else {
                    startForeground(notificationId, buildNotification())
                }

                if (currentState == RecorderState.IDLE) {
                    configResultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                    @Suppress("DEPRECATION") configData = intent.getParcelableExtra(EXTRA_DATA)
                    configWidth = intent.getIntExtra(EXTRA_WIDTH, 1080)
                    configHeight = intent.getIntExtra(EXTRA_HEIGHT, 1920)
                    configFps = intent.getIntExtra(EXTRA_FPS, 60)
                    configBitrate = intent.getIntExtra(EXTRA_BITRATE, 15)

                    if (configResultCode == Activity.RESULT_OK && configData != null) {
                        try {
                            val mpManager =
                                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as
                                            MediaProjectionManager
                            mediaProjection =
                                    mpManager.getMediaProjection(configResultCode, configData!!)
                            mediaProjection?.registerCallback(projectionCallback, null)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error initializing MediaProjection", e)
                        }
                    }
                }

                if (currentState == RecorderState.IDLE && mediaProjection == null) {
                    stopService()
                }
            }
            ACTION_RECORD -> {
                if (currentState == RecorderState.IDLE) {
                    startRecording()
                }
            }
            ACTION_STOP -> stopService()
            ACTION_PAUSE_RESUME -> togglePauseResume()
        }
        return START_NOT_STICKY
    }

    private fun getDeviceDpi(): Int {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = wm.currentWindowMetrics
            val density = resources.configuration.densityDpi
            if (density > 0) density else DisplayMetrics.DENSITY_DEFAULT
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getMetrics(dm)
            dm.densityDpi
        }
    }

    private fun startRecording() {
        if (mediaProjection == null) {
            stopService()
            return
        }

        // Acquire partial wake lock to prevent CPU sleep during recording
        acquireWakeLock()

        currentState = RecorderState.RECORDING
        durationSeconds.set(0)
        updateNotification()

        recorderJob =
                coroutineScope.launch(recorderDispatcher) {
                    try {
                        prepareRecorder(configWidth, configHeight, configFps, configBitrate)

                        virtualDisplay =
                                mediaProjection?.createVirtualDisplay(
                                        "ScreenRecorder",
                                        configWidth,
                                        configHeight,
                                        configDpi,
                                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                        encoderInputSurface,
                                        null,
                                        null
                                )

                        mediaCodec?.start()

                        durationJob =
                                launch(Dispatchers.IO) {
                                    while (isActive && currentState != RecorderState.IDLE) {
                                        delay(1000)
                                        if (currentState == RecorderState.RECORDING) {
                                            durationSeconds.incrementAndGet()
                                            updateNotification()
                                        }
                                    }
                                }

                        drainEncoder()
                    } catch (e: Exception) {
                        Log.e(TAG, "Recording failed", e)
                        stopService()
                    }
                }
    }

    /**
     * Selects the best hardware encoder and MIME type in a single codec list scan.
     * Prefers HEVC (H.265) hardware encoder if available, otherwise falls back to H.264.
     */
    private fun selectBestCodec(): Pair<String, MediaCodec> {
        val codecList = android.media.MediaCodecList(android.media.MediaCodecList.ALL_CODECS)
        val codecInfos = codecList.codecInfos

        var hevcEncoder: android.media.MediaCodecInfo? = null
        var avcEncoder: android.media.MediaCodecInfo? = null

        for (info in codecInfos) {
            if (!info.isEncoder) continue
            if (info.name.contains("sw", ignoreCase = true) ||
                            info.name.contains("google", ignoreCase = true) ||
                            info.name.contains("OMX.google", ignoreCase = true)
            )
                    continue

            val types = info.supportedTypes
            for (type in types) {
                if (hevcEncoder == null &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                                type.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true)
                ) {
                    hevcEncoder = info
                }
                if (avcEncoder == null &&
                                type.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true)
                ) {
                    avcEncoder = info
                }
            }
            if (hevcEncoder != null && avcEncoder != null) break
        }

        return when {
            hevcEncoder != null -> {
                Log.d(TAG, "Using HEVC (H.265) HW encoder: ${hevcEncoder.name}")
                Pair(MediaFormat.MIMETYPE_VIDEO_HEVC, MediaCodec.createByCodecName(hevcEncoder.name))
            }
            avcEncoder != null -> {
                Log.d(TAG, "Using H.264 HW encoder: ${avcEncoder.name}")
                Pair(MediaFormat.MIMETYPE_VIDEO_AVC, MediaCodec.createByCodecName(avcEncoder.name))
            }
            else -> {
                Log.w(TAG, "No dedicated HW encoder found, using default H.264")
                Pair(MediaFormat.MIMETYPE_VIDEO_AVC, MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC))
            }
        }
    }

    private fun prepareRecorder(width: Int, height: Int, fps: Int, bitrate: Int) {
        val (mimeType, encoder) = selectBestCodec()

        val format =
                MediaFormat.createVideoFormat(mimeType, width, height).apply {
                    setInteger(
                            MediaFormat.KEY_COLOR_FORMAT,
                            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                    )
                    setInteger(MediaFormat.KEY_BIT_RATE, bitrate * 1000 * 1000)
                    setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
                    setInteger(
                            MediaFormat.KEY_BITRATE_MODE,
                            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
                    )

                    // Repeat previous frame to keep stream alive if game drops frames
                    setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000L / fps)

                    // Optimize for low latency and gaming
                    setInteger(MediaFormat.KEY_PRIORITY, 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        setInteger(MediaFormat.KEY_LATENCY, 0)
                    }

                    // Disable B-frames for faster encoding (lower latency)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            setInteger("max-bframes", 0)
                        } catch (_: Exception) {
                            /* Not supported on this codec */
                        }
                    }

                    // Baseline profile for H.264 (no B-frames, widest compatibility)
                    if (mimeType == MediaFormat.MIMETYPE_VIDEO_AVC) {
                        setInteger(
                                MediaFormat.KEY_PROFILE,
                                MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
                        )
                        setInteger(
                                MediaFormat.KEY_LEVEL,
                                MediaCodecInfo.CodecProfileLevel.AVCLevel41
                        )
                    }
                }

        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoderInputSurface = encoder.createInputSurface()
        mediaCodec = encoder

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "SwiftSense_$timeStamp.mp4"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SwiftSense")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                outputUri = uri
                outputPfd = contentResolver.openFileDescriptor(uri, "rw")
                mediaMuxer = MediaMuxer(outputPfd!!.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            } else {
                throw IllegalStateException("Failed to create MediaStore entry for recording")
            }
        } else {
            @Suppress("DEPRECATION")
            val movieDir = File(Environment.getExternalStorageDirectory(), "Movies/SwiftSense")
            if (!movieDir.exists()) movieDir.mkdirs()
            val outputFile = File(movieDir, fileName)
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }

        isMuxerStarted = false
        videoTrackIndex = -1
    }

    private suspend fun drainEncoder() {
        val bufferInfo = MediaCodec.BufferInfo()
        while (currentState != RecorderState.IDLE) {
            if (currentState == RecorderState.PAUSED) {
                delay(100)
                continue
            }

            val codec = mediaCodec ?: break
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val muxer = mediaMuxer
                if (muxer != null && !isMuxerStarted) {
                    val newFormat = codec.outputFormat
                    videoTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    isMuxerStarted = true
                }
            } else if (outputBufferIndex >= 0) {
                val encodedData = codec.getOutputBuffer(outputBufferIndex)
                if (encodedData != null) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0 && isMuxerStarted && videoTrackIndex != -1) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer?.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                }
                codec.releaseOutputBuffer(outputBufferIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock =
                pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SwiftSense::ScreenRecorder").apply {
                    acquire(4 * 60 * 60 * 1000L) // Max 4 hours
                }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {}
        wakeLock = null
    }

    private fun stopService() {
        if (currentState == RecorderState.IDLE) return
        currentState = RecorderState.IDLE

        coroutineScope.launch(recorderDispatcher) {
            durationJob?.cancel()
            recorderJob?.cancel()

            try {
                mediaCodec?.stop()
                mediaCodec?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping codec", e)
            }

            try {
                if (isMuxerStarted) {
                    mediaMuxer?.stop()
                }
                mediaMuxer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping muxer", e)
            }

            try {
                encoderInputSurface?.release()
            } catch (_: Exception) {}

            try {
                virtualDisplay?.release()
            } catch (_: Exception) {}

            try {
                mediaProjection?.unregisterCallback(projectionCallback)
                mediaProjection?.stop()
            } catch (_: Exception) {}

            // Finalize MediaStore entry on Android 10+
            finalizeMediaStoreEntry()

            try {
                outputPfd?.close()
            } catch (_: Exception) {}

            mediaCodec = null
            mediaMuxer = null
            encoderInputSurface = null
            virtualDisplay = null
            mediaProjection = null
            outputPfd = null
            outputUri = null
            isMuxerStarted = false

            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun finalizeMediaStoreEntry() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = outputUri ?: return
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, contentValues, null, null)
                Log.d(TAG, "MediaStore entry finalized: $uri")
            } catch (e: Exception) {
                Log.e(TAG, "Error finalizing MediaStore entry", e)
            }
        }
    }

    private fun togglePauseResume() {
        if (currentState == RecorderState.RECORDING) {
            currentState = RecorderState.PAUSED
        } else if (currentState == RecorderState.PAUSED) {
            currentState = RecorderState.RECORDING
        }
        updateNotification()
    }

    private fun buildNotification(): Notification {
        val stopIntent =
                Intent(this, ScreenRecorderService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent =
                PendingIntent.getService(
                        this,
                        1,
                        stopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val builder =
                NotificationCompat.Builder(this, channelId)
                        .setContentIntent(mainPendingIntent)
                        .setSmallIcon(R.drawable.videocam_24px)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)

        when (currentState) {
            RecorderState.IDLE -> {
                val recordIntent =
                        Intent(this, ScreenRecorderService::class.java).apply {
                            action = ACTION_RECORD
                        }
                val recordPendingIntent =
                        PendingIntent.getService(
                                this,
                                3,
                                recordIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                builder.setContentTitle(getString(R.string.screen_recorder_ready))
                builder.setContentText(getString(R.string.feature_screen_recorder))
                builder.addAction(
                        0,
                        getString(R.string.screen_recorder_action_start),
                        recordPendingIntent
                )
                builder.addAction(0, getString(R.string.screen_recorder_stop), stopPendingIntent)
            }
            RecorderState.RECORDING, RecorderState.PAUSED -> {
                val pauseResumeIntent =
                        Intent(this, ScreenRecorderService::class.java).apply {
                            action = ACTION_PAUSE_RESUME
                        }
                val pauseResumePendingIntent =
                        PendingIntent.getService(
                                this,
                                2,
                                pauseResumeIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                val statusText =
                        if (currentState == RecorderState.PAUSED)
                                getString(R.string.screen_recorder_paused)
                        else getString(R.string.screen_recorder_recording)
                val seconds = durationSeconds.get()
                val timeString =
                        String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                seconds / 60,
                                seconds % 60
                        )
                val pauseResumeLabel =
                        if (currentState == RecorderState.PAUSED)
                                getString(R.string.screen_recorder_action_resume)
                        else getString(R.string.screen_recorder_action_pause)

                builder.setContentTitle("$statusText ($timeString)")
                builder.setContentText(getString(R.string.feature_screen_recorder))
                builder.addAction(0, pauseResumeLabel, pauseResumePendingIntent)
                builder.addAction(0, getString(R.string.screen_recorder_stop), stopPendingIntent)
            }
        }

        return builder.build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, buildNotification())
    }

    private fun createNotificationChannel() {
        val channel =
                NotificationChannel(
                        channelId,
                        getString(R.string.screen_recorder_notification_channel),
                        NotificationManager.IMPORTANCE_LOW
                )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        // Stop recording and release resources BEFORE closing the dispatcher
        if (currentState != RecorderState.IDLE) {
            currentState = RecorderState.IDLE
            // Block briefly to ensure cleanup completes on the encoder thread
            runBlocking(recorderDispatcher) {
                durationJob?.cancel()
                recorderJob?.cancel()
                releaseRecorderResources()
            }
        }
        serviceJob.cancel()
        recorderDispatcher.close()
    }

    private fun releaseRecorderResources() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
        } catch (_: Exception) {}

        try {
            if (isMuxerStarted) mediaMuxer?.stop()
            mediaMuxer?.release()
        } catch (_: Exception) {}

        try { encoderInputSurface?.release() } catch (_: Exception) {}
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try {
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
        } catch (_: Exception) {}

        finalizeMediaStoreEntry()
        try { outputPfd?.close() } catch (_: Exception) {}

        mediaCodec = null
        mediaMuxer = null
        encoderInputSurface = null
        virtualDisplay = null
        mediaProjection = null
        outputPfd = null
        outputUri = null
        isMuxerStarted = false

        releaseWakeLock()
    }
}
