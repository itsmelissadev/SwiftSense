package io.github.itsmelissadev.swiftsense.service.adb

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

import android.util.Log

object SwiftSenseDaemonBridge {

    private const val TAG = "SwiftSenseDaemon"
    const val DAEMON_PORT = SwiftSenseServer.PORT

    fun isDaemonAlive(): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress("127.0.0.1", DAEMON_PORT), 800)
                socket.soTimeout = 800
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println(SwiftSenseServer.PING_CMD)
                val response = reader.readLine()
                val alive = response != null && response.startsWith(SwiftSenseServer.PONG_RESP_PREFIX)
                Log.d(TAG, "isDaemonAlive ping response: $response -> alive=$alive")
                alive
            }
        } catch (e: Exception) {
            Log.d(TAG, "isDaemonAlive connect failed: ${e.message}")
            false
        }
    }

    fun getDaemonVersion(): Int {
        return try {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress("127.0.0.1", DAEMON_PORT), 800)
                socket.soTimeout = 800
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println(SwiftSenseServer.PING_CMD)
                val response = reader.readLine()
                val version = if (response != null && response.startsWith(SwiftSenseServer.PONG_RESP_PREFIX)) {
                    response.substringAfter(":").toIntOrNull() ?: 1
                } else 0
                Log.d(TAG, "getDaemonVersion: $version")
                version
            }
        } catch (e: Exception) {
            Log.w(TAG, "getDaemonVersion failed: ${e.message}")
            0
        }
    }

    suspend fun stopDaemon(): Result<Unit> = withContext(Dispatchers.IO) {
        Log.i(TAG, "stopDaemon requested. Attempting Step 1: Send STOP_CMD via socket...")
        val socketStopped = try {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress("127.0.0.1", DAEMON_PORT), 1000)
                socket.soTimeout = 1000
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println(SwiftSenseServer.STOP_CMD)
                val response = reader.readLine()
                Log.d(TAG, "Step 1 STOP_CMD response: $response")
                response == SwiftSenseServer.STOP_RESP
            }
        } catch (e: Exception) {
            Log.w(TAG, "Step 1 STOP_CMD socket exception: ${e.message}")
            false
        }

        kotlinx.coroutines.delay(300)
        if (!isDaemonAlive()) {
            Log.i(TAG, "Daemon stopped successfully at Step 1.")
            return@withContext Result.success(Unit)
        }

        Log.i(TAG, "Daemon still alive. Attempting Step 2: Self-kill via daemon shell...")
        try {
            val killCmd = "kill -9 \$PPID 2>/dev/null; pkill -9 -f SwiftSenseServer 2>/dev/null; pkill -9 -f swiftsense_daemon 2>/dev/null; fuser -k 19527/tcp 2>/dev/null"
            val killRes = executeDaemonCommand(killCmd)
            Log.d(TAG, "Step 2 self-kill result: $killRes")
        } catch (e: Exception) {
            Log.w(TAG, "Step 2 self-kill exception: ${e.message}")
        }

        kotlinx.coroutines.delay(400)
        if (!isDaemonAlive()) {
            Log.i(TAG, "Daemon stopped successfully at Step 2.")
            return@withContext Result.success(Unit)
        }

        Log.i(TAG, "Daemon still alive. Attempting Step 3: Raw ADB pkill...")
        try {
            val adbRes = AdbManager.runCommandRawAdb("pkill -9 -f swiftsense_daemon || killall -9 swiftsense_daemon")
            Log.d(TAG, "Step 3 raw ADB result: $adbRes")
        } catch (e: Exception) {
            Log.w(TAG, "Step 3 raw ADB exception: ${e.message}")
        }

        kotlinx.coroutines.delay(300)
        if (!isDaemonAlive()) {
            Log.i(TAG, "Daemon stopped successfully at Step 3.")
            Result.success(Unit)
        } else {
            val err = "Failed to terminate daemon. Daemon is still responsive on port $DAEMON_PORT."
            Log.e(TAG, err)
            Result.failure(Exception(err))
        }
    }

    suspend fun executeDaemonCommand(command: String, timeoutMs: Int = 15000): Result<String> = withContext(Dispatchers.IO) {
        try {
            Socket("127.0.0.1", DAEMON_PORT).use { socket ->
                socket.soTimeout = timeoutMs
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                writer.println(command)

                val response = StringBuilder()
                var line: String?
                var exitCode = 0
                while (reader.readLine().also { line = it } != null) {
                    val current = line ?: break
                    if (current == SwiftSenseServer.DONE_TAG) break
                    if (current.startsWith(SwiftSenseServer.EXIT_TAG)) {
                        exitCode = current.substringAfter(SwiftSenseServer.EXIT_TAG).toIntOrNull() ?: 0
                    } else {
                        if (response.isNotEmpty()) response.append("\n")
                        response.append(current)
                    }
                }

                if (exitCode == 0) {
                    Result.success(response.toString().trim())
                } else {
                    Result.failure(Exception("Exit Code $exitCode: ${response.toString().trim()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun spawnDaemonViaAdb(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        if (isDaemonAlive()) {
            if (getDaemonVersion() < SwiftSenseServer.VERSION) {
                stopDaemon()
                kotlinx.coroutines.delay(300)
            } else {
                return@withContext Result.success(Unit)
            }
        }

        val apkPath = context.packageCodePath
        val className = SwiftSenseServer::class.java.name

        val launchCommand = "sh -c 'export CLASSPATH=\"$apkPath\"; exec setsid /system/bin/app_process /system/bin --nice-name=swiftsense_daemon $className </dev/null >/dev/null 2>&1 &' || sh -c 'export CLASSPATH=\"$apkPath\"; nohup /system/bin/app_process /system/bin --nice-name=swiftsense_daemon $className </dev/null >/dev/null 2>&1 &'"

        val spawnRes = AdbManager.runCommandRawAdb(launchCommand)
        if (spawnRes.isFailure) {
            return@withContext Result.failure(spawnRes.exceptionOrNull() ?: Exception("Failed to launch daemon"))
        }

        for (i in 0 until 15) {
            kotlinx.coroutines.delay(200)
            if (isDaemonAlive()) {
                return@withContext Result.success(Unit)
            }
        }

        if (isDaemonAlive()) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Daemon failed to start"))
        }
    }
}
