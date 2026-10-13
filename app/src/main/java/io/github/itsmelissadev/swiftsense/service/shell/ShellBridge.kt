package io.github.itsmelissadev.swiftsense.service.shell

import android.content.Context
import android.content.pm.PackageManager
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.adb.AdbManager
import io.github.itsmelissadev.swiftsense.service.adb.SwiftSenseDaemonBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

sealed interface BackendStatus {
    data object Ready : BackendStatus
    data object ShizukuNotInstalled : BackendStatus
    data object ShizukuNotRunning : BackendStatus
    data object ShizukuUnauthorized : BackendStatus
    data object SwiftSenseStopped : BackendStatus
}

object ShellBridge {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var appContext: Context? = null

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _status = MutableStateFlow<BackendStatus>(BackendStatus.SwiftSenseStopped)
    val status: StateFlow<BackendStatus> = _status.asStateFlow()

    @Volatile
    private var isStarted = false

    fun init(context: Context) {
        appContext = context.applicationContext
        startMonitoring()
        tryAutoStartSwiftSense()
    }

    private fun startMonitoring() {
        if (isStarted) return
        isStarted = true
        scope.launch {
            while (true) {
                checkAndUpdate()
                delay(1500)
            }
        }
    }

    fun refreshNow() {
        scope.launch {
            checkAndUpdate()
        }
    }

    private fun checkAndUpdate() {
        val context = appContext ?: return
        val sp = context.getSharedPreferences("swiftsense_shell_prefs", Context.MODE_PRIVATE)
        val mode = sp.getString("shell_mode", "shizuku") ?: "shizuku"

        if (mode == "adb") {
            val isAlive = SwiftSenseDaemonBridge.isDaemonAlive()
            _ready.value = isAlive
            _status.value = if (isAlive) BackendStatus.Ready else BackendStatus.SwiftSenseStopped
        } else {
            val isInstalled = try {
                context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
            if (!isInstalled) {
                _ready.value = false
                _status.value = BackendStatus.ShizukuNotInstalled
                return
            }

            val isRunning = Shizuku.pingBinder()
            if (!isRunning) {
                _ready.value = false
                _status.value = BackendStatus.ShizukuNotRunning
                return
            }

            val isAuthorized = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            if (!isAuthorized) {
                _ready.value = false
                _status.value = BackendStatus.ShizukuUnauthorized
                return
            }

            _ready.value = true
            _status.value = BackendStatus.Ready
        }
    }

    private fun tryAutoStartSwiftSense() {
        val context = appContext ?: return
        scope.launch {
            val pref = PreferenceManager(context)
            if (!pref.getSwiftSenseAutoStartSync()) {
                return@launch
            }
            val sp = context.getSharedPreferences("swiftsense_shell_prefs", Context.MODE_PRIVATE)
            val mode = sp.getString("shell_mode", "shizuku") ?: "shizuku"
            if (mode == "adb" && !SwiftSenseDaemonBridge.isDaemonAlive()) {
                val port = pref.getAdbPortSync().takeIf { it > 0 } ?: AdbManager.lastPort
                val host = pref.getAdbHostSync()
                if (port > 0) {
                    val res = kotlinx.coroutines.withTimeoutOrNull(2500L) {
                        AdbManager.connect(host, port)
                    }
                    if (res != null && res.isSuccess) {
                        SwiftSenseDaemonBridge.spawnDaemonViaAdb(context)
                        delay(600)
                        checkAndUpdate()
                    }
                }
            }
        }
    }

    private fun getBackend(): ShellBackend {
        val context = appContext
        val mode = if (context != null) {
            val sp = context.getSharedPreferences("swiftsense_shell_prefs", Context.MODE_PRIVATE)
            sp.getString("shell_mode", "shizuku") ?: "shizuku"
        } else "shizuku"

        return if (mode == "adb") SwiftSenseBackend else ShizukuBackend
    }

    fun isReady(): Boolean = _ready.value

    fun run(command: String, timeoutMs: Int = 15000): Result<String> {
        val backend = getBackend()
        return if (backend is SwiftSenseBackend) {
            backend.run(command, timeoutMs)
        } else {
            backend.run(command)
        }
    }

    suspend fun runAsync(command: String, timeoutMs: Int = 15000): Result<String> = withContext(Dispatchers.IO) {
        run(command, timeoutMs)
    }
}
