package io.github.itsmelissadev.swiftsense.feature.appstopper

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CircularProgressIndicator
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
import io.github.itsmelissadev.swiftsense.service.shizuku.ShizukuShellRunner
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
import io.github.itsmelissadev.swiftsense.ui.components.ShizukuStatusWidget
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseButton
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSelectionDialog
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTextField
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

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

@OptIn(ExperimentalMaterial3Api::class)
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
    var isLoadingApps by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(StopperFilter.ALL) }

    val isShizukuReady = Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val filteredApps = installedApps.filter { app ->
                val isSelf = app.packageName == context.packageName
                val isShizuku = app.packageName.contains("shizuku", ignoreCase = true)
                val isCritical = app.flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
                        (app.packageName.contains("android.launcher") ||
                                app.packageName == "android" ||
                                app.packageName.contains("systemui"))
                !isSelf && !isShizuku && !isCritical
            }.map { app ->
                AppInfo(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    icon = pm.getApplicationIcon(app).toBitmap().asImageBitmap(),
                    isSystem = app.flags and ApplicationInfo.FLAG_SYSTEM != 0
                )
            }.sortedBy { it.label.lowercase() }

            apps = filteredApps
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
        topBar = {
            SwiftSenseTopAppBar(
                title = stringResource(R.string.feature_app_stopper),
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedPackages.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        if (isRunning) {
                            Column(modifier = Modifier.padding(bottom = 14.dp)) {
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
                                        .height(4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                )
                            }
                        }

                        SwiftSenseButton(
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
                                            ShizukuShellRunner.runCommand("$cmdPrefix ${app.packageName}")
                                        }
                                    }
                                    isRunning = false
                                }
                            },
                            enabled = selectedPackages.isNotEmpty() && isShizukuReady && !isRunning,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ShizukuStatusWidget() }

            item {
                FeatureCard(
                    title = stringResource(R.string.stopper_service_title),
                    description = if (isStopperServiceRunning) {
                        stringResource(
                            R.string.stopper_service_running,
                            selectedPackages.size,
                            stopperInterval
                        )
                    } else {
                        stringResource(R.string.stopper_service_desc)
                    },
                    icon = Icons.Default.Bolt,
                    checked = isStopperServiceRunning,
                    enabled = isShizukuReady,
                    onCheckedChange = { active ->
                        scope.launch {
                            preferenceManager.setStopperServiceRunning(active)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SwiftSenseTextField(
                            value = stringResource(
                                when (currentStopMode) {
                                    StopMode.SIMPLE -> R.string.mode_simple
                                    StopMode.FORCE -> R.string.mode_force
                                }
                            ),
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.stop_mode),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showModeDialog = true }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
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
                        SwiftSenseTextField(
                            value = intervalLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.stopper_interval),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showIntervalDialog = true }
                        )
                    }
                }
            }

            item {
                SwiftSenseTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = stringResource(R.string.search_apps),
                    leadingIcon = Icons.Default.Search,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
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
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
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
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
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
                        Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 4.dp)
                    }
                }
            }

            items(filteredList, key = { it.packageName }) { app ->
                AppItem(
                    app = app,
                    isSelected = selectedPackages.contains(app.packageName),
                    onToggle = {
                        scope.launch { preferenceManager.toggleStopperApp(app.packageName) }
                    }
                )
            }
        }
    }

    if (showModeDialog) {
        SwiftSenseSelectionDialog(
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
        SwiftSenseSelectionDialog(
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
        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(filter.titleRes),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.background
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
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
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).alpha(if (isSelected) 1f else 0.7f)
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
                            null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
