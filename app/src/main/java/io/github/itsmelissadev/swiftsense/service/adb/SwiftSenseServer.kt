package io.github.itsmelissadev.swiftsense.service.adb

import android.os.Looper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class SwiftSenseServer {

    companion object {
        const val VERSION = 3
        const val PORT = 19527
        const val PING_CMD = "__SWIFTSENSE_PING__"
        const val PONG_RESP_PREFIX = "__SWIFTSENSE_PONG__"
        const val PONG_RESP = "__SWIFTSENSE_PONG__:3"
        const val STOP_CMD = "__SWIFTSENSE_STOP__"
        const val STOP_RESP = "__SWIFTSENSE_STOPPED__"
        const val EXIT_TAG = "__SWIFTSENSE_EXIT__:"
        const val DONE_TAG = "__SWIFTSENSE_DONE__"

        @Volatile
        private var serverSocketInstance: ServerSocket? = null

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                Looper.prepareMainLooper()
            } catch (_: Exception) {
            }

            val threadPool = Executors.newCachedThreadPool()
            val serverSocket = try {
                ServerSocket(PORT).also { serverSocketInstance = it }
            } catch (e: Exception) {
                return
            }

            while (true) {
                try {
                    val client = serverSocket.accept()
                    threadPool.execute {
                        handleClient(client)
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }

        private fun handleClient(socket: Socket) {
            try {
                socket.soTimeout = 120000
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                val line = reader.readLine()
                if (line == null) {
                    socket.close()
                    return
                }

                if (line == PING_CMD) {
                    writer.println(PONG_RESP)
                    writer.println(DONE_TAG)
                    socket.close()
                    return
                }

                if (line == STOP_CMD) {
                    writer.println(STOP_RESP)
                    writer.println(DONE_TAG)
                    writer.flush()
                    try { socket.close() } catch (_: Exception) {}
                    try { serverSocketInstance?.close() } catch (_: Exception) {}
                    kotlin.concurrent.thread {
                        try { Thread.sleep(50) } catch (_: Exception) {}
                        android.os.Process.killProcess(android.os.Process.myPid())
                        System.exit(0)
                    }
                    return
                }

                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", line))
                val outputReader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                var outLine: String?
                while (outputReader.readLine().also { outLine = it } != null) {
                    writer.println(outLine)
                }

                var errLine: String?
                while (errorReader.readLine().also { errLine = it } != null) {
                    writer.println(errLine)
                }

                val exitCode = process.waitFor()
                writer.println("$EXIT_TAG$exitCode")
                writer.println(DONE_TAG)
                socket.close()
            } catch (_: Exception) {
                try {
                    socket.close()
                } catch (_: Exception) {
                }
            }
        }
    }
}
