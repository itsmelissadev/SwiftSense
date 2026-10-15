package io.github.itsmelissadev.swiftsense.feature.updates

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.itsmelissadev.swiftsense.BuildConfig
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveCardGroup
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdatesUiState {
    data object Loading : UpdatesUiState
    data class Success(val releases: List<GitHubRelease>) : UpdatesUiState
    data class Error(val message: String) : UpdatesUiState
}

@Composable
fun AppUpdatesScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf<UpdatesUiState>(UpdatesUiState.Loading) }
    val expandedReleases = remember { mutableStateMapOf<Long, Boolean>() }
    val activeDownloads = remember { mutableStateMapOf<Long, Long>() }
    val downloadProgress = remember { mutableStateMapOf<Long, Int>() }
    val downloadedFiles = remember { mutableStateMapOf<Long, File>() }

    fun loadReleases() {
        uiState = UpdatesUiState.Loading
        scope.launch {
            val result = UpdateManager.fetchReleases()
            uiState = if (result.isSuccess) {
                val list = result.getOrThrow()
                if (list.isNotEmpty()) {
                    expandedReleases[list.first().id] = true
                }
                UpdatesUiState.Success(list)
            } else {
                UpdatesUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Unknown error")
            }
        }
    }

    LaunchedEffect(Unit) {
        loadReleases()
    }

    LaunchedEffect(activeDownloads.toMap()) {
        while (activeDownloads.isNotEmpty()) {
            val completed = mutableListOf<Long>()
            for ((releaseId, downloadId) in activeDownloads) {
                val prog = UpdateManager.getDownloadProgress(context, downloadId)
                if (prog >= 100) {
                    val file = UpdateManager.getDownloadedFile(context, downloadId)
                    if (file != null && file.exists()) {
                        downloadedFiles[releaseId] = file
                    }
                    completed.add(releaseId)
                } else if (prog == -2) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.updates_download_failed, "Failed"),
                        Toast.LENGTH_SHORT
                    ).show()
                    completed.add(releaseId)
                } else if (prog >= 0) {
                    downloadProgress[releaseId] = prog
                }
            }
            completed.forEach {
                activeDownloads.remove(it)
                downloadProgress.remove(it)
            }
            delay(1000)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ExpressiveHeader(
                title = stringResource(R.string.updates_screen_title),
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { loadReleases() }) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CurrentVersionCard()
            }

            when (val state = uiState) {
                is UpdatesUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = stringResource(R.string.updates_loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                is UpdatesUiState.Error -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.updates_error, state.message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            ExpressiveButton(
                                text = stringResource(R.string.updates_retry),
                                onClick = { loadReleases() },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
                is UpdatesUiState.Success -> {
                    if (state.releases.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.updates_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        itemsIndexed(state.releases) { index, release ->
                            val isExpanded = expandedReleases[release.id] ?: (index == 0)
                            val isDownloading = activeDownloads.containsKey(release.id)
                            val progress = downloadProgress[release.id] ?: 0
                            val downloadedApk = downloadedFiles[release.id]

                            ReleaseCard(
                                release = release,
                                isLatest = (index == 0),
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedReleases[release.id] = !isExpanded
                                },
                                isDownloading = isDownloading,
                                downloadProgress = progress,
                                downloadedFile = downloadedApk,
                                onDownloadApk = {
                                    val asset = release.apkAsset
                                    if (asset != null) {
                                        val downloadId = UpdateManager.enqueueDownload(
                                            context = context,
                                            downloadUrl = asset.downloadUrl,
                                            fileName = "SwiftSense-${release.tagName}.apk",
                                            title = "SwiftSense ${release.tagName}"
                                        )
                                        activeDownloads[release.id] = downloadId
                                        Toast.makeText(
                                            context,
                                            R.string.updates_download_started,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onInstallApk = { apkFile ->
                                    UpdateManager.launchApkInstaller(context, apkFile)
                                },
                                onOpenGithub = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CurrentVersionCard() {
    ExpressiveCardGroup {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.updates_current_version),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseCard(
    release: GitHubRelease,
    isLatest: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    isDownloading: Boolean,
    downloadProgress: Int,
    downloadedFile: File?,
    onDownloadApk: () -> Unit,
    onInstallApk: (File) -> Unit,
    onOpenGithub: () -> Unit
) {
    ExpressiveCardGroup {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = release.name.ifEmpty { release.tagName },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isLatest) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.updates_latest_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (release.isPrerelease) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.updates_prerelease_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (release.publishedAt.isNotEmpty()) {
                            val dateText = release.publishedAt.take(10)
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Text(
                            text = stringResource(R.string.updates_release_notes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (release.body.isNotBlank()) {
                            MarkdownContent(
                                markdown = release.body,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.updates_no_release_notes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        val apkAsset = release.apkAsset
                        if (apkAsset != null) {
                            val apkSizeText = UpdateManager.formatFileSize(apkAsset.size)
                            if (downloadedFile != null && downloadedFile.exists()) {
                                ExpressiveButton(
                                    text = stringResource(R.string.updates_action_install),
                                    onClick = { onInstallApk(downloadedFile) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else if (isDownloading) {
                                OutlinedButton(
                                    onClick = { },
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.updates_action_downloading, downloadProgress),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                ExpressiveButton(
                                    text = stringResource(R.string.updates_action_download_apk, apkSizeText),
                                    onClick = onDownloadApk,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.updates_no_apk),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenGithub,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.updates_action_view_github),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
