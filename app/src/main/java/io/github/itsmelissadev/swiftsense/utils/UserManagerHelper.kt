package io.github.itsmelissadev.swiftsense.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.feature.appmanager.RichAppInfo
import io.github.itsmelissadev.swiftsense.service.shell.ShellBridge

data class AndroidUserInfo(
    val id: Int,
    val name: String,
    val isRunning: Boolean = true,
    val isCurrentProcess: Boolean = false
)

object UserManagerHelper {

    fun getCurrentProcessUserId(): Int {
        return Process.myUid() / 100000
    }

    fun getInitialUserProfiles(context: Context): List<AndroidUserInfo> {
        val currentUserId = getCurrentProcessUserId()
        val userManager = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager
        if (userManager != null) {
            try {
                val profiles = userManager.userProfiles
                if (!profiles.isNullOrEmpty()) {
                    return profiles.map { userHandle ->
                        val id = try {
                            val method = userHandle.javaClass.getMethod("getIdentifier")
                            method.invoke(userHandle) as Int
                        } catch (_: Exception) {
                            userHandle.hashCode()
                        }
                        val isCurrent = (id == currentUserId)
                        AndroidUserInfo(
                            id = id,
                            name = if (id == 0) context.getString(R.string.user_profile_main) else context.getString(R.string.user_profile_secondary, id),
                            isRunning = true,
                            isCurrentProcess = isCurrent
                        )
                    }
                }
            } catch (_: Exception) {}
        }
        return listOf(createFallbackUser(context, currentUserId, true))
    }

    fun getUserProfiles(context: Context): List<AndroidUserInfo> {
        val currentUserId = getCurrentProcessUserId()
        var output = ShellBridge.run("pm list users").getOrNull().orEmpty()
        if (output.isBlank() || !output.contains("UserInfo")) {
            output = ShellBridge.run("cmd user list").getOrNull().orEmpty()
        }
        if (output.isBlank() || !output.contains("UserInfo")) {
            output = ShellBridge.run("dumpsys user").getOrNull().orEmpty()
        }

        if (output.isBlank() || !output.contains("UserInfo")) {
            return getInitialUserProfiles(context)
        }

        val regex = Regex("""UserInfo\{(\d+):([^:]*):([0-9a-fA-F]+)\}""")
        val parsedUsers = output.lineSequence().mapNotNull { line ->
            val match = regex.find(line) ?: return@mapNotNull null
            val id = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val rawName = match.groupValues[2].trim()
            val isRunning = line.contains("running") || (id == currentUserId)
            val isCurrent = (id == currentUserId)
            val displayName = resolveUserDisplayName(context, id, rawName)

            AndroidUserInfo(
                id = id,
                name = displayName,
                isRunning = isRunning,
                isCurrentProcess = isCurrent
            )
        }.distinctBy { it.id }.toList()

        return parsedUsers.ifEmpty { getInitialUserProfiles(context) }
    }

    private fun resolveUserDisplayName(context: Context, id: Int, rawName: String): String {
        val cleanName = rawName.trim()
        if (cleanName.isNotBlank() && !cleanName.equals("null", ignoreCase = true)) {
            return cleanName
        }
        return when (id) {
            0 -> context.getString(R.string.user_profile_main)
            999 -> context.getString(R.string.user_profile_dual_app)
            else -> context.getString(R.string.user_profile_secondary, id)
        }
    }

    private fun createFallbackUser(context: Context, id: Int, isCurrent: Boolean): AndroidUserInfo {
        return AndroidUserInfo(
            id = id,
            name = if (id == 0) context.getString(R.string.user_profile_main) else context.getString(R.string.user_profile_secondary, id),
            isRunning = true,
            isCurrentProcess = isCurrent
        )
    }

    fun getInstalledPackagesForUser(userId: Int): List<String> {
        val result = ShellBridge.run("pm list packages --user $userId")
        if (result.isFailure) return emptyList()
        val output = result.getOrNull().orEmpty()
        return output.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:").trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    fun getDisabledPackagesForUser(userId: Int): Set<String> {
        val result = ShellBridge.run("pm list packages -d --user $userId")
        if (result.isFailure) return emptySet()
        val output = result.getOrNull().orEmpty()
        return output.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:").trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun loadRichAppsForUser(
        context: Context,
        targetUserId: Int
    ): List<RichAppInfo> {
        val pm = context.packageManager
        val currentUserId = getCurrentProcessUserId()

        if (targetUserId == currentUserId) {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA or PackageManager.MATCH_DISABLED_COMPONENTS)
            return installedApps
                .filter { it.packageName != context.packageName && !it.packageName.contains("shizuku", ignoreCase = true) }
                .map { app ->
                    var vName = ""
                    var vCode = 0L
                    try {
                        val pInfo = pm.getPackageInfo(app.packageName, 0)
                        vName = pInfo.versionName ?: ""
                        vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            pInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            pInfo.versionCode.toLong()
                        }
                    } catch (_: Exception) {}

                    val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        app.minSdkVersion
                    } else 0

                    RichAppInfo(
                        packageName = app.packageName,
                        label = app.loadLabel(pm).toString(),
                        isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        isEnabled = app.enabled,
                        appInfo = app,
                        versionName = vName,
                        versionCode = vCode,
                        targetSdkVersion = app.targetSdkVersion,
                        minSdkVersion = minSdk
                    )
                }.sortedBy { it.label.lowercase() }
        }

        val packageList = getInstalledPackagesForUser(targetUserId)
            .filter { it != context.packageName && !it.contains("shizuku", ignoreCase = true) }
        val disabledSet = getDisabledPackagesForUser(targetUserId)

        return packageList.mapNotNull { pkgName ->
            try {
                val appInfo = try {
                    pm.getApplicationInfo(pkgName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
                } catch (_: Exception) {
                    pm.getApplicationInfo(pkgName, 0)
                }

                var vName = ""
                var vCode = 0L
                try {
                    val pInfo = pm.getPackageInfo(pkgName, 0)
                    vName = pInfo.versionName ?: ""
                    vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        pInfo.versionCode.toLong()
                    }
                } catch (_: Exception) {}

                val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appInfo.minSdkVersion
                } else 0

                RichAppInfo(
                    packageName = pkgName,
                    label = appInfo.loadLabel(pm).toString(),
                    isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isEnabled = !disabledSet.contains(pkgName),
                    appInfo = appInfo,
                    versionName = vName,
                    versionCode = vCode,
                    targetSdkVersion = appInfo.targetSdkVersion,
                    minSdkVersion = minSdk
                )
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.label.lowercase() }
    }
}
