package io.github.itsmelissadev.swiftsense.service.adb

import android.content.Context
import android.os.Build
import io.github.rhythmcache.adb.AdbClient
import io.github.rhythmcache.adb.FileKeyProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

object AdbManager {
    private const val CONNECT_AUTO_WINDOW_MS = 45_000L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()

    private var appContext: Context? = null
    private var keyProvider: FileKeyProvider? = null
    private var adbClient: AdbClient? = null

    var lastHost: String = "127.0.0.1"
    var lastPort: Int = 0

    private val _connectionState = MutableStateFlow<AdbConnectionState>(AdbConnectionState.Disconnected)
    val connectionState: StateFlow<AdbConnectionState> = _connectionState.asStateFlow()

    fun init(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        val keyFile = File(app.filesDir, "adbkey")
        keyProvider = FileKeyProvider(
            keyFile = keyFile,
            identityComment = "SwiftSense"
        )
    }

    fun isConnected(): Boolean {
        return adbClient != null && _connectionState.value is AdbConnectionState.Connected
    }

    suspend fun pair(host: String, port: Int, pairingCode: String): Result<Unit> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return Result.failure(IllegalStateException("Manual ADB requires Android 11+"))
        }
        val kp = keyProvider ?: return Result.failure(IllegalStateException("AdbManager not initialized"))
        _connectionState.value = AdbConnectionState.Pairing
        return try {
            val result = AdbClient.pairTls(
                host = host,
                port = port,
                pairingCode = pairingCode,
                keyProvider = kp
            )
            if (result.isSuccess) {
                _connectionState.value = AdbConnectionState.Paired
                Result.success(Unit)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Pairing failed")
                _connectionState.value = AdbConnectionState.Error(error.message ?: "Pairing failed")
                Result.failure(error)
            }
        } catch (e: Exception) {
            _connectionState.value = AdbConnectionState.Error(e.message ?: "Pairing failed")
            Result.failure(e)
        }
    }

    suspend fun connect(host: String, port: Int): Result<Unit> = mutex.withLock {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return Result.failure(IllegalStateException("Manual ADB requires Android 11+"))
        }
        val kp = keyProvider ?: return Result.failure(IllegalStateException("AdbManager not initialized"))
        disconnectInternal()
        _connectionState.value = AdbConnectionState.Connecting
        return try {
            val client = AdbClient.connectTls(
                host = host,
                port = port,
                keyProvider = kp
            )
            adbClient = client
            lastHost = host
            lastPort = port
            _connectionState.value = AdbConnectionState.Connected(port)
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = AdbConnectionState.Error(e.message ?: "Connection failed")
            Result.failure(e)
        }
    }

    suspend fun connectAuto(context: Context, host: String, savedPort: Int): Result<Int> {
        var discoveredPort = 0
        val mdns = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AdbMdns(context.applicationContext, AdbMdns.TLS_CONNECT) { _, port ->
                discoveredPort = port
            }.also { it.start() }
        } else null
        var lastError: Throwable? = null
        try {
            val deadline = System.currentTimeMillis() + CONNECT_AUTO_WINDOW_MS
            while (System.currentTimeMillis() < deadline) {
                val candidates = listOf(discoveredPort, savedPort).filter { it > 0 }.distinct()
                for (port in candidates) {
                    val res = withTimeoutOrNull(2500L) { connect(host, port) }
                    if (res != null && res.isSuccess) return Result.success(port)
                    lastError = res?.exceptionOrNull()
                }
                delay(300L)
            }
        } finally {
            mdns?.stop()
        }
        return Result.failure(lastError ?: Exception("ADB connect port not found"))
    }

    fun disconnect() {
        scope.launch {
            mutex.withLock {
                disconnectInternal()
                _connectionState.value = AdbConnectionState.Disconnected
            }
        }
    }

    private fun disconnectInternal() {
        try {
            adbClient?.close()
        } catch (_: Exception) {
        }
        adbClient = null
    }

    suspend fun runCommand(command: String): Result<String> {
        val client = adbClient
        if (client == null) {
            if (lastPort > 0) {
                val connectRes = connect(lastHost, lastPort)
                if (connectRes.isFailure) {
                    return Result.failure(connectRes.exceptionOrNull() ?: Exception("ADB not connected"))
                }
            } else {
                return Result.failure(Exception("ADB not connected"))
            }
        }

        return try {
            val currentClient = adbClient ?: return Result.failure(Exception("ADB not connected"))
            val result = currentClient.shell(command)
            if (result.isSuccess) {
                Result.success(result.stdoutText.trim())
            } else {
                val err = result.stderrText.trim()
                Result.failure(Exception("Error (Code ${result.exitCode}): $err"))
            }
        } catch (e: Exception) {
            disconnectInternal()
            _connectionState.value = AdbConnectionState.Error(e.message ?: "Command execution error")
            Result.failure(e)
        }
    }

    suspend fun runCommandRawAdb(command: String): Result<String> = runCommand(command)

    fun runCommandSync(command: String): Result<String> {
        return runBlocking(Dispatchers.IO) {
            runCommand(command)
        }
    }
}

sealed class AdbConnectionState {
    object Disconnected : AdbConnectionState()
    object Pairing : AdbConnectionState()
    object Paired : AdbConnectionState()
    object Connecting : AdbConnectionState()
    data class Connected(val port: Int) : AdbConnectionState()
    data class Error(val message: String) : AdbConnectionState()
}
