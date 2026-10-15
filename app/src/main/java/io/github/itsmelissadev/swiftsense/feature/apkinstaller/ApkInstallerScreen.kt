package io.github.itsmelissadev.swiftsense.feature.apkinstaller

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.service.shell.ShellBridge
import io.github.itsmelissadev.swiftsense.ui.components.ExecutionStatusWidget
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveCardGroup
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveHeader
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveOutlinedButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSwitchPreference
import io.github.itsmelissadev.swiftsense.ui.theme.ExpressivePalette
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ApkInstallerScreen(
    initialUri: Uri? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isShellReady by ShellBridge.ready.collectAsState()

    var installState by remember { mutableStateOf<ApkInstallState>(ApkInstallState.Idle) }
    var options by remember { mutableStateOf(ApkInstallOptions()) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var pendingSecurityRisk by remember { mutableStateOf<ApkSecurityRisk?>(null) }
    var historyItems by remember { mutableStateOf(emptyList<InstallHistoryItem>()) }

    var batchQueue by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var batchTotal by remember { mutableStateOf(0) }
    var batchCurrentIndex by remember { mutableStateOf(0) }

    fun processUri(uri: Uri) {
        ApkInstallerHelper.cleanAllStagingDirs(context)
        installState = ApkInstallState.Parsing(context.getString(R.string.apk_parsing))
        scope.launch {
            val parseRes = ApkInstallerHelper.parsePackage(context, uri)
            if (parseRes.isSuccess) {
                installState = ApkInstallState.Ready(parseRes.getOrThrow())
            } else {
                val err = parseRes.exceptionOrNull()?.message ?: context.getString(R.string.apk_parse_error)
                installState = ApkInstallState.Error(null, err)
            }
        }
    }

    fun startBatch(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (uris.size == 1) {
            processUri(uris.first())
            return
        }
        batchQueue = uris
        batchTotal = uris.size
        batchCurrentIndex = 0
        processUri(uris.first())
    }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            processUri(initialUri)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ApkInstallerHelper.cleanAllStagingDirs(context)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            startBatch(uris)
        }
    }

    if (showPermissionsDialog) {
        val readyDetails = (installState as? ApkInstallState.Ready)?.details
        val permissions = readyDetails?.requestedPermissions ?: emptyList()
        AlertDialog(
            onDismissRequest = { showPermissionsDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.apk_permissions_title, permissions.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(permissions) { perm ->
                        val simplePerm = perm.substringAfterLast('.')
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = simplePerm,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = perm,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPermissionsDialog = false }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.apk_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (historyItems.isNotEmpty()) {
                        IconButton(onClick = {
                            ApkInstallerHelper.clearHistory(context)
                            historyItems = emptyList()
                        }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            text = {
                if (historyItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.apk_history_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyItems) { item ->
                            val dateStr = remember(item.timestamp) {
                                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (item.isSuccess) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (item.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (item.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.appName.ifBlank { item.packageName },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${item.versionName} • $dateStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!item.errorMessage.isNullOrBlank()) {
                                            Text(
                                                text = item.errorMessage,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }

    if (pendingSecurityRisk != null) {
        ApkSecurityWarningDialog(
            risk = pendingSecurityRisk!!,
            onConfirm = {
                val r = pendingSecurityRisk
                pendingSecurityRisk = null
                if (r == ApkSecurityRisk.GRANT_PERMISSIONS) {
                    options = options.copy(grantRuntimePermissions = true)
                } else if (r == ApkSecurityRisk.SKIP_VERIFICATION) {
                    options = options.copy(skipVerification = true)
                }
            },
            onDismiss = { pendingSecurityRisk = null }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ExpressiveHeader(
                title = stringResource(R.string.feature_apk_installer),
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = {
                        historyItems = ApkInstallerHelper.getHistoryItems(context)
                        showHistoryDialog = true
                    }) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = stringResource(R.string.apk_history_title),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                ExecutionStatusWidget(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (batchTotal > 1) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.apk_batch_title, batchCurrentIndex + 1, batchTotal),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            when (val state = installState) {
                is ApkInstallState.Idle -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Text(
                                    text = stringResource(R.string.apk_select_file),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = stringResource(R.string.apk_select_file_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                ExpressiveButton(
                                    text = stringResource(R.string.apk_select_button),
                                    onClick = {
                                        filePickerLauncher.launch(
                                            arrayOf(
                                                "application/vnd.android.package-archive",
                                                "application/octet-stream",
                                                "application/zip",
                                                "application/x-zip-compressed",
                                                "*/*"
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Default.FolderOpen
                                )
                            }
                        }
                    }
                }

                is ApkInstallState.Parsing -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                is ApkInstallState.Ready,
                is ApkInstallState.Installing,
                is ApkInstallState.Success,
                is ApkInstallState.Error -> {
                    val details = when (state) {
                        is ApkInstallState.Ready -> state.details
                        is ApkInstallState.Installing -> state.details
                        is ApkInstallState.Success -> state.details
                        is ApkInstallState.Error -> state.details
                    }

                    if (details != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val iconBitmap = remember(details.icon) {
                                            try {
                                                details.icon?.toBitmap(96, 96)?.asImageBitmap()
                                            } catch (_: Exception) {
                                                null
                                            }
                                        }

                                        if (iconBitmap != null) {
                                            Image(
                                                bitmap = iconBitmap,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Android,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = details.label,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = details.packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${details.versionName} (${details.versionCode})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    details.installedAppInfo?.let { installed ->
                                        val (statusText, statusBg, statusColor) = when (installed.updateType) {
                                            UpdateType.NEW_INSTALL -> Triple(
                                                stringResource(R.string.apk_status_new),
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            UpdateType.UPDATE -> Triple(
                                                stringResource(R.string.apk_status_update, installed.versionName),
                                                MaterialTheme.colorScheme.tertiaryContainer,
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            UpdateType.DOWNGRADE -> Triple(
                                                stringResource(R.string.apk_status_downgrade, installed.versionName),
                                                MaterialTheme.colorScheme.errorContainer,
                                                MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            UpdateType.SAME_VERSION -> Triple(
                                                stringResource(R.string.apk_status_same, installed.versionName),
                                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = statusBg,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.SystemUpdate,
                                                    contentDescription = null,
                                                    tint = statusColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = statusText,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = stringResource(R.string.apk_file_size, details.formattedSize),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = stringResource(R.string.apk_target_sdk, details.targetSdk),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (details.isSplitPackage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (details.isSplitPackage) {
                                                    stringResource(R.string.apk_package_type_split, details.splitCount)
                                                } else {
                                                    stringResource(R.string.apk_package_type_single)
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = if (details.isSplitPackage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }

                                        if (details.hasObb) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.apk_package_obb_badge),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (details.certMatch != null) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (details.certMatch == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    if (details.certMatch == true) Icons.Default.VerifiedUser else Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = if (details.certMatch == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (details.certMatch == true) stringResource(R.string.apk_signature_match) else stringResource(R.string.apk_signature_mismatch),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (details.certMatch == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }

                                    if (details.requestedPermissions.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showPermissionsDialog = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Shield,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = stringResource(R.string.apk_permissions_title, details.requestedPermissions.size),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    text = stringResource(R.string.apk_view_permissions),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    if (state !is ApkInstallState.Installing) {
                                        ExpressiveOutlinedButton(
                                            text = stringResource(R.string.apk_action_install_another),
                                            onClick = {
                                                filePickerLauncher.launch(
                                                    arrayOf(
                                                        "application/vnd.android.package-archive",
                                                        "application/octet-stream",
                                                        "application/zip",
                                                        "application/x-zip-compressed",
                                                        "*/*"
                                                    )
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            icon = Icons.Default.FolderOpen
                                        )
                                    }
                                }
                            }
                        }

                        if (state is ApkInstallState.Ready) {
                            item {
                                Text(
                                    text = stringResource(R.string.category_tools),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                                )

                                ExpressiveCardGroup {
                                    item {
                                        ExpressiveSwitchPreference(
                                            title = stringResource(R.string.apk_reinstall_option),
                                            description = stringResource(R.string.apk_reinstall_desc),
                                            icon = Icons.Default.Refresh,
                                            badgeColors = ExpressivePalette.BlueLight,
                                            checked = options.reinstall,
                                            onCheckedChange = { options = options.copy(reinstall = it) }
                                        )
                                    }

                                    item {
                                        ExpressiveSwitchPreference(
                                            title = stringResource(R.string.apk_downgrade_option),
                                            description = stringResource(R.string.apk_downgrade_desc),
                                            icon = Icons.Default.SystemUpdate,
                                            badgeColors = ExpressivePalette.AmberLight,
                                            checked = options.allowDowngrade,
                                            onCheckedChange = { options = options.copy(allowDowngrade = it) }
                                        )
                                    }

                                    item {
                                        ExpressiveSwitchPreference(
                                            title = stringResource(R.string.apk_bypass_sdk_option),
                                            description = stringResource(R.string.apk_bypass_sdk_desc),
                                            icon = Icons.Default.Security,
                                            badgeColors = ExpressivePalette.PurpleLight,
                                            checked = options.bypassLowSdkBlock,
                                            onCheckedChange = { options = options.copy(bypassLowSdkBlock = it) }
                                        )
                                    }

                                    item {
                                        ExpressiveSwitchPreference(
                                            title = stringResource(R.string.apk_grant_perms_option),
                                            description = stringResource(R.string.apk_grant_perms_desc),
                                            icon = Icons.Default.CheckCircle,
                                            badgeColors = ExpressivePalette.GreenLight,
                                            checked = options.grantRuntimePermissions,
                                            onCheckedChange = { enable ->
                                                if (enable) {
                                                    pendingSecurityRisk = ApkSecurityRisk.GRANT_PERMISSIONS
                                                } else {
                                                    options = options.copy(grantRuntimePermissions = false)
                                                }
                                            }
                                        )
                                    }

                                    item {
                                        ExpressiveSwitchPreference(
                                            title = stringResource(R.string.apk_test_only_option),
                                            description = stringResource(R.string.apk_test_only_desc),
                                            icon = Icons.Default.BugReport,
                                            badgeColors = ExpressivePalette.AmberLight,
                                            checked = options.allowTestOnly,
                                            onCheckedChange = { options = options.copy(allowTestOnly = it) }
                                        )
                                    }

                                    item {
                                        ExpressiveSwitchPreference(
                                            title = stringResource(R.string.apk_skip_verification_option),
                                            description = stringResource(R.string.apk_skip_verification_desc),
                                            icon = Icons.Default.LockOpen,
                                            badgeColors = ExpressivePalette.PinkLight,
                                            checked = options.skipVerification,
                                            onCheckedChange = { enable ->
                                                if (enable) {
                                                    pendingSecurityRisk = ApkSecurityRisk.SKIP_VERIFICATION
                                                } else {
                                                    options = options.copy(skipVerification = false)
                                                }
                                            }
                                        )
                                    }

                                    item {
                                        ExpressiveSwitchPreference(
                                            title = stringResource(R.string.apk_user_profile_option),
                                            description = stringResource(R.string.apk_user_profile_desc),
                                            icon = Icons.Default.People,
                                            badgeColors = ExpressivePalette.BlueLight,
                                            checked = options.targetUserId == null,
                                            onCheckedChange = { installForAll ->
                                                options = options.copy(targetUserId = if (installForAll) null else 0)
                                            }
                                        )
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                ExpressiveButton(
                                    text = stringResource(R.string.apk_action_install),
                                    enabled = isShellReady,
                                    onClick = {
                                        installState = ApkInstallState.Installing(
                                            details,
                                            context.getString(R.string.apk_install_step_exec)
                                        )
                                        scope.launch {
                                            val result = ApkInstallerHelper.installPackage(context, details, options)
                                            if (result.isSuccess) {
                                                installState = ApkInstallState.Success(details, result.getOrThrow())
                                            } else {
                                                val rawErr = result.exceptionOrNull()?.message
                                                val msg = ApkInstallerHelper.getLocalizedInstallError(context, rawErr)
                                                installState = ApkInstallState.Error(details, msg)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Default.Download
                                )

                                if (!isShellReady) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.apk_service_not_ready_warning),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }

                        if (state is ApkInstallState.Installing) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(24.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(36.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.apk_installing),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = state.step,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (state is ApkInstallState.Success) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }

                                        Text(
                                            text = stringResource(R.string.apk_install_success_title),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = stringResource(R.string.apk_install_success_desc),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            val launchIntent = remember(details.packageName) {
                                                context.packageManager.getLaunchIntentForPackage(details.packageName)
                                            }

                                            if (launchIntent != null) {
                                                ExpressiveButton(
                                                    text = stringResource(R.string.apk_action_open_app),
                                                    onClick = {
                                                        try {
                                                            context.startActivity(launchIntent)
                                                        } catch (_: Exception) {
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    icon = Icons.AutoMirrored.Filled.Launch
                                                )
                                            }

                                            if (batchCurrentIndex + 1 < batchTotal) {
                                                ExpressiveButton(
                                                    text = stringResource(R.string.next),
                                                    onClick = {
                                                        batchCurrentIndex++
                                                        processUri(batchQueue[batchCurrentIndex])
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else {
                                                ExpressiveOutlinedButton(
                                                    text = stringResource(R.string.apk_action_install_another),
                                                    onClick = {
                                                        batchTotal = 0
                                                        batchQueue = emptyList()
                                                        installState = ApkInstallState.Idle
                                                    },
                                                    modifier = if (launchIntent != null) Modifier.weight(1f) else Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (state is ApkInstallState.Error) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Error,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }

                                        Text(
                                            text = stringResource(R.string.apk_install_failed_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )

                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            ExpressiveButton(
                                                text = stringResource(R.string.apk_action_retry),
                                                onClick = {
                                                    installState = ApkInstallState.Ready(details)
                                                },
                                                modifier = Modifier.weight(1f),
                                                icon = Icons.Default.Refresh
                                            )

                                            ExpressiveOutlinedButton(
                                                text = stringResource(R.string.apk_action_install_another),
                                                onClick = {
                                                    batchTotal = 0
                                                    batchQueue = emptyList()
                                                    installState = ApkInstallState.Idle
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (state is ApkInstallState.Error) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    ExpressiveButton(
                                        text = stringResource(R.string.apk_action_retry),
                                        onClick = {
                                            installState = ApkInstallState.Idle
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

