package io.github.itsmelissadev.swiftsense.service.shell

import io.github.itsmelissadev.swiftsense.service.adb.SwiftSenseDaemonBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object SwiftSenseBackend : ShellBackend {

    override fun isReady(): Boolean {
        return SwiftSenseDaemonBridge.isDaemonAlive()
    }

    override fun run(command: String): Result<String> {
        return run(command, 15000)
    }

    fun run(command: String, timeoutMs: Int): Result<String> {
        return runBlocking(Dispatchers.IO) {
            SwiftSenseDaemonBridge.executeDaemonCommand(command, timeoutMs)
        }
    }
}
