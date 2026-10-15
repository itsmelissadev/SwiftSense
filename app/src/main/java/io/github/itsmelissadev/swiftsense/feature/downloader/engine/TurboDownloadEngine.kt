package io.github.itsmelissadev.swiftsense.feature.downloader.engine

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
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
import java.io.InputStream
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
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(publicDir, "SwiftSense")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return if (dir.exists() && dir.canWrite()) dir else context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
    }

    suspend fun probeUrl(rawUrl: String): Result<Pair<String, Long>> = withContext(Dispatchers.IO) {
        try {
            val url = URL(rawUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                instanceFollowRedirects = true
            }

            var size = conn.contentLengthLong
            var disposition = conn.getHeaderField("Content-Disposition")
            var resolvedUrl = conn.url.toString()
            conn.disconnect()

            if (size <= 0) {
                val getConn = (URL(resolvedUrl).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("Range", "bytes=0-0")
                    connectTimeout = 12000
                    readTimeout = 12000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                }
                val contentRange = getConn.getHeaderField("Content-Range")
                if (contentRange != null && contentRange.contains("/")) {
                    size = contentRange.substringAfter("/").toLongOrNull() ?: -1L
                }
                if (disposition == null) disposition = getConn.getHeaderField("Content-Disposition")
                getConn.disconnect()
            }

            var fileName = extractFileName(disposition, resolvedUrl)
            if (fileName.isBlank()) fileName = "download_" + System.currentTimeMillis()

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
        val initialTask = DownloadTask(
            id = id,
            url = url,
            fileName = suggestedName ?: fallbackName,
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
            try { File(current.filePath).delete() } catch (_: Exception) {}
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
        val finalFileName = customName?.takeIf { it.isNotBlank() } ?: probe?.first ?: ("file_" + System.currentTimeMillis())
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

        try {
            RandomAccessFile(destinationFile, "rw").use { raf ->
                raf.setLength(fileSize)
            }

            val workerJobs = segments.map { segment ->
                launch {
                    val remainingStart = segment.startByte + segment.downloadedBytes
                    if (remainingStart > segment.endByte) return@launch

                    val conn = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
                        setRequestProperty("Range", "bytes=$remainingStart-${segment.endByte}")
                        connectTimeout = 15000
                        readTimeout = 15000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                    }

                    val code = conn.responseCode
                    if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK) {
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
            } else {
                updateTask(
                    task.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = e.message ?: "Download failed",
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
            val conn = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
                if (totalDownloaded.get() > 0) {
                    setRequestProperty("Range", "bytes=${totalDownloaded.get()}-")
                }
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
            }

            val append = totalDownloaded.get() > 0 && conn.responseCode == HttpURLConnection.HTTP_PARTIAL
            val actualSize = if (knownSize > 0) knownSize else conn.contentLengthLong

            conn.inputStream.use { input ->
                val fos = java.io.FileOutputStream(destinationFile, append)
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
                        errorMessage = e.message ?: "Single-stream download failed",
                        speedBytesPerSec = 0L
                    )
                )
            }
        }
    }

    private fun checkRangeSupport(rawUrl: String): Boolean {
        return try {
            val conn = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
                setRequestProperty("Range", "bytes=0-0")
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
            }
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

    private fun extractFileName(disposition: String?, urlString: String): String {
        if (!disposition.isNullOrBlank()) {
            val match = Regex("filename\\*?=['\"]?(?:UTF-8'')?([^;'\"]+)", RegexOption.IGNORE_CASE).find(disposition)
            if (match != null) {
                return try {
                    URLDecoder.decode(match.groupValues[1].trim(), "UTF-8")
                } catch (_: Exception) {
                    match.groupValues[1].trim()
                }
            }
        }
        return try {
            val path = URL(urlString).path
            val raw = path.substringAfterLast("/")
            if (raw.isNotBlank()) URLDecoder.decode(raw, "UTF-8") else ""
        } catch (_: Exception) {
            ""
        }
    }

    fun detectCategory(fileNameOrUrl: String): FileCategory {
        val ext = fileNameOrUrl.substringAfterLast(".", "").lowercase()
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
