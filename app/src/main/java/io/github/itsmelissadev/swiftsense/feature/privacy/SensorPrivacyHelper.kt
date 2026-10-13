package io.github.itsmelissadev.swiftsense.feature.privacy

import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import io.github.itsmelissadev.swiftsense.service.shell.ShellBridge

object SensorPrivacyHelper {

    fun isSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    fun isShizukuAvailable(): Boolean {
        return ShellBridge.ready.value
    }

    fun setSensorEnabled(sensor: String, enabled: Boolean): Result<Unit> {
        if (!isSupported()) {
            return Result.failure(IllegalStateException("Requires Android 12 or higher"))
        }

        if (!isShizukuAvailable()) {
            return Result.failure(IllegalStateException("Execution backend is not ready"))
        }

        val userId = try {
            Process.myUid() / 100000
        } catch (_: Exception) {
            0
        }

        val action = if (enabled) "disable" else "enable"
        val command = "cmd sensor_privacy $action $userId $sensor"
        val result = ShellBridge.run(command)

        if (result.isSuccess) {
            return Result.success(Unit)
        }

        if (userId != 0) {
            val fallbackCommand = "cmd sensor_privacy $action 0 $sensor"
            val fallbackResult = ShellBridge.run(fallbackCommand)
            if (fallbackResult.isSuccess) {
                return Result.success(Unit)
            }
        }

        return Result.failure(result.exceptionOrNull() ?: Exception("Failed to execute sensor_privacy command"))
    }
}
