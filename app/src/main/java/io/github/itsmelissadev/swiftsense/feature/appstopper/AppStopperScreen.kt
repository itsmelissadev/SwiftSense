package io.github.itsmelissadev.swiftsense.feature.appstopper

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.shell.ShellBridge
import io.github.itsmelissadev.swiftsense.ui.components.ExecutionStatusWidget
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveCardGroup
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveHeader
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveHeroToggleCard
import io.github.itsmelissadev.swiftsense.ui.components.ExpressivePreferenceItem
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSearchBar
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSelectionDialog
import io.github.itsmelissadev.swiftsense.ui.theme.ExpressivePalette
import io.github.itsmelissadev.swiftsense.utils.UserManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
    val isSystem: Boolean
)

enum class StopMode {
    SIMPLE, FORCE
}

enum class StopperFilter(val titleRes: Int) {
    ALL(R.string.filter_all),
    USER(R.string.filter_user),
    SYSTEM(R.string.filter_system),
    SELECTED(R.string.common_selected)
}

@Composable
fun AppStopperScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferenceManager = remember { PreferenceManager(context) }

    var isRunning by remember { mutableStateOf(false) }
    var currentProcessingApp by remember { mutableStateOf("") }
    var processingProgress by remember { mutableFloatStateOf(0f) }

    val isStopperServiceRunning by AppStopperService.isRunning.collectAsState()
    val stopperInterval by preferenceManager.stopperInterval.collectAsState(initial = 10)
    val stopperModeString by preferenceManager.stopperMode.collectAsState(initial = "FORCE")
    val currentStopMode = if (stopperModeString.equals("SIMPLE", ignoreCase = true)) StopMode.SIMPLE else StopMode.FORCE

    var showModeDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val selectedPackages by preferenceManager.stopperApps.collectAsState(initial = emptySet())
    val storedTargetUserId by preferenceManager.stopperTargetUserId.collectAsState(initial = UserManagerHelper.getCurrentProcessUserId())
    var selectedUserId by remember(storedTargetUserId) { mutableIntStateOf(storedTargetUserId) }
    var userProfiles by remember { mutableStateOf(UserManagerHelper.getInitialUserProfiles(context)) }
    var showUserSelectionDialog by remember { mutableStateOf(false) }

    var isLoadingApps by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(StopperFilter.ALL) }

    val isReady by ShellBridge.ready.collectAsState()

    LaunchedEffect(isReady) {
        if (isReady) {
            withContext(Dispatchers.IO) {
                userProfiles = UserManagerHelper.getUserProfiles(context)
            }
        }
    }

    LaunchedEffect(selectedUserId, isReady) {
        isLoadingApps = true
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installed = try {
                if (isReady && selectedUserId != UserManagerHelper.getCurrentProcessUserId()) {
                    val rawPackages = ShellBridge.run("pm list packages --user $selectedUserId")
                    val output = rawPackages.getOrNull() ?: ""
                    val pkgNames = output.lines()
                        .filter { it.startsWith("package:") }
                        .map { it.removePrefix("package:").trim() }

                    pkgNames.mapNotNull { pkg ->
                        try {
                            pm.getApplicationInfo(pkg, 0)
                        } catch (_: Exception) {
                            null
                        }
                    }
                } else {
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                }
            } catch (_: Exception) {
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }

            val appList = installed
                .filter { it.packageName != context.packageName }
                .map { appInfo ->
                    val label = appInfo.loadLabel(pm).toString()
                    val iconDrawable = appInfo.loadIcon(pm)
                    val bitmap = iconDrawable.toBitmap(96, 96).asImageBitmap()
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    AppInfo(appInfo.packageName, label, bitmap, isSystem)
                }
                .sortedBy { it.label.lowercase() }

            apps = appList
            isLoadingApps = false
        }
    }

    val filteredList = remember(apps, searchQuery, selectedFilter, selectedPackages) {
        apps.filter { app ->
            val matchesFilter = when (selectedFilter) {
                StopperFilter.ALL -> true
                StopperFilter.USER -> !app.isSystem
                StopperFilter.SYSTEM -> app.isSystem
                StopperFilter.SELECTED -> selectedPackages.contains(app.packageName)
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ExpressiveHeader(
                title = stringResource(R.string.feature_app_stopper),
                onNavigateBack = onNavigateBack,
                actions = {
                    if (userProfiles.size > 1) {
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                userProfiles = UserManagerHelper.getUserProfiles(context)
                            }
                            showUserSelectionDialog = true
                        }) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedPackages.isNotEmpty(),
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
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        if (isRunning) {
                            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                Text(
                                    text = currentProcessingApp,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { processingProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            }
                        }

                        ExpressiveButton(
                            text = stringResource(R.string.stop_now),
                            onClick = {
                                scope.launch {
                                    isRunning = true
                                    val targets = apps.filter { selectedPackages.contains(it.packageName) }
                                    val cmdPrefix =
                                        if (currentStopMode == StopMode.FORCE) "am force-stop" else "am kill"
                                    val total = targets.size.toFloat()
                                    targets.forEachIndexed { index, app ->
                                        currentProcessingApp = app.label
                                        processingProgress = (index + 1) / total
                                        withContext(Dispatchers.IO) {
                                            ShellBridge.run("$cmdPrefix --user $selectedUserId ${app.packageName}")
                                        }
                                    }
                                    isRunning = false
                                }
                            },
                            enabled = selectedPackages.isNotEmpty() && isReady && !isRunning,
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.DoneAll
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isReady) {
                item { ExecutionStatusWidget() }
            }

            item {
                ExpressiveHeroToggleCard(
                    title = stringResource(R.string.stopper_service_title),
                    subtitle = if (isStopperServiceRunning) {
                        stringResource(
                            R.string.stopper_service_running,
                            selectedPackages.size,
                            stopperInterval
                        )
                    } else {
                        stringResource(R.string.stopper_service_desc)
                    },
                    checked = isStopperServiceRunning,
                    enabled = isReady,
                    onCheckedChange = { active ->
                        scope.launch {
                            preferenceManager.setStopperServiceRunning(active)
                            if (active) {
                                preferenceManager.setStopperTargetUserId(selectedUserId)
                            }
                            val intent = Intent(context, AppStopperService::class.java)
                            if (active) {
                                context.startForegroundService(intent)
                            } else {
                                context.stopService(intent)
                            }
                        }
                    }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                )

                ExpressiveCardGroup {
                    item {
                        ExpressivePreferenceItem(
                            title = stringResource(R.string.stop_mode),
                            description = stringResource(
                                when (currentStopMode) {
                                    StopMode.SIMPLE -> R.string.mode_simple
                                    StopMode.FORCE -> R.string.mode_force
                                }
                            ),
                            icon = Icons.Rounded.Speed,
                            badgeColors = ExpressivePalette.AmberLight,
                            onClick = { showModeDialog = true }
                        )
                    }

                    val intervalLabel = when (stopperInterval) {
                        3 -> stringResource(R.string.interval_3s)
                        5 -> stringResource(R.string.interval_5s)
                        10 -> stringResource(R.string.interval_10s)
                        30 -> stringResource(R.string.interval_30s)
                        60 -> stringResource(R.string.interval_1m)
                        120 -> stringResource(R.string.interval_2m)
                        300 -> stringResource(R.string.interval_5m)
                        else -> "$stopperInterval s"
                    }

                    item {
                        ExpressivePreferenceItem(
                            title = stringResource(R.string.stopper_interval),
                            description = intervalLabel,
                            icon = Icons.Rounded.Timer,
                            badgeColors = ExpressivePalette.BlueLight,
                            onClick = { showIntervalDialog = true }
                        )
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
                StopperFilterBar(
                    selectedFilter = selectedFilter,
                    allCount = apps.size,
                    userCount = apps.count { !it.isSystem },
                    systemCount = apps.count { it.isSystem },
                    selectedCount = selectedPackages.size,
                    onFilterSelected = { selectedFilter = it }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                scope.launch {
                                    val visiblePackages = filteredList.map { it.packageName }.toSet()
                                    preferenceManager.setStopperApps(selectedPackages + visiblePackages)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.SelectAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.select_all),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                scope.launch {
                                    val visiblePackages = filteredList.map { it.packageName }.toSet()
                                    preferenceManager.setStopperApps(selectedPackages - visiblePackages)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Deselect,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.deselect_all),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (isLoadingApps) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else if (filteredList.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_apps_found),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp)
                    )
                }
            } else {
                items(filteredList, key = { it.packageName }) { app ->
                    val isSelected = selectedPackages.contains(app.packageName)
                    AppItem(
                        app = app,
                        isSelected = isSelected,
                        onToggle = {
                            scope.launch {
                                preferenceManager.toggleStopperApp(app.packageName)
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showModeDialog) {
        ExpressiveSelectionDialog(
            title = stringResource(R.string.stop_mode),
            options = listOf(
                StopMode.SIMPLE to stringResource(R.string.mode_simple),
                StopMode.FORCE to stringResource(R.string.mode_force)
            ),
            selected = currentStopMode,
            onSelectedChange = { mode ->
                scope.launch { preferenceManager.setStopperMode(mode.name) }
            },
            onDismissRequest = { showModeDialog = false }
        )
    }

    if (showIntervalDialog) {
        ExpressiveSelectionDialog(
            title = stringResource(R.string.stopper_interval),
            options = listOf(
                3 to stringResource(R.string.interval_3s),
                5 to stringResource(R.string.interval_5s),
                10 to stringResource(R.string.interval_10s),
                30 to stringResource(R.string.interval_30s),
                60 to stringResource(R.string.interval_1m),
                120 to stringResource(R.string.interval_2m),
                300 to stringResource(R.string.interval_5m)
            ),
            selected = stopperInterval,
            onSelectedChange = { interval ->
                scope.launch { preferenceManager.setStopperInterval(interval) }
            },
            onDismissRequest = { showIntervalDialog = false }
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
                    preferenceManager.setStopperTargetUserId(newId)
                }
            },
            onDismissRequest = { showUserSelectionDialog = false }
        )
    }
}

@Composable
private fun StopperFilterBar(
    selectedFilter: StopperFilter,
    allCount: Int,
    userCount: Int,
    systemCount: Int,
    selectedCount: Int,
    onFilterSelected: (StopperFilter) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StopperFilter.values().forEach { filter ->
            val count = when (filter) {
                StopperFilter.ALL -> allCount
                StopperFilter.USER -> userCount
                StopperFilter.SYSTEM -> systemCount
                StopperFilter.SELECTED -> selectedCount
            }
            val isSelected = selectedFilter == filter
            Surface(
                onClick = { onFilterSelected(filter) },
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(filter.titleRes),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun AppItem(app: AppInfo, isSelected: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .alpha(if (isSelected) 1f else 0.85f)
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
