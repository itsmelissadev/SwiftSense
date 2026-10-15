package io.github.itsmelissadev.swiftsense.feature.downloader

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.feature.downloader.engine.DownloadSegment
import io.github.itsmelissadev.swiftsense.feature.downloader.engine.DownloadStatus
import io.github.itsmelissadev.swiftsense.feature.downloader.engine.DownloadTask
import io.github.itsmelissadev.swiftsense.feature.downloader.engine.FileCategory
import io.github.itsmelissadev.swiftsense.feature.downloader.engine.TurboDownloadEngine
import io.github.itsmelissadev.swiftsense.feature.downloader.service.TurboDownloadService
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveHeader
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveIconBadge
import io.github.itsmelissadev.swiftsense.ui.theme.ExpressivePalette
import io.github.itsmelissadev.swiftsense.utils.FormatUtils
import java.io.File

@Composable
fun TurboDownloaderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToApkInstaller: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val tasks by TurboDownloadEngine.tasks.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    var selectedThreadCount by remember { mutableIntStateOf(8) }

    val activeTasks = remember(tasks) {
        tasks.filter { it.status != DownloadStatus.COMPLETED }
    }
    val completedTasks = remember(tasks) {
        tasks.filter { it.status == DownloadStatus.COMPLETED }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ExpressiveHeader(
                title = stringResource(R.string.feature_turbo_downloader),
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NewDownloadCard(
                    url = urlInput,
                    onUrlChange = { urlInput = it },
                    threadCount = selectedThreadCount,
                    onThreadCountChange = { selectedThreadCount = it },
                    onPasteClipboard = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
                            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                            if (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) {
                                urlInput = text
                            }
                        }
                    },
                    onStartDownload = {
                        val trimmed = urlInput.trim()
                        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                            TurboDownloadEngine.startDownload(
                                context = context,
                                url = trimmed,
                                threadCount = selectedThreadCount
                            )
                            TurboDownloadService.startService(context)
                            urlInput = ""
                            keyboardController?.hide()
                        }
                    }
                )
            }

            if (activeTasks.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.downloader_active_downloads),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                    )
                }

                items(activeTasks, key = { it.id }) { task ->
                    ActiveDownloadItem(
                        task = task,
                        onPause = { TurboDownloadEngine.pauseDownload(task.id) },
                        onResume = {
                            TurboDownloadEngine.resumeDownload(context, task.id)
                            TurboDownloadService.startService(context)
                        },
                        onCancel = { TurboDownloadEngine.cancelDownload(task.id) }
                    )
                }
            }

            if (completedTasks.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.downloader_completed_downloads),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                    )
                }

                items(completedTasks, key = { it.id }) { task ->
                    CompletedDownloadItem(
                        task = task,
                        onOpen = {
                            if (task.filePath != null) {
                                openDownloadedFile(context, File(task.filePath))
                            }
                        },
                        onInstallApk = {
                            if (task.filePath != null && onNavigateToApkInstaller != null) {
                                val file = File(task.filePath)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                onNavigateToApkInstaller(uri)
                            }
                        },
                        onDelete = { TurboDownloadEngine.cancelDownload(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NewDownloadCard(
    url: String,
    onUrlChange: (String) -> Unit,
    threadCount: Int,
    onThreadCountChange: (Int) -> Unit,
    onPasteClipboard: () -> Unit,
    onStartDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.downloader_url_hint)) },
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (url.isEmpty()) {
                        IconButton(onClick = onPasteClipboard) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null)
                        }
                    } else {
                        IconButton(onClick = { onUrlChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onStartDownload() })
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.downloader_threads),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.downloader_threads_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(4, 8, 16).forEach { threads ->
                        FilterChip(
                            selected = threadCount == threads,
                            onClick = { onThreadCountChange(threads) },
                            label = { Text(stringResource(R.string.downloader_threads_count, threads)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            ExpressiveButton(
                text = stringResource(R.string.downloader_action_download),
                icon = Icons.Default.Download,
                enabled = url.isNotBlank(),
                onClick = onStartDownload,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ActiveDownloadItem(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveIconBadge(
                    icon = getCategoryIcon(task.category),
                    colors = ExpressivePalette.TealLight
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${stringResource(getCategoryLabel(task.category))} • ${stringResource(R.string.downloader_threads_count, task.threadCount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    if (task.status == DownloadStatus.DOWNLOADING) {
                        IconButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    } else if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.FAILED) {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getStatusText(task.status),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (task.status == DownloadStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (task.status == DownloadStatus.DOWNLOADING && task.speedBytesPerSec > 0) {
                        Text(
                            text = "• ${FormatUtils.formatSpeed(task.speedBytesPerSec)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (task.fileSize > 0) {
                    Text(
                        text = "${FormatUtils.formatBytes(task.downloadedBytes)} / ${FormatUtils.formatBytes(task.fileSize)} (${(task.progress * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (task.downloadedBytes > 0) {
                    Text(
                        text = FormatUtils.formatBytes(task.downloadedBytes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (task.segments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.downloader_segments_label, task.segments.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        task.segments.forEach { segment ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(segment.progress)
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedDownloadItem(
    task: DownloadTask,
    onOpen: () -> Unit,
    onInstallApk: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveIconBadge(
                icon = getCategoryIcon(task.category),
                colors = ExpressivePalette.GreenLight
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${stringResource(R.string.downloader_status_completed)} • ${FormatUtils.formatBytes(task.fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                if (task.category == FileCategory.APK) {
                    IconButton(onClick = onInstallApk) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                IconButton(onClick = onOpen) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun openDownloadedFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, file.name, Toast.LENGTH_SHORT).show()
    }
}

private fun getCategoryIcon(category: FileCategory): ImageVector {
    return when (category) {
        FileCategory.VIDEO -> Icons.Default.Videocam
        FileCategory.AUDIO -> Icons.Default.AudioFile
        FileCategory.APK -> Icons.Default.InstallMobile
        FileCategory.ARCHIVE -> Icons.Default.FolderZip
        FileCategory.DOCUMENT -> Icons.Default.Description
        FileCategory.OTHER -> Icons.Default.Download
    }
}

@Composable
private fun getStatusText(status: DownloadStatus): String {
    return when (status) {
        DownloadStatus.QUEUED -> stringResource(R.string.downloader_status_connecting)
        DownloadStatus.CONNECTING -> stringResource(R.string.downloader_status_connecting)
        DownloadStatus.DOWNLOADING -> stringResource(R.string.downloader_status_downloading)
        DownloadStatus.PAUSED -> stringResource(R.string.downloader_status_paused)
        DownloadStatus.COMPLETED -> stringResource(R.string.downloader_status_completed)
        DownloadStatus.FAILED -> stringResource(R.string.downloader_status_failed)
    }
}

private fun getCategoryLabel(category: FileCategory): Int {
    return when (category) {
        FileCategory.VIDEO -> R.string.downloader_cat_video
        FileCategory.AUDIO -> R.string.downloader_cat_audio
        FileCategory.APK -> R.string.downloader_cat_apk
        FileCategory.ARCHIVE -> R.string.downloader_cat_archive
        FileCategory.DOCUMENT -> R.string.downloader_cat_document
        FileCategory.OTHER -> R.string.downloader_cat_file
    }
}

