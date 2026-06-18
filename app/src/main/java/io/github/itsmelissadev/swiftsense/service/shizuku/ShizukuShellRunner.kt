package io.github.itsmelissadev.swiftsense.service.shizuku

import rikka.shizuku.Shizuku

object ShizukuShellRunner {

    fun runCommand(command: String): Result<String> {
        return try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true

            val process = newProcessMethod.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as Process

            var error = ""
            val errorReaderThread = Thread {
                try {
                    error = process.errorStream.bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    error = e.message ?: "Error reading error stream"
                }
            }
            errorReaderThread.start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            errorReaderThread.join()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success(output.trim())
            } else {
                Result.failure(Exception("Error (Code $exitCode): $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
