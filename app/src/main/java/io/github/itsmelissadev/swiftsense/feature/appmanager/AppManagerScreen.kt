package io.github.itsmelissadev.swiftsense.feature.appmanager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import io.github.itsmelissadev.swiftsense.service.shell.ShellBridge
import io.github.itsmelissadev.swiftsense.utils.AndroidUserInfo
import io.github.itsmelissadev.swiftsense.utils.UserManagerHelper
import io.github.itsmelissadev.swiftsense.ui.components.ExecutionStatusWidget
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveDialog
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveDialogButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveHeader
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveOutlinedButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSearchBar
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSelectionDialog
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToApkInstaller: () -> Unit = {}
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val preferenceManager = remember { PreferenceManager(context) }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(AppFilter.ALL) }
    var showImportExportDialog by remember { mutableStateOf(false) }

    var apps by remember { mutableStateOf<List<RichAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    val rawCustomListsJson by preferenceManager.appManagerCustomListsJson.collectAsState(initial = emptySet())
    val storedActiveListId by preferenceManager.appManagerActiveListId.collectAsState(initial = null)
    val storedTargetUserId by preferenceManager.appManagerTargetUserId.collectAsState(initial = UserManagerHelper.getCurrentProcessUserId())
    val legacyDisabledApps by preferenceManager.disabledApps.collectAsState(initial = emptySet())
    val isWarningDismissed by preferenceManager.appManagerWarningDismissed.collectAsState(initial = false)

    var selectedUserId by remember(storedTargetUserId) { mutableIntStateOf(storedTargetUserId) }
    var userProfiles by remember { mutableStateOf(UserManagerHelper.getInitialUserProfiles(context)) }
    var showUserSelectionDialog by remember { mutableStateOf(false) }

    val defaultListName = stringResource(R.string.default_list_name)
    val customLists = remember(rawCustomListsJson, legacyDisabledApps, defaultListName) {
        if (rawCustomListsJson.isEmpty()) {
            listOf(
                AppPackageList(
                    id = "default",
                    name = defaultListName,
                    packages = legacyDisabledApps
                )
            )
        } else {
            rawCustomListsJson.mapNotNull {
                try {
                    AppPackageList.fromJson(it)
                } catch (_: Exception) {
                    null
                }
            }.ifEmpty {
                listOf(
                    AppPackageList(
                        id = "default",
                        name = defaultListName,
                        packages = legacyDisabledApps
                    )
                )
            }
        }
    }

    val activeListId = storedActiveListId ?: customLists.firstOrNull()?.id ?: "default"
    val activeList = customLists.find { it.id == activeListId } ?: customLists.first()
    val activePackages = activeList.packages

    var showCreateListDialog by remember { mutableStateOf(false) }
    var showRenameListDialog by remember { mutableStateOf(false) }
    var showDeleteListDialog by remember { mutableStateOf(false) }
    var showListSelectionDialog by remember { mutableStateOf(false) }
    var listNameInput by remember { mutableStateOf("") }

    var selectedAppForDetails by remember { mutableStateOf<RichAppInfo?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var currentProcessingApp by remember { mutableStateOf("") }
    var processingAction by remember { mutableStateOf("") }
    var processingProgress by remember { mutableFloatStateOf(0f) }

    val isReady by ShellBridge.ready.collectAsState()

    fun saveLists(newLists: List<AppPackageList>, newActiveId: String? = null) {
        scope.launch {
            val jsonSet = newLists.map { it.toJson() }.toSet()
            val targetActiveId = newActiveId ?: activeListId
            preferenceManager.saveAppManagerCustomLists(jsonSet, targetActiveId)
            val currentActive = newLists.find { it.id == targetActiveId } ?: newLists.firstOrNull()
            if (currentActive != null) {
                preferenceManager.setDisabledApps(currentActive.packages)
            }
        }
    }

    fun toggleAppInActiveList(packageName: String) {
        val updatedPackages = if (activePackages.contains(packageName)) {
            activePackages - packageName
        } else {
            activePackages + packageName
        }
        val updatedLists = customLists.map {
            if (it.id == activeList.id) it.copy(packages = updatedPackages) else it
        }
        saveLists(updatedLists)
    }

    fun setPackagesInActiveList(packages: Set<String>) {
        val updatedLists = customLists.map {
            if (it.id == activeList.id) it.copy(packages = packages) else it
        }
        saveLists(updatedLists)
    }

    LaunchedEffect(isReady) {
        if (isReady) {
            withContext(Dispatchers.IO) {
                userProfiles = UserManagerHelper.getUserProfiles(context)
            }
        }
    }

    LaunchedEffect(selectedUserId) {
        isLoadingApps = true
        withContext(Dispatchers.IO) {
            apps = UserManagerHelper.loadRichAppsForUser(context, selectedUserId)
            isLoadingApps = false
        }
    }

    val filteredApps = remember(searchQuery, selectedFilter, apps) {
        apps.filter { app ->
            val matchesFilter = when (selectedFilter) {
                AppFilter.ALL -> true
                AppFilter.USER -> !app.isSystem
                AppFilter.SYSTEM -> app.isSystem
                AppFilter.DISABLED -> !app.isEnabled
                AppFilter.ENABLED -> app.isEnabled
            }
            val matchesQuery = if (searchQuery.isEmpty()) true
            else {
                val query = searchQuery.lowercase()
                app.label.lowercase().contains(query) || app.packageName.lowercase().contains(query)
            }
            matchesFilter && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            ExpressiveHeader(
                title = stringResource(R.string.feature_app_manager),
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            userProfiles = UserManagerHelper.getUserProfiles(context)
                        }
                        showUserSelectionDialog = true
                    }) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = if (userProfiles.size > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.new_list)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    listNameInput = ""
                                    showCreateListDialog = true
                                }
                            )
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
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.feature_apk_installer)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onNavigateToApkInstaller()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = activePackages.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        if (isProcessing) {
                            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                                Text(
                                    text = "$processingAction: $currentProcessingApp",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { processingProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ExpressiveOutlinedButton(
                                text = stringResource(R.string.action_enable),
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        processApps(
                                            context,
                                            selectedUserId,
                                            activePackages,
                                            true,
                                            onAppEnabled = { pkg, isEnabled ->
                                                apps = apps.map { if (it.packageName == pkg) it.copy(isEnabled = isEnabled) else it }
                                            }
                                        ) { action, pkg, progress ->
                                            processingAction = action
                                            currentProcessingApp = pkg
                                            processingProgress = progress
                                        }
                                        isProcessing = false
                                    }
                                },
                                enabled = !isProcessing && isReady,
                                modifier = Modifier.weight(1f)
                            )

                            ExpressiveButton(
                                text = stringResource(R.string.action_disable),
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        processApps(
                                            context,
                                            selectedUserId,
                                            activePackages,
                                            false,
                                            onAppEnabled = { pkg, isEnabled ->
                                                apps = apps.map { if (it.packageName == pkg) it.copy(isEnabled = isEnabled) else it }
                                            }
                                        ) { action, pkg, progress ->
                                            processingAction = action
                                            currentProcessingApp = pkg
                                            processingProgress = progress
                                        }
                                        isProcessing = false
                                    }
                                },
                                enabled = !isProcessing && isReady,
                                modifier = Modifier.weight(1f)
                            )
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (userProfiles.size > 1) {
                item {
                    val currentProfile = userProfiles.find { it.id == selectedUserId }
                    val profileLabel = currentProfile?.name ?: stringResource(R.string.user_profile_secondary, selectedUserId)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable { showUserSelectionDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$profileLabel ($selectedUserId)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item {
                ExpressiveSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = stringResource(R.string.search_apps)
                )
            }

            item {
                AppFilterBar(
                    selectedFilter = selectedFilter,
                    allCount = apps.size,
                    userCount = apps.count { !it.isSystem },
                    systemCount = apps.count { it.isSystem },
                    disabledCount = apps.count { !it.isEnabled },
                    enabledCount = apps.count { it.isEnabled },
                    onFilterSelected = { selectedFilter = it }
                )
            }

            item {
                ActiveListCard(
                    activeList = activeList,
                    customListsCount = customLists.size,
                    onSwitchList = { showListSelectionDialog = true },
                    onNewList = {
                        listNameInput = ""
                        showCreateListDialog = true
                    },
                    onRenameList = {
                        listNameInput = activeList.name
                        showRenameListDialog = true
                    },
                    onDeleteList = {
                        showDeleteListDialog = true
                    },
                    onToggleSelectAll = {
                        val visiblePackages = filteredApps.map { it.packageName }.toSet()
                        val allSelected = visiblePackages.all { activePackages.contains(it) }
                        if (allSelected) {
                            setPackagesInActiveList(activePackages - visiblePackages)
                        } else {
                            setPackagesInActiveList(activePackages + visiblePackages)
                        }
                    }
                )
            }

            if (!isReady) {
                item { ExecutionStatusWidget() }
            }

            if (!isWarningDismissed) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.app_manager_warning_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.app_manager_warning_text),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        preferenceManager.setAppManagerWarningDismissed(true)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isLoadingApps) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(strokeWidth = 4.dp) }
                }
            }

            items(items = filteredApps, key = { it.packageName }) { app ->
                AppItem(
                    app = app,
                    isSelected = activePackages.contains(app.packageName),
                    onToggleSelect = { toggleAppInActiveList(app.packageName) },
                    onLongClick = { selectedAppForDetails = app }
                )
            }
        }
    }

    if (selectedAppForDetails != null) {
        AppDetailDialog(
            app = selectedAppForDetails!!,
            targetUserId = selectedUserId,
            isInActiveList = activePackages.contains(selectedAppForDetails!!.packageName),
            isReady = isReady,
            onToggleActiveList = {
                toggleAppInActiveList(selectedAppForDetails!!.packageName)
            },
            onStatusChanged = { newEnabledState ->
                apps = apps.map {
                    if (it.packageName == selectedAppForDetails!!.packageName) {
                        it.copy(isEnabled = newEnabledState)
                    } else it
                }
                selectedAppForDetails = selectedAppForDetails?.copy(isEnabled = newEnabledState)
            },
            onDismiss = { selectedAppForDetails = null }
        )
    }

    if (showUserSelectionDialog) {
        val options = userProfiles.map { user ->
            val activeTag = if (user.isCurrentProcess) " ${stringResource(R.string.user_profile_current_tag)}" else ""
            user.id to "${user.name} (${user.id})$activeTag"
        }
        ExpressiveSelectionDialog(
            title = stringResource(R.string.select_user_profile),
            options = options,
            selected = selectedUserId,
            onSelectedChange = { newId ->
                selectedUserId = newId
                scope.launch {
                    preferenceManager.setAppManagerTargetUserId(newId)
                }
            },
            onDismissRequest = { showUserSelectionDialog = false }
        )
    }

    if (showCreateListDialog) {
        ExpressiveDialog(
            onDismissRequest = { showCreateListDialog = false },
            title = stringResource(R.string.create_list),
            content = {
                ExpressiveTextField(
                    value = listNameInput,
                    onValueChange = { listNameInput = it },
                    label = stringResource(R.string.list_name)
                )
            },
            confirmButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_apply),
                    enabled = listNameInput.trim().isNotEmpty(),
                    onClick = {
                        val name = listNameInput.trim()
                        val newList = AppPackageList(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            packages = emptySet()
                        )
                        val updated = customLists + newList
                        saveLists(updated, newList.id)
                        scope.launch { preferenceManager.setAppManagerActiveListId(newList.id) }
                        showCreateListDialog = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_list_created),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            dismissButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { showCreateListDialog = false }
                )
            }
        )
    }

    if (showRenameListDialog) {
        ExpressiveDialog(
            onDismissRequest = { showRenameListDialog = false },
            title = stringResource(R.string.rename_list),
            content = {
                ExpressiveTextField(
                    value = listNameInput,
                    onValueChange = { listNameInput = it },
                    label = stringResource(R.string.list_name)
                )
            },
            confirmButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_apply),
                    enabled = listNameInput.trim().isNotEmpty(),
                    onClick = {
                        val updatedLists = customLists.map {
                            if (it.id == activeList.id) it.copy(name = listNameInput.trim()) else it
                        }
                        saveLists(updatedLists)
                        showRenameListDialog = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_list_renamed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            dismissButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { showRenameListDialog = false }
                )
            }
        )
    }

    if (showDeleteListDialog) {
        ExpressiveDialog(
            onDismissRequest = { showDeleteListDialog = false },
            title = stringResource(R.string.confirm_delete_list_title),
            description = stringResource(R.string.confirm_delete_list_desc),
            confirmButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.delete_list),
                    onClick = {
                        val remaining = customLists.filter { it.id != activeList.id }
                        val nextList = remaining.firstOrNull() ?: AppPackageList(
                            id = "default",
                            name = defaultListName,
                            packages = emptySet()
                        )
                        val finalLists = if (remaining.isEmpty()) listOf(nextList) else remaining
                        saveLists(finalLists, nextList.id)
                        scope.launch { preferenceManager.setAppManagerActiveListId(nextList.id) }
                        showDeleteListDialog = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_list_deleted),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            dismissButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { showDeleteListDialog = false }
                )
            }
        )
    }

    if (showListSelectionDialog) {
        val options = customLists.map { it.id to "${it.name} (${it.packages.size})" }
        ExpressiveSelectionDialog(
            title = stringResource(R.string.custom_lists),
            options = options,
            selected = activeList.id,
            onSelectedChange = { selectedId ->
                scope.launch {
                    preferenceManager.setAppManagerActiveListId(selectedId)
                    val list = customLists.find { it.id == selectedId }
                    if (list != null) {
                        preferenceManager.setDisabledApps(list.packages)
                    }
                }
            },
            onDismissRequest = { showListSelectionDialog = false }
        )
    }

    if (showImportExportDialog) {
        ImportExportDialog(
            currentList = activePackages,
            onDismiss = { showImportExportDialog = false },
            onImport = { newList ->
                setPackagesInActiveList(newList)
                showImportExportDialog = false
            }
        )
    }
}

