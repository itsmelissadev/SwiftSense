package io.github.itsmelissadev.swiftsense.feature.downloader.engine

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object TurboDownloadEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    fun getDownloadDir(context: Context): File {
        return try {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dir = File(publicDir, "SwiftSense")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            if (dir.exists() && dir.canWrite()) {
                val testFile = File(dir, ".probe_${System.currentTimeMillis()}")
                if (testFile.createNewFile()) {
                    testFile.delete()
                    dir
                } else {
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                }
            } else {
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            }
        } catch (_: Exception) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        }
    }

    fun openReliableConnection(
        rawUrl: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        maxRedirects: Int = 10
    ): Pair<HttpURLConnection, String> {
        var currentUrl = rawUrl
        var redirects = 0
        var cookies: String? = null

        while (redirects < maxRedirects) {
            val url = URL(currentUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15000
                readTimeout = 15000
                instanceFollowRedirects = false
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9,tr;q=0.8")
                setRequestProperty("Accept-Encoding", "identity")
                if (!cookies.isNullOrBlank()) {
                    setRequestProperty("Cookie", cookies)
                }
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }

            val setCookie = conn.getHeaderField("Set-Cookie")
            if (!setCookie.isNullOrBlank()) {
                val cookiePart = setCookie.substringBefore(";")
                cookies = if (cookies == null) cookiePart else "$cookies; $cookiePart"
            }

            val code = conn.responseCode
            if (code in listOf(301, 302, 303, 307, 308)) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (!location.isNullOrBlank()) {
                    currentUrl = if (location.startsWith("http://", ignoreCase = true) || location.startsWith("https://", ignoreCase = true)) {
                        location
                    } else {
                        URL(url, location).toString()
                    }
                    redirects++
                    continue
                }
            }
            return Pair(conn, currentUrl)
        }
        throw Exception("Too many redirects ($maxRedirects)")
    }

    suspend fun probeUrl(rawUrl: String): Result<Pair<String, Long>> = withContext(Dispatchers.IO) {
        try {
            var resolvedUrl = rawUrl
            var size = -1L
            var disposition: String? = null

            try {
                val (headConn, finalUrl) = openReliableConnection(rawUrl, "HEAD")
                resolvedUrl = finalUrl
                val code = headConn.responseCode
                if (code in 200..299) {
                    size = headConn.contentLengthLong
                    disposition = headConn.getHeaderField("Content-Disposition")
                }
                headConn.disconnect()
            } catch (_: Exception) {}

            if (size <= 0) {
                try {
                    val (getConn, finalUrl) = openReliableConnection(
                        resolvedUrl,
                        "GET",
                        mapOf("Range" to "bytes=0-0")
                    )
                    resolvedUrl = finalUrl
                    val code = getConn.responseCode
                    if (code == HttpURLConnection.HTTP_PARTIAL || code == HttpURLConnection.HTTP_OK) {
                        val contentRange = getConn.getHeaderField("Content-Range")
                        if (contentRange != null && contentRange.contains("/")) {
                            size = contentRange.substringAfter("/").trim().toLongOrNull() ?: -1L
                        }
                        if (size <= 0) {
                            size = getConn.contentLengthLong
                        }
                        if (disposition.isNullOrBlank()) {
                            disposition = getConn.getHeaderField("Content-Disposition")
                        }
                    }
                    getConn.disconnect()
                } catch (_: Exception) {}
            }

            var fileName = extractFileName(disposition, resolvedUrl)
            if (fileName.isBlank()) {
                fileName = "download_" + System.currentTimeMillis()
            }
            fileName = sanitizeFileName(fileName)

            Result.success(Pair(fileName, size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun startDownload(
        context: Context,
        url: String,
        suggestedName: String? = null,
        threadCount: Int = 8
    ): String {
        val id = UUID.randomUUID().toString()
        val initialCategory = detectCategory(suggestedName ?: url)
        val fallbackName = extractFileName(null, url).ifBlank { "download_${System.currentTimeMillis()}" }
        val sanitizedFallback = sanitizeFileName(fallbackName)

        val initialTask = DownloadTask(
            id = id,
            url = url,
            fileName = suggestedName ?: sanitizedFallback,
            fileSize = 0L,
            downloadedBytes = 0L,
            status = DownloadStatus.CONNECTING,
            speedBytesPerSec = 0L,
            threadCount = threadCount.coerceIn(1, 16),
            category = initialCategory
        )

        updateTask(initialTask)

        val job = scope.launch {
            executeDownload(context, id, url, suggestedName, threadCount.coerceIn(1, 16))
        }
        activeJobs[id] = job
        return id
    }

    fun pauseDownload(id: String) {
        val job = activeJobs.remove(id)
        job?.cancel()
        val current = _tasks.value.find { it.id == id } ?: return
        if (current.status == DownloadStatus.DOWNLOADING || current.status == DownloadStatus.CONNECTING) {
            updateTask(current.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L))
        }
    }

    fun resumeDownload(context: Context, id: String) {
        val current = _tasks.value.find { it.id == id } ?: return
        if (current.status != DownloadStatus.PAUSED && current.status != DownloadStatus.FAILED) return

        val job = scope.launch {
            executeDownload(context, id, current.url, current.fileName, current.threadCount)
        }
        activeJobs[id] = job
    }

    fun cancelDownload(id: String) {
        val job = activeJobs.remove(id)
        job?.cancel()
        val current = _tasks.value.find { it.id == id }
        if (current?.filePath != null) {
            try {
                File(current.filePath).delete()
            } catch (_: Exception) {}
        }
        _tasks.value = _tasks.value.filter { it.id != id }
    }

    private suspend fun executeDownload(
        context: Context,
        taskId: String,
        rawUrl: String,
        customName: String?,
        requestedThreads: Int
    ) = withContext(Dispatchers.IO) {
        var task = _tasks.value.find { it.id == taskId } ?: return@withContext
        updateTask(task.copy(status = DownloadStatus.CONNECTING))

        val probe = probeUrl(rawUrl).getOrNull()
        val candidateName = customName?.takeIf { it.isNotBlank() } ?: probe?.first ?: ("file_" + System.currentTimeMillis())
        val finalFileName = sanitizeFileName(candidateName)
        val fileSize = probe?.second ?: -1L
        val downloadDir = getDownloadDir(context)
        val destinationFile = File(downloadDir, finalFileName)

        val category = detectCategory(finalFileName)

        if (fileSize <= 0) {
            downloadSingleStream(taskId, rawUrl, destinationFile, category, finalFileName)
            return@withContext
        }

        val rangeSupported = checkRangeSupport(rawUrl)
        if (!rangeSupported || requestedThreads <= 1 || fileSize < 1024 * 1024) {
            downloadSingleStream(taskId, rawUrl, destinationFile, category, finalFileName, fileSize)
            return@withContext
        }

        val existingSegments = task.segments
        val segments = if (existingSegments.isNotEmpty() && existingSegments.size == requestedThreads) {
            existingSegments
        } else {
            createSegments(fileSize, requestedThreads)
        }

        task = task.copy(
            fileName = finalFileName,
            fileSize = fileSize,
            category = category,
            filePath = destinationFile.absolutePath,
            segments = segments,
            status = DownloadStatus.DOWNLOADING
        )
        updateTask(task)

        val totalDownloaded = AtomicLong(segments.sumOf { it.downloadedBytes })
        var lastDownloaded = totalDownloaded.get()
        var lastTime = System.currentTimeMillis()

        val speedTrackerJob = launch {
            while (isActive) {
                delay(1000)
                val current = totalDownloaded.get()
                val now = System.currentTimeMillis()
                val timeDiff = (now - lastTime).coerceAtLeast(1)
                val speed = ((current - lastDownloaded) * 1000) / timeDiff
                lastDownloaded = current
                lastTime = now

                val t = _tasks.value.find { it.id == taskId } ?: break
                if (t.status == DownloadStatus.DOWNLOADING) {
                    updateTask(t.copy(downloadedBytes = current, speedBytesPerSec = speed, segments = segments.toList()))
                }
            }
        }

        var segmentFailureOccurred = false

        try {
            RandomAccessFile(destinationFile, "rw").use { raf ->
                raf.setLength(fileSize)
            }

            val workerJobs = segments.map { segment ->
                launch {
                    val remainingStart = segment.startByte + segment.downloadedBytes
                    if (remainingStart > segment.endByte) return@launch

                    val (conn, _) = openReliableConnection(
                        rawUrl = rawUrl,
                        method = "GET",
                        headers = mapOf("Range" to "bytes=$remainingStart-${segment.endByte}")
                    )

                    val code = conn.responseCode
                    if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK) {
                        conn.disconnect()
                        segmentFailureOccurred = true
                        throw Exception("Segment HTTP $code")
                    }

                    conn.inputStream.use { stream ->
                        RandomAccessFile(destinationFile, "rw").use { targetRaf ->
                            targetRaf.seek(remainingStart)
                            val buffer = ByteArray(16384)
                            var bytesRead: Int
                            while (stream.read(buffer).also { bytesRead = it } != -1) {
                                if (!isActive) break
                                targetRaf.write(buffer, 0, bytesRead)
                                segment.downloadedBytes += bytesRead
                                totalDownloaded.addAndGet(bytesRead.toLong())
                            }
                        }
                    }
                    conn.disconnect()
                }
            }

            workerJobs.forEach { it.join() }
            speedTrackerJob.cancel()

            val finalDownloaded = totalDownloaded.get()
            if (finalDownloaded >= fileSize) {
                updateTask(
                    task.copy(
                        downloadedBytes = fileSize,
                        status = DownloadStatus.COMPLETED,
                        speedBytesPerSec = 0L,
                        segments = segments.toList()
                    )
                )
            }
        } catch (e: Exception) {
            speedTrackerJob.cancel()
            if (e is CancellationException) {
                updateTask(task.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L))
            } else if (segmentFailureOccurred) {
                downloadSingleStream(taskId, rawUrl, destinationFile, category, finalFileName, fileSize)
            } else {
                updateTask(
                    task.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = e.message,
                        speedBytesPerSec = 0L
                    )
                )
            }
        }
    }

    private suspend fun downloadSingleStream(
        taskId: String,
        rawUrl: String,
        destinationFile: File,
        category: FileCategory,
        fileName: String,
        knownSize: Long = -1L
    ) = withContext(Dispatchers.IO) {
        var task = _tasks.value.find { it.id == taskId } ?: return@withContext
        updateTask(
            task.copy(
                fileName = fileName,
                fileSize = knownSize,
                category = category,
                filePath = destinationFile.absolutePath,
                status = DownloadStatus.DOWNLOADING
            )
        )

        val totalDownloaded = AtomicLong(destinationFile.length())
        var lastDownloaded = totalDownloaded.get()
        var lastTime = System.currentTimeMillis()

        val speedTrackerJob = launch {
            while (isActive) {
                delay(1000)
                val current = totalDownloaded.get()
                val now = System.currentTimeMillis()
                val timeDiff = (now - lastTime).coerceAtLeast(1)
                val speed = ((current - lastDownloaded) * 1000) / timeDiff
                lastDownloaded = current
                lastTime = now

                val t = _tasks.value.find { it.id == taskId } ?: break
                if (t.status == DownloadStatus.DOWNLOADING) {
                    updateTask(t.copy(downloadedBytes = current, speedBytesPerSec = speed))
                }
            }
        }

        try {
            destinationFile.parentFile?.mkdirs()

            val headers = mutableMapOf<String, String>()
            if (totalDownloaded.get() > 0) {
                headers["Range"] = "bytes=${totalDownloaded.get()}-"
            }

            val (conn, _) = openReliableConnection(rawUrl, "GET", headers)

            val append = totalDownloaded.get() > 0 && conn.responseCode == HttpURLConnection.HTTP_PARTIAL
            val actualSize = if (knownSize > 0) knownSize else conn.contentLengthLong

            conn.inputStream.use { input ->
                val fos = FileOutputStream(destinationFile, append)
                fos.use { output ->
                    val buffer = ByteArray(16384)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (!isActive) break
                        output.write(buffer, 0, read)
                        totalDownloaded.addAndGet(read.toLong())
                    }
                }
            }
            conn.disconnect()
            speedTrackerJob.cancel()

            updateTask(
                task.copy(
                    downloadedBytes = totalDownloaded.get(),
                    fileSize = if (actualSize > 0) actualSize else totalDownloaded.get(),
                    status = DownloadStatus.COMPLETED,
                    speedBytesPerSec = 0L
                )
            )
        } catch (e: Exception) {
            speedTrackerJob.cancel()
            if (e is CancellationException) {
                updateTask(task.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L))
            } else {
                updateTask(
                    task.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = e.message,
                        speedBytesPerSec = 0L
                    )
                )
            }
        }
    }

    private fun checkRangeSupport(rawUrl: String): Boolean {
        return try {
            val (conn, _) = openReliableConnection(
                rawUrl = rawUrl,
                method = "GET",
                headers = mapOf("Range" to "bytes=0-0")
            )
            val code = conn.responseCode
            conn.disconnect()
            code == HttpURLConnection.HTTP_PARTIAL
        } catch (_: Exception) {
            false
        }
    }

    private fun createSegments(totalSize: Long, count: Int): List<DownloadSegment> {
        val segments = mutableListOf<DownloadSegment>()
        val chunkSize = totalSize / count
        for (i in 0 until count) {
            val start = i * chunkSize
            val end = if (i == count - 1) totalSize - 1 else (start + chunkSize - 1)
            segments.add(DownloadSegment(index = i, startByte = start, endByte = end, downloadedBytes = 0L))
        }
        return segments
    }

    private fun updateTask(newTask: DownloadTask) {
        val currentList = _tasks.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == newTask.id }
        if (index != -1) {
            currentList[index] = newTask
        } else {
            currentList.add(0, newTask)
        }
        _tasks.value = currentList
    }

    fun sanitizeFileName(name: String): String {
        var clean = name.replace("[\\\\/:*?\"<>|]".toRegex(), "_").trim()
        clean = clean.substringBefore("?")
        if (clean.length > 180) {
            val ext = clean.substringAfterLast(".", "")
            val base = clean.substringBeforeLast(".")
            clean = if (ext.isNotEmpty()) "${base.take(170)}.$ext" else base.take(180)
        }
        return clean.ifBlank { "download_${System.currentTimeMillis()}" }
    }

    private fun extractFileName(disposition: String?, urlString: String): String {
        if (!disposition.isNullOrBlank()) {
            val matchUtf = Regex("filename\\*=(?:UTF-8'')?([^;'\"]+)", RegexOption.IGNORE_CASE).find(disposition)
            if (matchUtf != null) {
                try {
                    return URLDecoder.decode(matchUtf.groupValues[1].trim(), "UTF-8")
                } catch (_: Exception) {}
            }
            val match = Regex("filename=['\"]?([^;'\"]+)", RegexOption.IGNORE_CASE).find(disposition)
            if (match != null) {
                try {
                    return URLDecoder.decode(match.groupValues[1].trim(), "UTF-8")
                } catch (_: Exception) {}
            }
        }
        return try {
            val path = URL(urlString).path
            val raw = path.substringAfterLast("/").substringBefore("?")
            if (raw.isNotBlank()) URLDecoder.decode(raw, "UTF-8") else ""
        } catch (_: Exception) {
            ""
        }
    }

    fun detectCategory(fileNameOrUrl: String): FileCategory {
        val ext = fileNameOrUrl.substringBefore("?").substringAfterLast(".", "").lowercase()
        return when (ext) {
            "mp4", "mkv", "webm", "avi", "mov", "flv", "wmv", "3gp" -> FileCategory.VIDEO
            "mp3", "m4a", "aac", "flac", "wav", "ogg", "opus" -> FileCategory.AUDIO
            "apk", "xapk", "apks", "apkm" -> FileCategory.APK
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> FileCategory.ARCHIVE
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt" -> FileCategory.DOCUMENT
            else -> FileCategory.OTHER
        }
    }
}
