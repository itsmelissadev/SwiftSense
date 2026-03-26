package io.github.itsmelissadev.swiftsense.feature.appmanager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.shizuku.ShizukuShellRunner
import io.github.itsmelissadev.swiftsense.ui.components.ShizukuStatusWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

private val iconCache = mutableMapOf<String, ImageBitmap>()

data class RichAppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val isEnabled: Boolean,
    val appInfo: ApplicationInfo
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val preferenceManager = remember { PreferenceManager(context) }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showImportExportDialog by remember { mutableStateOf(false) }

    var apps by remember { mutableStateOf<List<RichAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    val selectedApps by preferenceManager.disabledApps.collectAsState(initial = emptySet())

    var isProcessing by remember { mutableStateOf(false) }
    var currentProcessingApp by remember { mutableStateOf("") }
    var processingAction by remember { mutableStateOf("") }
    var processingProgress by remember { mutableStateOf(0f) }

    val isShizukuReady = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isShizukuReady.value =
                Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            delay(2000)
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val installedApps =
                pm.getInstalledApplications(PackageManager.GET_META_DATA or PackageManager.MATCH_DISABLED_COMPONENTS)
            apps = installedApps.map { app ->
                RichAppInfo(
                    packageName = app.packageName,
                    label = app.loadLabel(pm).toString(),
                    isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isEnabled = app.enabled,
                    appInfo = app
                )
            }.sortedBy { it.label.lowercase() }
            isLoadingApps = false
        }
    }

    val filteredApps = remember(searchQuery, apps) {
        if (searchQuery.isEmpty()) apps
        else {
            val query = searchQuery.lowercase()
            apps.filter {
                it.label.lowercase().contains(query) || it.packageName.lowercase().contains(query)
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.search_apps),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(56.dp),
                        singleLine = true,
                        shape = CircleShape,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                alpha = 0.9f
                            ),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                alpha = 0.6f
                            ),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_import_export)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.SwapVert,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showImportExportDialog = true
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedApps.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .navigationBarsPadding()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                        if (isProcessing) {
                            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                                Text(
                                    text = "$processingAction: $currentProcessingApp",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { processingProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        processApps(
                                            context,
                                            selectedApps,
                                            true,
                                            preferenceManager
                                        ) { action, pkg, progress ->
                                            processingAction = action
                                            currentProcessingApp = pkg
                                            processingProgress = progress
                                        }
                                        isProcessing = false
                                    }
                                },
                                enabled = !isProcessing && isShizukuReady.value,
                                shape = RoundedCornerShape(100),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                Text(
                                    stringResource(R.string.action_enable),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        processApps(
                                            context,
                                            selectedApps,
                                            false,
                                            preferenceManager
                                        ) { action, pkg, progress ->
                                            processingAction = action
                                            currentProcessingApp = pkg
                                            processingProgress = progress
                                        }
                                        isProcessing = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                enabled = !isProcessing && isShizukuReady.value,
                                shape = RoundedCornerShape(100),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                Text(
                                    stringResource(R.string.action_disable),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ShizukuStatusWidget() }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                stringResource(R.string.app_manager_warning_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                stringResource(R.string.app_manager_warning_text),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            if (isLoadingApps) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(strokeWidth = 5.dp) }
                }
            }

            items(items = filteredApps, key = { it.packageName }) { app ->
                AppItem(
                    app = app,
                    isSelected = selectedApps.contains(app.packageName),
                    onToggleSelect = { scope.launch { preferenceManager.toggleAppInList(app.packageName) } }
                )
            }
        }
    }

    if (showImportExportDialog) {
        ImportExportDialog(
            currentList = selectedApps,
            onDismiss = { showImportExportDialog = false },
            onImport = { newList ->
                scope.launch {
                    preferenceManager.setDisabledApps(newList)
                    showImportExportDialog = false
                }
            }
        )
    }
}

@Composable
fun AppItem(app: RichAppInfo, isSelected: Boolean, onToggleSelect: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager

    Surface(
        onClick = onToggleSelect,
        shape = RoundedCornerShape(32.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var icon by remember(app.packageName) { mutableStateOf(iconCache[app.packageName]) }
            LaunchedEffect(app.packageName) {
                if (icon == null) {
                    withContext(Dispatchers.IO) {
                        try {
                            val loadedIcon = pm.getApplicationIcon(app.appInfo).toBitmap().asImageBitmap()
                            iconCache[app.packageName] = loadedIcon
                            icon = loadedIcon
                        } catch (_: Exception) { icon = null }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(bitmap = icon!!, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (app.isEnabled) stringResource(R.string.app_status_enabled)
                        else stringResource(R.string.app_status_disabled),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        else if (app.isEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.outline
                    )

                    Text(
                        text = if (app.isSystem) stringResource(R.string.app_type_system)
                        else stringResource(R.string.app_type_user),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    checkmarkColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun ImportExportDialog(
    currentList: Set<String>,
    onDismiss: () -> Unit,
    onImport: (Set<String>) -> Unit
) {
    var text by remember { mutableStateOf(currentList.joinToString("\n")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.menu_import_export),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                placeholder = { Text(stringResource(R.string.import_hint)) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val newList =
                        text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                    onImport(newList)
                },
                shape = RoundedCornerShape(100)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.action_cancel),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

private suspend fun processApps(
    context: Context,
    packageNames: Set<String>,
    enable: Boolean,
    preferenceManager: PreferenceManager,
    onProgress: (String, String, Float) -> Unit
) {
    var successCount = 0
    val pm = context.packageManager
    val actionName =
        if (enable) context.getString(R.string.action_enabling) else context.getString(R.string.action_disabling)

    val listToProcess = packageNames.toList()
    val total = listToProcess.size.toFloat()

    withContext(Dispatchers.IO) {
        listToProcess.forEachIndexed { index, pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                val progress = (index + 1) / total
                withContext(Dispatchers.Main) { onProgress(actionName, pkg, progress) }

                val cmd = if (enable) "enable" else "disable-user"
                val result = ShizukuShellRunner.runCommand("pm $cmd $pkg")

                if (result.isSuccess) successCount++
                delay(10)
            } catch (_: PackageManager.NameNotFoundException) {
                preferenceManager.toggleAppInList(pkg)
            }
        }
    }

    withContext(Dispatchers.Main) {
        Toast.makeText(
            context,
            context.getString(R.string.toast_process_complete, successCount),
            Toast.LENGTH_SHORT
        ).show()
    }
}