package io.github.itsmelissadev.swiftsense.feature.apkinstaller

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.pm.PackageInfoCompat
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.service.shell.ShellBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile

object ApkInstallerHelper {

    private const val HISTORY_PREFS = "apk_install_history_prefs"
    private const val KEY_HISTORY_ITEMS = "key_history_items"

    fun resolveFileName(context: Context, uri: Uri): String {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {
        }
        return name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "package.apk"
    }

    private fun computeSha256(signature: Signature): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signature.toByteArray())
            digest.joinToString(":") { String.format(Locale.US, "%02X", it) }
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun parsePackage(context: Context, uri: Uri): Result<ApkPackageDetails> = withContext(Dispatchers.IO) {
        val fileName = resolveFileName(context, uri)
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val stagingDir = File(cacheDir, "pkg_staging_${System.currentTimeMillis()}").apply {
            mkdirs()
            setReadable(true, false)
            setExecutable(true, false)
        }

        try {
            var isMultiApkArchive = false
            val apkFiles = mutableListOf<File>()
            val obbFiles = mutableListOf<Pair<String, String>>()
            var manifestJson: JSONObject? = null
            var rootIconDrawable: Drawable? = null

            val rawPackageFile = File(stagingDir, "raw_package.tmp")
            context.contentResolver.openInputStream(uri)?.use { inStream ->
                FileOutputStream(rawPackageFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            } ?: return@withContext Result.failure(Exception("Cannot read package stream"))

            var zipFile: ZipFile? = null
            try {
                zipFile = ZipFile(rawPackageFile)
            } catch (_: Exception) {
            }

            if (zipFile != null) {
                zipFile.use { zip ->
                    val manifestEntry = zip.getEntry("manifest.json")
                        ?: zip.entries().asSequence().find { File(it.name).name.equals("manifest.json", ignoreCase = true) }
                    if (manifestEntry != null) {
                        try {
                            val text = zip.getInputStream(manifestEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                            manifestJson = JSONObject(text)
                        } catch (_: Exception) {
                        }
                    }

                    val iconEntry = zip.getEntry("icon.png")
                        ?: zip.entries().asSequence().find { File(it.name).name.equals("icon.png", ignoreCase = true) }
                    if (iconEntry != null) {
                        try {
                            val bmp = zip.getInputStream(iconEntry).use { BitmapFactory.decodeStream(it) }
                            if (bmp != null) {
                                rootIconDrawable = BitmapDrawable(context.resources, bmp)
                            }
                        } catch (_: Exception) {
                        }
                    }

                    val apkEntries = zip.entries().asSequence().filter {
                        !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true)
                    }.toList()

                    val obbEntries = zip.entries().asSequence().filter {
                        !it.isDirectory && it.name.endsWith(".obb", ignoreCase = true)
                    }.toList()

                    if (apkEntries.isNotEmpty()) {
                        isMultiApkArchive = true
                        for (entry in apkEntries) {
                            val simpleName = File(entry.name).name
                            val target = File(stagingDir, simpleName)
                            zip.getInputStream(entry).use { inStream ->
                                FileOutputStream(target).use { outStream ->
                                    inStream.copyTo(outStream)
                                }
                            }
                            target.setReadable(true, false)
                            apkFiles.add(target)
                        }

                        for (entry in obbEntries) {
                            val simpleName = File(entry.name).name
                            val obbDir = File(stagingDir, "obb").apply { mkdirs() }
                            val target = File(obbDir, simpleName)
                            zip.getInputStream(entry).use { inStream ->
                                FileOutputStream(target).use { outStream ->
                                    inStream.copyTo(outStream)
                                }
                            }
                            target.setReadable(true, false)
                            val relPath = if (entry.name.contains("Android/obb/", ignoreCase = true)) {
                                entry.name.substring(entry.name.indexOf("Android/obb/", ignoreCase = true)).trimStart('/')
                            } else {
                                "Android/obb/$simpleName"
                            }
                            obbFiles.add(target.absolutePath to relPath)
                        }
                    }
                }
            }

            if (isMultiApkArchive) {
                rawPackageFile.delete()
            } else {
                val isZipOrApk = rawPackageFile.length() >= 4 && try {
                    rawPackageFile.inputStream().use { stream ->
                        val b0 = stream.read()
                        val b1 = stream.read()
                        val b2 = stream.read()
                        val b3 = stream.read()
                        b0 == 0x50 && b1 == 0x4B && (b2 == 0x03 || b2 == 0x05 || b2 == 0x07) && (b3 == 0x04 || b3 == 0x06 || b3 == 0x08)
                    }
                } catch (_: Exception) {
                    false
                }

                if (!isZipOrApk) {
                    stagingDir.deleteRecursively()
                    return@withContext Result.failure(
                        Exception(context.getString(R.string.apk_invalid_format))
                    )
                }

                val singleApk = File(stagingDir, "base.apk")
                if (!rawPackageFile.renameTo(singleApk)) {
                    rawPackageFile.copyTo(singleApk, overwrite = true)
                    rawPackageFile.delete()
                }
                singleApk.setReadable(true, false)
                apkFiles.clear()
                apkFiles.add(singleApk)
            }

            var baseApkFile: File? = null

            val manifestBaseName = manifestJson?.optJSONArray("split_apks")?.let { arr ->
                var baseName: String? = null
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj?.optString("id") == "base") {
                        baseName = obj.optString("file")
                        break
                    }
                }
                baseName
            }
            if (!manifestBaseName.isNullOrBlank()) {
                baseApkFile = apkFiles.find { it.name.equals(manifestBaseName, ignoreCase = true) }
            }

            if (baseApkFile == null) {
                val manifestPkg = manifestJson?.optString("package_name")
                if (!manifestPkg.isNullOrBlank()) {
                    baseApkFile = apkFiles.find {
                        it.name.equals("$manifestPkg.apk", ignoreCase = true) ||
                                it.name.startsWith(manifestPkg, ignoreCase = true)
                    }
                }
            }

            if (baseApkFile == null) {
                baseApkFile = apkFiles.find {
                    it.name.startsWith("base", ignoreCase = true) ||
                            it.name.contains("master", ignoreCase = true)
                }
            }

            if (baseApkFile == null) {
                baseApkFile = apkFiles.maxByOrNull { it.length() }
            }

            val targetBase = baseApkFile ?: apkFiles.firstOrNull()
                ?: return@withContext Result.failure(Exception("No APK found in package"))

            val tempProbeFile = File(context.cacheDir, "probe_${System.currentTimeMillis()}.apk")
            var packageInfo: android.content.pm.PackageInfo? = null
            var certSha256: String? = null

            try {
                targetBase.inputStream().use { input ->
                    FileOutputStream(tempProbeFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_PERMISSIONS
                } else {
                    @Suppress("DEPRECATION")
                    PackageManager.GET_SIGNATURES or PackageManager.GET_PERMISSIONS
                }

                packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageArchiveInfo(
                        tempProbeFile.absolutePath,
                        PackageManager.PackageInfoFlags.of(flags.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageArchiveInfo(tempProbeFile.absolutePath, flags)
                }

                val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo?.signingInfo?.apkContentsSigners
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo?.signatures
                }
                val sig = signatures?.firstOrNull()
                if (sig != null) {
                    certSha256 = computeSha256(sig)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    tempProbeFile.delete()
                } catch (_: Exception) {
                }
            }

            val appInfo = packageInfo?.applicationInfo
            if (appInfo != null) {
                appInfo.sourceDir = targetBase.absolutePath
                appInfo.publicSourceDir = targetBase.absolutePath
            }

            val manifestPkg = manifestJson?.optString("package_name")?.takeIf { it.isNotBlank() }
            val resolvedPackageName = manifestPkg ?: packageInfo?.packageName

            if (resolvedPackageName.isNullOrBlank()) {
                stagingDir.deleteRecursively()
                return@withContext Result.failure(
                    Exception(context.getString(R.string.apk_invalid_package))
                )
            }

            val manifestLabel = manifestJson?.optString("name")?.takeIf { it.isNotBlank() }
            val resolvedLabel = manifestLabel
                ?: appInfo?.loadLabel(context.packageManager)?.toString()?.takeIf { it.isNotBlank() }
                ?: resolvedPackageName

            val resolvedIcon = rootIconDrawable ?: appInfo?.loadIcon(context.packageManager)

            val resolvedVersionName = manifestJson?.optString("version_name")?.takeIf { it.isNotBlank() }
                ?: packageInfo?.versionName
                ?: "1.0"

            val resolvedVersionCode = (manifestJson?.optString("version_code")?.toLongOrNull() ?: manifestJson?.optLong("version_code"))?.takeIf { it > 0 }
                ?: packageInfo?.let { PackageInfoCompat.getLongVersionCode(it) }
                ?: 1L

            val resolvedTargetSdk = (manifestJson?.optString("target_sdk_version")?.toIntOrNull() ?: manifestJson?.optInt("target_sdk_version"))?.takeIf { it > 0 }
                ?: appInfo?.targetSdkVersion
                ?: 0

            val resolvedMinSdk = (manifestJson?.optString("min_sdk_version")?.toIntOrNull() ?: manifestJson?.optInt("min_sdk_version"))?.takeIf { it > 0 }
                ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appInfo?.minSdkVersion ?: 0
                } else {
                    0
                }

            val requestedPermissions = mutableListOf<String>()
            packageInfo?.requestedPermissions?.let {
                requestedPermissions.addAll(it)
            }
            if (requestedPermissions.isEmpty() && manifestJson != null) {
                val permsArr = manifestJson.optJSONArray("permissions")
                if (permsArr != null) {
                    for (i in 0 until permsArr.length()) {
                        val p = permsArr.optString(i)
                        if (p.isNotBlank()) requestedPermissions.add(p)
                    }
                }
            }

            var installedAppInfo: InstalledAppInfo? = null
            var certMatch: Boolean? = null

            try {
                val installedPi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        resolvedPackageName,
                        PackageManager.PackageInfoFlags.of(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                (PackageManager.GET_SIGNING_CERTIFICATES).toLong()
                            } else {
                                @Suppress("DEPRECATION")
                                (PackageManager.GET_SIGNATURES).toLong()
                            }
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(
                        resolvedPackageName,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
                    )
                }

                val installedVersionCode = PackageInfoCompat.getLongVersionCode(installedPi)
                val installedVersionName = installedPi.versionName ?: "1.0"

                val updateType = when {
                    resolvedVersionCode > installedVersionCode -> UpdateType.UPDATE
                    resolvedVersionCode < installedVersionCode -> UpdateType.DOWNGRADE
                    else -> UpdateType.SAME_VERSION
                }

                val installedSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    installedPi.signingInfo?.apkContentsSigners
                } else {
                    @Suppress("DEPRECATION")
                    installedPi.signatures
                }
                val installedSig = installedSignatures?.firstOrNull()
                val installedCertSha256 = if (installedSig != null) computeSha256(installedSig) else null

                if (certSha256 != null && installedCertSha256 != null) {
                    certMatch = certSha256.equals(installedCertSha256, ignoreCase = true)
                }

                installedAppInfo = InstalledAppInfo(
                    versionName = installedVersionName,
                    versionCode = installedVersionCode,
                    updateType = updateType,
                    installedCertSha256 = installedCertSha256
                )
            } catch (_: PackageManager.NameNotFoundException) {
                installedAppInfo = InstalledAppInfo(
                    versionName = "",
                    versionCode = 0L,
                    updateType = UpdateType.NEW_INSTALL,
                    installedCertSha256 = null
                )
            } catch (_: Exception) {
            }

            if (manifestJson != null && obbFiles.isNotEmpty()) {
                val expansions = manifestJson.optJSONArray("expansions")
                if (expansions != null) {
                    for (i in 0 until expansions.length()) {
                        val exp = expansions.optJSONObject(i) ?: continue
                        val file = exp.optString("file")
                        val installPath = exp.optString("install_path")
                        if (file.isNotBlank() && installPath.isNotBlank()) {
                            val fName = File(file).name
                            val idx = obbFiles.indexOfFirst { File(it.first).name.equals(fName, ignoreCase = true) }
                            if (idx != -1) {
                                obbFiles[idx] = obbFiles[idx].first to installPath.trimStart('/')
                            }
                        }
                    }
                }
            }

            val totalSize = apkFiles.sumOf { it.length() } + obbFiles.sumOf { File(it.first).length() }
            val formattedSize = if (totalSize >= 1024 * 1024) {
                String.format(Locale.US, "%.1f MB", totalSize / (1024.0 * 1024.0))
            } else {
                String.format(Locale.US, "%.1f KB", totalSize / 1024.0)
            }

            Result.success(
                ApkPackageDetails(
                    label = resolvedLabel,
                    packageName = resolvedPackageName,
                    versionName = resolvedVersionName,
                    versionCode = resolvedVersionCode,
                    targetSdk = resolvedTargetSdk,
                    minSdk = resolvedMinSdk,
                    fileSize = totalSize,
                    formattedSize = formattedSize,
                    icon = resolvedIcon,
                    fileUri = uri,
                    fileName = fileName,
                    stagingDirPath = stagingDir.absolutePath,
                    isSplitPackage = isMultiApkArchive || apkFiles.size > 1,
                    splitCount = apkFiles.size,
                    hasObb = obbFiles.isNotEmpty(),
                    apkFilePaths = apkFiles.map { it.absolutePath },
                    obbFiles = obbFiles,
                    certSha256 = certSha256,
                    certMatch = certMatch,
                    requestedPermissions = requestedPermissions,
                    installedAppInfo = installedAppInfo
                )
            )
        } catch (e: Exception) {
            cleanStagingDir(stagingDir.absolutePath)
            Result.failure(e)
        }
    }

    suspend fun installPackage(
        context: Context,
        details: ApkPackageDetails,
        options: ApkInstallOptions
    ): Result<String> = withContext(Dispatchers.IO) {
        val stagingDir = File(details.stagingDirPath)
        if (!stagingDir.exists() || details.apkFilePaths.isEmpty()) {
            return@withContext Result.failure(Exception("Staging package files not found"))
        }

        val destDir = "/data/local/tmp/swiftsense_pkg_${System.currentTimeMillis()}"
        val flags = mutableListOf<String>()
        flags.add("-i com.android.vending")
        if (options.reinstall) flags.add("-r")
        if (options.allowDowngrade) flags.add("-d")
        if (options.allowTestOnly) flags.add("-t")
        if (options.grantRuntimePermissions) flags.add("-g")
        if (options.skipVerification) flags.add("--skip-verification")
        if (options.targetUserId != null) {
            flags.add("--user ${options.targetUserId}")
        }

        val baseFlags = flags.joinToString(" ")
        val bypassFlags = if (options.bypassLowSdkBlock) {
            (flags + "--bypass-low-target-sdk-block").joinToString(" ")
        } else baseFlags

        val copyApksCommands = details.apkFilePaths.joinToString(" ; ") { apkPath ->
            val fName = File(apkPath).name
            "cp '$apkPath' '$destDir/$fName' 2>/dev/null || cat '$apkPath' > '$destDir/$fName' 2>/dev/null"
        }

        val obbCommands = if (details.obbFiles.isNotEmpty()) {
            details.obbFiles.joinToString(" ; ") { (src, rel) ->
                val target = "/storage/emulated/0/$rel"
                "mkdir -p \"\$(dirname '$target')\" 2>/dev/null ; cp '$src' '$target' 2>/dev/null ; chmod 644 '$target' 2>/dev/null"
            }
        } else ""

        val script = buildString {
            append("mkdir -p '$destDir' ; ")
            append(copyApksCommands)
            append(" ; chmod 644 '$destDir'/*.apk 2>/dev/null ; ")
            append("(pm install $bypassFlags '$destDir'/*.apk || pm install $baseFlags '$destDir'/*.apk) ; ")
            append("RES=$? ; ")
            if (obbCommands.isNotBlank()) {
                append("if [ \$RES -eq 0 ]; then $obbCommands ; fi ; ")
            }
            append("rm -rf '$destDir' 2>/dev/null ; ")
            append("exit \$RES")
        }

        val runResult = ShellBridge.runAsync(script, timeoutMs = 120_000)

        cleanAllStagingDirs(context)

        if (runResult.isFailure) {
            val err = runResult.exceptionOrNull() ?: Exception("Execution error")
            saveHistoryItem(
                context,
                InstallHistoryItem(
                    packageName = details.packageName,
                    appName = details.label,
                    versionName = details.versionName,
                    timestamp = System.currentTimeMillis(),
                    isSuccess = false,
                    errorMessage = err.message
                )
            )
            return@withContext Result.failure(err)
        }

        val output = runResult.getOrDefault("")
        if (output.contains("Success", ignoreCase = true)) {
            saveHistoryItem(
                context,
                InstallHistoryItem(
                    packageName = details.packageName,
                    appName = details.label,
                    versionName = details.versionName,
                    timestamp = System.currentTimeMillis(),
                    isSuccess = true
                )
            )
            Result.success(output.trim())
        } else if (output.contains("Failure [", ignoreCase = true)) {
            val errReason = output.substringAfter("Failure [").substringBefore("]")
            val reason = errReason.ifBlank { output }
            saveHistoryItem(
                context,
                InstallHistoryItem(
                    packageName = details.packageName,
                    appName = details.label,
                    versionName = details.versionName,
                    timestamp = System.currentTimeMillis(),
                    isSuccess = false,
                    errorMessage = reason
                )
            )
            Result.failure(Exception(reason))
        } else if (output.contains("Failure", ignoreCase = true)) {
            saveHistoryItem(
                context,
                InstallHistoryItem(
                    packageName = details.packageName,
                    appName = details.label,
                    versionName = details.versionName,
                    timestamp = System.currentTimeMillis(),
                    isSuccess = false,
                    errorMessage = output.trim()
                )
            )
            Result.failure(Exception(output.trim()))
        } else {
            saveHistoryItem(
                context,
                InstallHistoryItem(
                    packageName = details.packageName,
                    appName = details.label,
                    versionName = details.versionName,
                    timestamp = System.currentTimeMillis(),
                    isSuccess = true
                )
            )
            Result.success(output.trim().ifEmpty { "Success" })
        }
    }

    fun cleanStagingDir(dirPath: String?) {
        if (dirPath.isNullOrBlank()) return
        try {
            val f = File(dirPath)
            if (f.exists()) f.deleteRecursively()
        } catch (_: Exception) {
        }
    }

    fun cleanAllStagingDirs(context: Context) {
        val cacheDirs = listOfNotNull(context.externalCacheDir, context.cacheDir)
        for (dir in cacheDirs) {
            try {
                dir.listFiles()?.forEach { file ->
                    if (file.isDirectory && file.name.startsWith("pkg_staging_")) {
                        file.deleteRecursively()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun getHistoryItems(context: Context): List<InstallHistoryItem> {
        val prefs = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_HISTORY_ITEMS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<InstallHistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    InstallHistoryItem(
                        packageName = obj.optString("packageName"),
                        appName = obj.optString("appName"),
                        versionName = obj.optString("versionName"),
                        timestamp = obj.optLong("timestamp"),
                        isSuccess = obj.optBoolean("isSuccess"),
                        errorMessage = obj.optString("errorMessage").takeIf { it.isNotBlank() }
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveHistoryItem(context: Context, item: InstallHistoryItem) {
        val items = getHistoryItems(context).toMutableList()
        items.add(0, item)
        val capped = items.take(50)
        try {
            val arr = JSONArray()
            for (it in capped) {
                val obj = JSONObject().apply {
                    put("packageName", it.packageName)
                    put("appName", it.appName)
                    put("versionName", it.versionName)
                    put("timestamp", it.timestamp)
                    put("isSuccess", it.isSuccess)
                    put("errorMessage", it.errorMessage ?: "")
                }
                arr.put(obj)
            }
            val prefs = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_HISTORY_ITEMS, arr.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY_ITEMS).apply()
    }
}

