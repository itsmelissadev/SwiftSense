package io.github.itsmelissadev.swiftsense.service.shell

interface ShellBackend {
    fun isReady(): Boolean
    fun run(command: String): Result<String>
}
