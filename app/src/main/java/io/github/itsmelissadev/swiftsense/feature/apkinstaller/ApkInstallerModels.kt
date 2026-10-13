package io.github.itsmelissadev.swiftsense.feature.apkinstaller

import android.graphics.drawable.Drawable
import android.net.Uri

enum class UpdateType {
    NEW_INSTALL,
    UPDATE,
    DOWNGRADE,
    SAME_VERSION
}

data class InstalledAppInfo(
    val versionName: String,
    val versionCode: Long,
    val updateType: UpdateType,
    val installedCertSha256: String? = null
)

data class ApkPackageDetails(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val fileSize: Long,
    val formattedSize: String,
    val icon: Drawable?,
    val fileUri: Uri,
    val fileName: String,
    val stagingDirPath: String,
    val isSplitPackage: Boolean = false,
    val splitCount: Int = 1,
    val hasObb: Boolean = false,
    val apkFilePaths: List<String> = emptyList(),
    val obbFiles: List<Pair<String, String>> = emptyList(),
    val certSha256: String? = null,
    val certMatch: Boolean? = null,
    val requestedPermissions: List<String> = emptyList(),
    val installedAppInfo: InstalledAppInfo? = null
)

data class ApkInstallOptions(
    val reinstall: Boolean = true,
    val allowDowngrade: Boolean = true,
    val bypassLowSdkBlock: Boolean = true,
    val grantRuntimePermissions: Boolean = false,
    val allowTestOnly: Boolean = true,
    val skipVerification: Boolean = false,
    val targetUserId: Int? = null
)

data class InstallHistoryItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val timestamp: Long,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

sealed interface ApkInstallState {
    data object Idle : ApkInstallState
    data class Parsing(val message: String) : ApkInstallState
    data class Ready(val details: ApkPackageDetails) : ApkInstallState
    data class Installing(val details: ApkPackageDetails, val step: String) : ApkInstallState
    data class Success(val details: ApkPackageDetails, val output: String) : ApkInstallState
    data class Error(val details: ApkPackageDetails?, val message: String) : ApkInstallState
}

