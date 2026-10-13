package io.github.itsmelissadev.swiftsense.feature.updates

data class GitHubRelease(
    val id: Long,
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val htmlUrl: String,
    val isPrerelease: Boolean,
    val apkAsset: GitHubAsset?
)

data class GitHubAsset(
    val id: Long,
    val name: String,
    val size: Long,
    val downloadUrl: String,
    val downloadCount: Int
)
