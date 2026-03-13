package io.github.itsmelissadev.swiftsense.service.shizuku

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

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

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
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
