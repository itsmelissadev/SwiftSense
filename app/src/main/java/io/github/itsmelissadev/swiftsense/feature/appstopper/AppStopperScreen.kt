package io.github.itsmelissadev.swiftsense.feature.appstopper

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isSystem: Boolean
)

enum class StopMode {
    SIMPLE, FORCE
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
    var isComplete by remember { mutableStateOf(false) }
    var lastProcessedApp by remember { mutableStateOf("") }

    var stopMode by remember { mutableStateOf(StopMode.SIMPLE) }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val selectedPackages by preferenceManager.stopperApps.collectAsState(initial = emptySet())
    var isLoadingApps by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val isShizukuReady = Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val filteredApps = installedApps.filter { app ->
                val isSelf = app.packageName == context.packageName
                val isCritical = app.flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
                        (app.packageName.contains("android.launcher") ||
                                app.packageName == "android" ||
                                app.packageName.contains("systemui"))
                !isSelf && !isCritical
            }.map { app ->
                AppInfo(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    icon = pm.getApplicationIcon(app),
                    isSystem = app.flags and ApplicationInfo.FLAG_SYSTEM != 0
                )
            }.sortedBy { it.label }

            apps = filteredApps
            isLoadingApps = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.feature_app_stopper).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            ShizukuStatusWidget()

            Spacer(modifier = Modifier.height(24.dp))

            if (isRunning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp,
                            color = MaterialTheme.colorScheme.primary,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            stringResource(R.string.closing_apps),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            lastProcessedApp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else if (isComplete) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DoneAll,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.optimization_complete),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { isComplete = false },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                stringResource(R.string.granted),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Settings Section
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.stop_mode).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = stopMode == StopMode.SIMPLE,
                                onClick = { stopMode = StopMode.SIMPLE },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = 0,
                                    count = 2,
                                    baseShape = RoundedCornerShape(8.dp)
                                )
                            ) {
                                Text(
                                    stringResource(R.string.mode_simple),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            SegmentedButton(
                                selected = stopMode == StopMode.FORCE,
                                onClick = { stopMode = StopMode.FORCE },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = 1,
                                    count = 2,
                                    baseShape = RoundedCornerShape(8.dp)
                                )
                            ) {
                                Text(
                                    stringResource(R.string.mode_force),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Search & Actions
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            stringResource(R.string.search_apps),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    preferenceManager.setStopperApps(apps.map { it.packageName }
                                        .toSet())
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                stringResource(R.string.select_all),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "|",
                            modifier = Modifier.padding(horizontal = 4.dp).alpha(0.2f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { scope.launch { preferenceManager.setStopperApps(emptySet()) } },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                stringResource(R.string.deselect_all),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            stringResource(R.string.selected_count, selectedPackages.size),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }

                if (isLoadingApps) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                } else {
                    val filteredList = apps.filter { it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                // Action Button (Sticky at bottom but part of column flow or FAB)
                Button(
                    onClick = {
                        scope.launch {
                            isRunning = true
                            isComplete = false
                            val targets = apps.filter { selectedPackages.contains(it.packageName) }
                            val cmdPrefix =
                                if (stopMode == StopMode.FORCE) "am force-stop" else "am kill"
                            targets.forEach { app ->
                                lastProcessedApp = app.label
                                ShizukuShellRunner.runCommand("$cmdPrefix ${app.packageName}")
                                delay(50)
                            }
                            isRunning = false
                            isComplete = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .height(52.dp),
                    enabled = selectedPackages.isNotEmpty() && isShizukuReady && !isRunning,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.DoneAll, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.stop_now).uppercase(),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
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
        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    bitmap = app.icon.toBitmap().asImageBitmap(),
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
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant).copy(
                        alpha = 0.7f
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
