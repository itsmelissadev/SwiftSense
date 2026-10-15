package io.github.itsmelissadev.swiftsense.feature.downloader.engine

enum class DownloadStatus {
    QUEUED,
    CONNECTING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

enum class FileCategory {
    VIDEO,
    AUDIO,
    APK,
    ARCHIVE,
    DOCUMENT,
    OTHER
}

data class DownloadSegment(
    val index: Int,
    val startByte: Long,
    val endByte: Long,
    var downloadedBytes: Long = 0L
) {
    val totalBytes: Long get() = (endByte - startByte) + 1L
    val progress: Float get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
}

data class DownloadTask(
    val id: String,
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val speedBytesPerSec: Long,
    val threadCount: Int,
    val category: FileCategory,
    val segments: List<DownloadSegment> = emptyList(),
    val errorMessage: String? = null,
    val filePath: String? = null
) {
    val progress: Float get() = if (fileSize > 0) (downloadedBytes.toFloat() / fileSize).coerceIn(0f, 1f) else 0f
}
