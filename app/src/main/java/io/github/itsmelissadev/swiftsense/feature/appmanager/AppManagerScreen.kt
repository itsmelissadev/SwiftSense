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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.shizuku.ShizukuShellRunner
import io.github.itsmelissadev.swiftsense.ui.components.ShadcnDialog
import io.github.itsmelissadev.swiftsense.ui.components.ShadcnDialogButton
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
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.search_apps).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(48.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.3f
                            ),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.3f
                            ),
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
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
                                    text = "$processingAction: $currentProcessingApp".uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { processingProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
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
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    stringResource(R.string.action_enable).uppercase(),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.2.sp
                                    )
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
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    stringResource(R.string.action_disable).uppercase(),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.2.sp
                                    )
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ShizukuStatusWidget() }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(R.string.app_manager_warning_title).uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.app_manager_warning_text),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(bitmap = icon!!, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(
                        Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (app.isEnabled) stringResource(R.string.app_status_enabled).uppercase()
                        else stringResource(R.string.app_status_disabled).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = if (app.isEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )

                    Text(
                        text = (if (app.isSystem) stringResource(R.string.app_type_system)
                        else stringResource(R.string.app_type_user)).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
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

    ShadcnDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.menu_import_export),
        description = stringResource(R.string.import_hint),
        content = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            )
        },
        confirmButton = {
            ShadcnDialogButton(
                text = stringResource(R.string.action_import),
                onClick = {
                    val newList =
                        text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                    onImport(newList)
                }
            )
        },
        dismissButton = {
            ShadcnDialogButton(
                text = stringResource(R.string.action_cancel),
                isPrimary = false,
                onClick = onDismiss
            )
        }
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