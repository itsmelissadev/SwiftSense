package io.github.itsmelissadev.swiftsense.feature.downloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.github.itsmelissadev.swiftsense.MainActivity
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.feature.downloader.engine.DownloadStatus
import io.github.itsmelissadev.swiftsense.feature.downloader.engine.TurboDownloadEngine
import io.github.itsmelissadev.swiftsense.utils.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TurboDownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "turbo_downloader_channel"
        const val NOTIFICATION_ID = 9182

        private const val ACTION_PAUSE = "io.github.itsmelissadev.swiftsense.ACTION_DOWNLOAD_PAUSE"
        private const val ACTION_RESUME = "io.github.itsmelissadev.swiftsense.ACTION_DOWNLOAD_RESUME"
        private const val ACTION_CANCEL = "io.github.itsmelissadev.swiftsense.ACTION_DOWNLOAD_CANCEL"
        private const val EXTRA_TASK_ID = "extra_task_id"

        fun startService(context: Context) {
            val intent = Intent(context, TurboDownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, TurboDownloadService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildInitialNotification())

        serviceScope.launch {
            TurboDownloadEngine.tasks.collectLatest { tasks ->
                val activeTasks = tasks.filter {
                    it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.CONNECTING
                }

                if (activeTasks.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    val firstTask = activeTasks.first()
                    val totalSpeed = activeTasks.sumOf { it.speedBytesPerSec }
                    val speedText = FormatUtils.formatSpeed(totalSpeed)
                    val progressPercent = (firstTask.progress * 100).toInt()
                    val statusText = when (firstTask.status) {
                        DownloadStatus.CONNECTING -> getString(R.string.downloader_status_connecting)
                        DownloadStatus.DOWNLOADING -> getString(R.string.downloader_status_downloading)
                        DownloadStatus.PAUSED -> getString(R.string.downloader_status_paused)
                        DownloadStatus.COMPLETED -> getString(R.string.downloader_status_completed)
                        DownloadStatus.FAILED -> getString(R.string.downloader_status_failed)
                        DownloadStatus.QUEUED -> getString(R.string.downloader_status_connecting)
                    }

                    val isIndeterminate = firstTask.status == DownloadStatus.CONNECTING || firstTask.fileSize <= 0
                    val downloadedStr = FormatUtils.formatBytes(firstTask.downloadedBytes)
                    val totalStr = if (firstTask.fileSize > 0) FormatUtils.formatBytes(firstTask.fileSize) else ""
                    val contentDetail = if (totalStr.isNotEmpty()) "$statusText • $downloadedStr / $totalStr" else "$statusText • $downloadedStr"

                    val pauseResumeAction = if (firstTask.status == DownloadStatus.DOWNLOADING) {
                        NotificationCompat.Action.Builder(
                            android.R.drawable.ic_media_pause,
                            getString(R.string.downloader_action_pause),
                            createServiceActionPendingIntent(ACTION_PAUSE, firstTask.id, 1)
                        ).build()
                    } else {
                        NotificationCompat.Action.Builder(
                            android.R.drawable.ic_media_play,
                            getString(R.string.downloader_action_resume),
                            createServiceActionPendingIntent(ACTION_RESUME, firstTask.id, 1)
                        ).build()
                    }

                    val cancelAction = NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.downloader_action_cancel),
                        createServiceActionPendingIntent(ACTION_CANCEL, firstTask.id, 2)
                    ).build()

                    val notification = NotificationCompat.Builder(this@TurboDownloadService, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle(firstTask.fileName)
                        .setContentText(contentDetail)
                        .setSubText(speedText)
                        .setProgress(100, if (isIndeterminate) 0 else progressPercent, isIndeterminate)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setShowWhen(false)
                        .setContentIntent(createPendingIntent())
                        .addAction(pauseResumeAction)
                        .addAction(cancelAction)
                        .build()

                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val taskId = intent?.getStringExtra(EXTRA_TASK_ID)
        if (taskId != null) {
            when (action) {
                ACTION_PAUSE -> TurboDownloadEngine.pauseDownload(taskId)
                ACTION_RESUME -> TurboDownloadEngine.resumeDownload(this, taskId)
                ACTION_CANCEL -> TurboDownloadEngine.cancelDownload(taskId)
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.downloader_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.downloader_notification_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildInitialNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.feature_turbo_downloader))
            .setContentText(getString(R.string.downloader_status_connecting))
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(createPendingIntent())
            .build()
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createServiceActionPendingIntent(action: String, taskId: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, TurboDownloadService::class.java).apply {
            this.action = action
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