@Composable
private fun AppFilterBar(
    selectedFilter: AppFilter,
    allCount: Int,
    userCount: Int,
    systemCount: Int,
    disabledCount: Int,
    enabledCount: Int,
    onFilterSelected: (AppFilter) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppFilter.values().forEach { filter ->
            val count = when (filter) {
                AppFilter.ALL -> allCount
                AppFilter.USER -> userCount
                AppFilter.SYSTEM -> systemCount
                AppFilter.DISABLED -> disabledCount
                AppFilter.ENABLED -> enabledCount
            }
            val isSelected = selectedFilter == filter
            Surface(
                onClick = { onFilterSelected(filter) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(filter.titleRes),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveListCard(
    activeList: AppPackageList,
    customListsCount: Int,
    onSwitchList: () -> Unit,
    onNewList: () -> Unit,
    onRenameList: () -> Unit,
    onDeleteList: () -> Unit,
    onToggleSelectAll: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = onSwitchList
                    )
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = activeList.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${activeList.packages.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.active_list),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onToggleSelectAll,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.SelectAll,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.custom_lists)) },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onSwitchList()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.new_list)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onNewList()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename_list)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onRenameList()
                            }
                        )
                        if (customListsCount > 1) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_list)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDeleteList()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItem(
    app: RichAppInfo,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onToggleSelect,
                onLongClick = onLongClick
            )
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
                        } catch (_: Exception) {
                            icon = null
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(bitmap = icon!!, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(
                        Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
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
                        text = if (app.isEnabled) stringResource(R.string.app_status_enabled)
                        else stringResource(R.string.app_status_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (app.isEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )

                    Text(
                        text = if (app.isSystem) stringResource(R.string.app_type_system)
                        else stringResource(R.string.app_type_user),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
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

    ExpressiveDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.menu_import_export),
        description = stringResource(R.string.import_hint),
        content = {
            ExpressiveTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.menu_import_export),
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
        },
        confirmButton = {
            ExpressiveDialogButton(
                text = stringResource(R.string.action_import),
                onClick = {
                    val newList =
                        text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                    onImport(newList)
                }
            )
        },
        dismissButton = {
            ExpressiveDialogButton(
                text = stringResource(R.string.action_cancel),
                isPrimary = false,
                onClick = onDismiss
            )
        }
    )
}

private suspend fun processApps(
    context: Context,
    targetUserId: Int,
    packageNames: Set<String>,
    enable: Boolean,
    onAppEnabled: (String, Boolean) -> Unit,
    onProgress: (String, String, Float) -> Unit
) {
    var successCount = 0
    val actionName =
        if (enable) context.getString(R.string.action_enabling) else context.getString(R.string.action_disabling)

    val listToProcess = packageNames.toList()
    val total = listToProcess.size.toFloat()

    withContext(Dispatchers.IO) {
        listToProcess.forEachIndexed { index, pkg ->
            val progress = (index + 1) / total
            withContext(Dispatchers.Main) { onProgress(actionName, pkg, progress) }

            val cmd = if (enable) "enable" else "disable-user"
            val result = ShellBridge.run("pm $cmd --user $targetUserId $pkg")

            if (result.isSuccess) {
                successCount++
                withContext(Dispatchers.Main) {
                    onAppEnabled(pkg, enable)
                }
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
