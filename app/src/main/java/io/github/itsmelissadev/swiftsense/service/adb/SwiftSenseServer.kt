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
        const val VERSION = 4
        const val PORT = 19527
        const val PING_CMD = "__SWIFTSENSE_PING__"
        const val PONG_RESP_PREFIX = "__SWIFTSENSE_PONG__"
        const val PONG_RESP = "__SWIFTSENSE_PONG__:4"
        const val STOP_CMD = "__SWIFTSENSE_STOP__"
        const val STOP_RESP = "__SWIFTSENSE_STOPPED__"
        const val EXIT_TAG = "__SWIFTSENSE_EXIT__:"
        const val DONE_TAG = "__SWIFTSENSE_DONE__"

        @Volatile
        private var serverSocketInstance: ServerSocket? = null

        @JvmStatic
        @Suppress("DEPRECATION")
        fun main(args: Array<String>) {
            try {
                Looper.prepareMainLooper()
            } catch (_: Exception) {
            }

            val threadPool = Executors.newFixedThreadPool(4)
            val serverSocket = try {
                ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(java.net.InetAddress.getByName("127.0.0.1"), PORT), 50)
                }.also { serverSocketInstance = it }
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
            var process: Process? = null
            try {
                socket.tcpNoDelay = true
                socket.setSoLinger(true, 0)
                socket.soTimeout = 30000
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

                val proc = ProcessBuilder("sh", "-c", line)
                    .redirectErrorStream(true)
                    .start()
                process = proc

                val outputReader = BufferedReader(InputStreamReader(proc.inputStream))
                var outLine: String?
                while (outputReader.readLine().also { outLine = it } != null) {
                    writer.println(outLine)
                }

                val exitCode = proc.waitFor()
                writer.println("$EXIT_TAG$exitCode")
                writer.println(DONE_TAG)
                socket.close()
            } catch (_: Exception) {
                try {
                    socket.close()
                } catch (_: Exception) {
                }
            } finally {
                try {
                    process?.let {
                        if (it.isAlive) {
                            it.destroyForcibly()
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}
