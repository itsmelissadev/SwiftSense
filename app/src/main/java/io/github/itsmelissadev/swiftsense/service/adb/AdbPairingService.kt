package io.github.itsmelissadev.swiftsense.service.adb

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import io.github.itsmelissadev.swiftsense.MainActivity
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.R)
class AdbPairingService : Service() {

    companion object {
        const val CHANNEL_ID = "adb_pairing_channel"
        const val NOTIFICATION_ID = 4001

        const val ACTION_START_DISCOVERY = "io.github.itsmelissadev.swiftsense.action.START_ADB_PAIRING"
        const val ACTION_STOP_DISCOVERY = "io.github.itsmelissadev.swiftsense.action.STOP_ADB_PAIRING"
        const val ACTION_PAIR_SUBMIT = "io.github.itsmelissadev.swiftsense.action.SUBMIT_PAIRING_CODE"

        const val EXTRA_PAIR_HOST = "extra_pair_host"
        const val EXTRA_PAIR_PORT = "extra_pair_port"
        const val KEY_PAIRING_CODE = "key_pairing_code"

        fun start(context: Context) {
            val intent = Intent(context, AdbPairingService::class.java).apply {
                action = ACTION_START_DISCOVERY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdbPairingService::class.java).apply {
                action = ACTION_STOP_DISCOVERY
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    private lateinit var preferenceManager: PreferenceManager

    private var pairingMdns: AdbMdns? = null
    private var connectMdns: AdbMdns? = null

    private var discoveredPairHost: String = "127.0.0.1"
    private var discoveredPairPort: Int = 0
    private var discoveredConnectPort: Int = 0

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        preferenceManager = PreferenceManager(this)
        AdbManager.init(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_DISCOVERY

        when (action) {
            ACTION_START_DISCOVERY -> {
                startForegroundWithNotification(buildSearchingNotification())
                startMdnsDiscovery()
            }
            ACTION_STOP_DISCOVERY -> {
                stopSelf()
            }
            ACTION_PAIR_SUBMIT -> {
                if (intent != null) {
                    val remoteInput = RemoteInput.getResultsFromIntent(intent)
                    val code = remoteInput?.getCharSequence(KEY_PAIRING_CODE)?.toString()?.trim()
                    val host = intent.getStringExtra(EXTRA_PAIR_HOST) ?: discoveredPairHost
                    val port = intent.getIntExtra(EXTRA_PAIR_PORT, discoveredPairPort)

                    if (!code.isNullOrBlank() && port > 0) {
                        handlePairAndConnect(host, port, code)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startMdnsDiscovery() {
        pairingMdns?.stop()
        connectMdns?.stop()

        pairingMdns = AdbMdns(this, AdbMdns.TLS_PAIRING) { host, port ->
            discoveredPairHost = host
            discoveredPairPort = port
            notificationManager.notify(NOTIFICATION_ID, buildDiscoveredNotification(host, port))
        }

        connectMdns = AdbMdns(this, AdbMdns.TLS_CONNECT) { _, port ->
            discoveredConnectPort = port
        }

        pairingMdns?.start()
        connectMdns?.start()
    }

    private fun handlePairAndConnect(host: String, pairPort: Int, code: String) {
        notificationManager.notify(NOTIFICATION_ID, buildPairingInProgressNotification())

        serviceScope.launch {
            val pairRes = AdbManager.pair(host, pairPort, code)
            if (pairRes.isSuccess) {
                preferenceManager.setHasPairedAdb(true)
                preferenceManager.setAdbPairingPort(pairPort)
                preferenceManager.setAdbHost(host)

                notificationManager.notify(NOTIFICATION_ID, buildPairedConnectingNotification())

                var connectPort = discoveredConnectPort
                if (connectPort <= 0) {
                    delay(1200)
                    connectPort = discoveredConnectPort
                }

                if (connectPort > 0) {
                    val connRes = AdbManager.connect(host, connectPort)
                    if (connRes.isSuccess) {
                        preferenceManager.setAdbPort(connectPort)
                        preferenceManager.setShellMode("adb")
                        SwiftSenseDaemonBridge.spawnDaemonViaAdb(this@AdbPairingService)
                        io.github.itsmelissadev.swiftsense.service.shell.ShellBridge.refreshNow()
                        notificationManager.notify(NOTIFICATION_ID, buildConnectedNotification(connectPort))
                        delay(2500)
                        stopSelf()
                        return@launch
                    }
                }

                notificationManager.notify(NOTIFICATION_ID, buildPairedOnlyNotification(host))
                delay(3000)
                stopSelf()
            } else {
                val errorMsg = pairRes.exceptionOrNull()?.message ?: getString(R.string.adb_notif_failed_title)
                notificationManager.notify(NOTIFICATION_ID, buildFailedNotification(errorMsg))
                delay(3000)
                stopSelf()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.adb_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.adb_notification_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildSearchingNotification(): android.app.Notification {
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AdbPairingService::class.java).apply { action = ACTION_STOP_DISCOVERY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt_24px)
            .setContentTitle(getString(R.string.adb_notif_searching_title))
            .setContentText(getString(R.string.adb_notif_searching_text))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.adb_notif_action_cancel), stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun buildDiscoveredNotification(host: String, port: Int): android.app.Notification {
        val remoteInput = RemoteInput.Builder(KEY_PAIRING_CODE)
            .setLabel(getString(R.string.adb_notif_input_code_label))
            .build()

        val submitIntent = Intent(this, AdbPairingService::class.java).apply {
            action = ACTION_PAIR_SUBMIT
            putExtra(EXTRA_PAIR_HOST, host)
            putExtra(EXTRA_PAIR_PORT, port)
        }

        val submitPendingIntent = PendingIntent.getService(
            this,
            2,
            submitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_bolt_24px,
            getString(R.string.adb_notif_action_submit),
            submitPendingIntent
        ).addRemoteInput(remoteInput).build()

        val stopPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, AdbPairingService::class.java).apply { action = ACTION_STOP_DISCOVERY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt_24px)
            .setContentTitle(getString(R.string.adb_notif_found_title))
            .setContentText(getString(R.string.adb_notif_found_text))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(replyAction)
            .addAction(0, getString(R.string.adb_notif_action_cancel), stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()
    }

    private fun buildPairingInProgressNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt_24px)
            .setContentTitle(getString(R.string.adb_notif_pairing_title))
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun buildPairedConnectingNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt_24px)
            .setContentTitle(getString(R.string.adb_notif_paired_title))
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun buildConnectedNotification(port: Int): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt_24px)
            .setContentTitle(getString(R.string.adb_notif_connected_title))
            .setContentText(getString(R.string.adb_notif_connected_text))
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun buildPairedOnlyNotification(host: String): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt_24px)
            .setContentTitle(getString(R.string.adb_status_paired))
            .setContentText(getString(R.string.adb_status_connected))
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun buildFailedNotification(errorMsg: String): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt_24px)
            .setContentTitle(getString(R.string.adb_notif_failed_title))
            .setContentText(errorMsg)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        pairingMdns?.stop()
        connectMdns?.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
