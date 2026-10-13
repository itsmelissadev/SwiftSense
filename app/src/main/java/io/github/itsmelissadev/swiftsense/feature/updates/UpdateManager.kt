package io.github.itsmelissadev.swiftsense.feature.updates

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val GITHUB_RELEASES_API = "https://api.github.com/repos/itsmelissadev/SwiftSense/releases"

    suspend fun fetchReleases(): Result<List<GitHubRelease>> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASES_API)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SwiftSense-Android-App")
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.failure(Exception("HTTP $responseCode: ${conn.responseMessage}"))
            }

            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonText)
            val releases = mutableListOf<GitHubRelease>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optLong("id")
                val tagName = obj.optString("tag_name", "")
                val name = obj.optString("name", tagName)
                val body = obj.optString("body", "")
                val publishedAt = obj.optString("published_at", "")
                val htmlUrl = obj.optString("html_url", "")
                val isPrerelease = obj.optBoolean("prerelease", false)

                val assetsArray = obj.optJSONArray("assets")
                var apkAsset: GitHubAsset? = null

                if (assetsArray != null) {
                    for (j in 0 until assetsArray.length()) {
                        val assetObj = assetsArray.getJSONObject(j)
                        val assetName = assetObj.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            apkAsset = GitHubAsset(
                                id = assetObj.optLong("id"),
                                name = assetName,
                                size = assetObj.optLong("size", 0L),
                                downloadUrl = assetObj.optString("browser_download_url", ""),
                                downloadCount = assetObj.optInt("download_count", 0)
                            )
                            if (assetName.equals("app-release.apk", ignoreCase = true)) {
                                break
                            }
                        }
                    }
                }

                releases.add(
                    GitHubRelease(
                        id = id,
                        tagName = tagName,
                        name = name,
                        body = body,
                        publishedAt = publishedAt,
                        htmlUrl = htmlUrl,
                        isPrerelease = isPrerelease,
                        apkAsset = apkAsset
                    )
                )
            }

            Result.success(releases)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun enqueueDownload(
        context: Context,
        downloadUrl: String,
        fileName: String,
        title: String
    ): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle(title)
            setDescription(fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
        }
        return downloadManager.enqueue(request)
    }

    fun getDownloadedFile(context: Context, downloadId: Long): File? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query) ?: return null
        cursor.use {
            if (it.moveToFirst()) {
                val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIndex != -1 && it.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                    val uriIndex = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    if (uriIndex != -1) {
                        val uriString = it.getString(uriIndex)
                        if (!uriString.isNullOrEmpty()) {
                            val uri = Uri.parse(uriString)
                            return if (uri.scheme == "file") {
                                File(uri.path ?: "")
                            } else {
                                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                val titleIndex = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
                                val title = if (titleIndex != -1) it.getString(titleIndex) else "app-release.apk"
                                File(downloadsDir, title)
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    fun getDownloadProgress(context: Context, downloadId: Long): Int {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query) ?: return -1
        cursor.use {
            if (it.moveToFirst()) {
                val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = if (statusIndex != -1) it.getInt(statusIndex) else -1
                if (status == DownloadManager.STATUS_SUCCESSFUL) return 100
                if (status == DownloadManager.STATUS_FAILED) return -2

                val bytesDownloadedIndex = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalIndex = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                    val downloaded = it.getLong(bytesDownloadedIndex)
                    val total = it.getLong(bytesTotalIndex)
                    if (total > 0) {
                        return ((downloaded * 100) / total).toInt().coerceIn(0, 99)
                    }
                }
                return 0
            }
        }
        return -1
    }

    fun launchApkInstaller(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return String.format(java.util.Locale.US, "%.1f %s", value, units[index])
    }
}
