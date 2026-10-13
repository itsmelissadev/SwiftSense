package io.github.itsmelissadev.swiftsense.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.LaunchedEffect
import io.github.itsmelissadev.swiftsense.service.adb.AdbConnectionState
import io.github.itsmelissadev.swiftsense.service.adb.AdbManager
import io.github.itsmelissadev.swiftsense.service.adb.SwiftSenseDaemonBridge
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveDialog
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveDialogButton
import io.github.itsmelissadev.swiftsense.BuildConfig
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.ui.components.AdbConnectionDialog
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveCardGroup
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveHeader
import io.github.itsmelissadev.swiftsense.ui.components.ExpressivePreferenceItem
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSelectionDialog
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSwitchPreference
import io.github.itsmelissadev.swiftsense.ui.theme.ExpressivePalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferenceManager: PreferenceManager,
    onNavigateBack: () -> Unit,
    onNavigateToUpdates: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by preferenceManager.themeMode.collectAsState(initial = "system")
    val dynamicColor by preferenceManager.dynamicColor.collectAsState(initial = false)
    val themeAccentColor by preferenceManager.themeAccentColor.collectAsState(initial = "pistachio")
    val language by preferenceManager.language.collectAsState(initial = "system")
    val shellMode by preferenceManager.shellMode.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showShellModeDialog by remember { mutableStateOf(false) }
    val adbConnectionState by AdbManager.connectionState.collectAsState()
    var showAdbSetupDialog by remember { mutableStateOf(false) }
    val autoStartService by preferenceManager.swiftSenseAutoStart.collectAsState(initial = false)
    val hasPairedAdb by preferenceManager.hasPairedAdb.collectAsState(initial = false)

    var isShizukuRunning by remember { mutableStateOf(false) }
    var isShizukuAuthorized by remember { mutableStateOf(false) }
    var isDaemonRunning by remember { mutableStateOf(false) }
    var showStopServiceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isShizukuRunning = rikka.shizuku.Shizuku.pingBinder()
            isShizukuAuthorized = if (isShizukuRunning) {
                rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else false
            isDaemonRunning = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                SwiftSenseDaemonBridge.isDaemonAlive()
            }
            io.github.itsmelissadev.swiftsense.service.shell.ShellBridge.refreshNow()
            kotlinx.coroutines.delay(1500)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ExpressiveHeader(
                title = stringResource(R.string.settings_title),
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.appearance_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )

                ExpressiveCardGroup {
                    item {
                        ExpressivePreferenceItem(
                            title = stringResource(R.string.theme_option),
                            description = when (themeMode) {
                                "light" -> stringResource(R.string.theme_light)
                                "dark" -> stringResource(R.string.theme_dark)
                                else -> stringResource(R.string.theme_system)
                            },
                            icon = Icons.Rounded.ColorLens,
                            badgeColors = ExpressivePalette.BlueLight,
                            onClick = { showThemeDialog = true }
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        item {
                            ExpressiveSwitchPreference(
                                title = stringResource(R.string.dynamic_color_option),
                                description = stringResource(R.string.dynamic_color_desc),
                                icon = Icons.Rounded.AutoAwesome,
                                badgeColors = ExpressivePalette.PurpleLight,
                                checked = dynamicColor,
                                onCheckedChange = { enabled ->
                                    scope.launch { preferenceManager.setDynamicColor(enabled) }
                                }
                            )
                        }
                    }

                    if (!dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        item {
                            ExpressivePreferenceItem(
                                title = stringResource(R.string.accent_color_option),
                                description = when (themeAccentColor) {
                                    "ocean" -> stringResource(R.string.color_ocean)
                                    "violet" -> stringResource(R.string.color_violet)
                                    "amber" -> stringResource(R.string.color_amber)
                                    "rose" -> stringResource(R.string.color_rose)
                                    else -> stringResource(R.string.color_pistachio)
                                },
                                icon = Icons.Rounded.Palette,
                                badgeColors = ExpressivePalette.GreenLight,
                                onClick = { showAccentDialog = true }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.language_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )

                ExpressiveCardGroup {
                    item {
                        ExpressivePreferenceItem(
                            title = stringResource(R.string.language_option),
                            description = when (language) {
                                "tr" -> stringResource(R.string.lang_tr)
                                "en" -> stringResource(R.string.lang_en)
                                else -> stringResource(R.string.theme_system)
                            },
                            icon = Icons.Rounded.Language,
                            badgeColors = ExpressivePalette.OrangeLight,
                            onClick = { showLanguageDialog = true }
                        )
                    }
                }
            }

            item(key = "execution_mode_group_$shellMode") {
                Text(
                    text = stringResource(R.string.execution_mode),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )

                ExpressiveCardGroup {
                    item {
                        ExpressivePreferenceItem(
                            title = stringResource(R.string.execution_mode),
                            description = if (shellMode == "adb") {
                                stringResource(R.string.mode_swiftsense)
                            } else {
                                stringResource(R.string.mode_shizuku)
                            },
                            icon = Icons.Rounded.Terminal,
                            badgeColors = ExpressivePalette.PurpleLight,
                            onClick = { showShellModeDialog = true }
                        )
                    }

                    if (shellMode == "adb") {
                        val isConnected = adbConnectionState is AdbConnectionState.Connected
                        val isConnecting = adbConnectionState is AdbConnectionState.Connecting
                        val connectedPort = (adbConnectionState as? AdbConnectionState.Connected)?.port ?: 0

                        item {
                            ExpressiveSwitchPreference(
                                title = stringResource(R.string.adb_service_auto_start),
                                description = stringResource(R.string.adb_service_auto_start_desc),
                                icon = Icons.Rounded.PlayArrow,
                                badgeColors = ExpressivePalette.TealLight,
                                checked = autoStartService,
                                onCheckedChange = { active ->
                                    scope.launch { preferenceManager.setSwiftSenseAutoStart(active) }
                                }
                            )
                        }

                        item {
                            ExpressivePreferenceItem(
                                title = if (hasPairedAdb) stringResource(R.string.adb_repair_pairing) else stringResource(R.string.adb_dialog_title),
                                description = when {
                                    isConnected -> stringResource(R.string.adb_connected, connectedPort)
                                    isConnecting -> stringResource(R.string.adb_status_connecting)
                                    hasPairedAdb -> stringResource(R.string.adb_status_paired)
                                    else -> stringResource(R.string.adb_auto_pairing_help)
                                },
                                icon = Icons.Rounded.Wifi,
                                badgeColors = when {
                                    isConnected -> ExpressivePalette.GreenLight
                                    isConnecting -> ExpressivePalette.AmberLight
                                    hasPairedAdb -> ExpressivePalette.BlueLight
                                    else -> ExpressivePalette.OrangeLight
                                },
                                onClick = {
                                    showAdbSetupDialog = true
                                }
                            )
                        }

                        item {
                            ExpressivePreferenceItem(
                                title = stringResource(R.string.adb_service_title),
                                description = if (isDaemonRunning) {
                                    stringResource(R.string.adb_service_running)
                                } else {
                                    stringResource(R.string.adb_service_action_start)
                                },
                                icon = if (isDaemonRunning) Icons.Rounded.CheckCircle else Icons.Rounded.PlayArrow,
                                badgeColors = if (isDaemonRunning) ExpressivePalette.GreenLight else ExpressivePalette.PurpleLight,
                                onClick = {
                                    if (isDaemonRunning) {
                                        showStopServiceDialog = true
                                    } else {
                                        scope.launch {
                                            if (isConnected) {
                                                SwiftSenseDaemonBridge.spawnDaemonViaAdb(context)
                                                kotlinx.coroutines.delay(600)
                                                isDaemonRunning = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    SwiftSenseDaemonBridge.isDaemonAlive()
                                                }
                                                io.github.itsmelissadev.swiftsense.service.shell.ShellBridge.refreshNow()
                                            } else {
                                                val res = AdbManager.connectAuto(
                                                    context,
                                                    preferenceManager.getAdbHostSync(),
                                                    preferenceManager.getAdbPortSync()
                                                )
                                                if (res.isSuccess) {
                                                    preferenceManager.setAdbPort(res.getOrThrow())
                                                    SwiftSenseDaemonBridge.spawnDaemonViaAdb(context)
                                                    kotlinx.coroutines.delay(600)
                                                    isDaemonRunning = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                        SwiftSenseDaemonBridge.isDaemonAlive()
                                                    }
                                                    io.github.itsmelissadev.swiftsense.service.shell.ShellBridge.refreshNow()
                                                } else {
                                                    showAdbSetupDialog = true
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                    } else {
                        item {
                            ExpressivePreferenceItem(
                                title = when {
                                    !isShizukuRunning -> stringResource(R.string.shizuku_not_running)
                                    !isShizukuAuthorized -> stringResource(R.string.shizuku_not_authorized)
                                    else -> stringResource(R.string.shizuku_authorized)
                                },
                                description = when {
                                    !isShizukuRunning -> stringResource(R.string.shizuku_not_running_desc)
                                    !isShizukuAuthorized -> stringResource(R.string.shizuku_not_authorized_desc)
                                    else -> stringResource(R.string.shizuku_authorized_desc)
                                },
                                icon = if (isShizukuRunning && isShizukuAuthorized) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                badgeColors = if (isShizukuRunning && isShizukuAuthorized) ExpressivePalette.GreenLight else ExpressivePalette.OrangeLight,
                                onClick = {
                                    if (isShizukuRunning && !isShizukuAuthorized) {
                                        rikka.shizuku.Shizuku.requestPermission(0)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.system_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )

                ExpressiveCardGroup {
                    item {
                        ExpressivePreferenceItem(
                            title = stringResource(R.string.check_updates_option),
                            description = stringResource(R.string.check_updates_desc),
                            icon = Icons.Rounded.SystemUpdate,
                            badgeColors = ExpressivePalette.CyanLight,
                            onClick = onNavigateToUpdates
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = stringResource(
                            R.string.app_version,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showThemeDialog) {
        ExpressiveSelectionDialog(
            title = stringResource(R.string.theme_option),
            options = listOf(
                "system" to stringResource(R.string.theme_system),
                "light" to stringResource(R.string.theme_light),
                "dark" to stringResource(R.string.theme_dark)
            ),
            selected = themeMode,
            onSelectedChange = { mode ->
                scope.launch { preferenceManager.setThemeMode(mode) }
            },
            onDismissRequest = { showThemeDialog = false }
        )
    }

    if (showAccentDialog) {
        ExpressiveSelectionDialog(
            title = stringResource(R.string.accent_color_option),
            options = listOf(
                "pistachio" to stringResource(R.string.color_pistachio),
                "ocean" to stringResource(R.string.color_ocean),
                "violet" to stringResource(R.string.color_violet),
                "amber" to stringResource(R.string.color_amber),
                "rose" to stringResource(R.string.color_rose)
            ),
            selected = themeAccentColor,
            onSelectedChange = { color ->
                scope.launch { preferenceManager.setThemeAccentColor(color) }
            },
            onDismissRequest = { showAccentDialog = false }
        )
    }

    if (showLanguageDialog) {
        ExpressiveSelectionDialog(
            title = stringResource(R.string.language_option),
            options = listOf(
                "system" to stringResource(R.string.theme_system),
                "en" to stringResource(R.string.lang_en),
                "tr" to stringResource(R.string.lang_tr)
            ),
            selected = language,
            onSelectedChange = { lang ->
                scope.launch { preferenceManager.setLanguage(lang) }
            },
            onDismissRequest = { showLanguageDialog = false }
        )
    }

    if (showShellModeDialog) {
        val options = listOf(
            "shizuku" to stringResource(R.string.mode_shizuku),
            "adb" to stringResource(R.string.mode_swiftsense)
        )
        ExpressiveSelectionDialog(
            title = stringResource(R.string.execution_mode),
            options = options,
            selected = shellMode,
            onSelectedChange = { mode ->
                scope.launch {
                    preferenceManager.setShellMode(mode)
                }
            },
            onDismissRequest = { showShellModeDialog = false }
        )
    }

    if (showAdbSetupDialog) {
        AdbConnectionDialog(
            preferenceManager = preferenceManager,
            onDismissRequest = { showAdbSetupDialog = false }
        )
    }

    if (showStopServiceDialog) {
        ExpressiveDialog(
            title = stringResource(R.string.adb_service_stop_title),
            onDismissRequest = { showStopServiceDialog = false },
            dismissButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { showStopServiceDialog = false },
                    isPrimary = false
                )
            },
            confirmButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.adb_service_stop_confirm),
                    isPrimary = true,
                    onClick = {
                        showStopServiceDialog = false
                        scope.launch {
                            android.util.Log.i("SettingsScreen", "User confirmed stopping SwiftSense daemon.")
                            val stopRes = SwiftSenseDaemonBridge.stopDaemon()
                            android.util.Log.i("SettingsScreen", "stopDaemon completed with: $stopRes")
                            kotlinx.coroutines.delay(400)
                            val alive = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                SwiftSenseDaemonBridge.isDaemonAlive()
                            }
                            android.util.Log.i("SettingsScreen", "Daemon post-stop check isAlive=$alive")
                            isDaemonRunning = alive
                            io.github.itsmelissadev.swiftsense.service.shell.ShellBridge.refreshNow()
                            if (alive || stopRes.isFailure) {
                                android.util.Log.e("SettingsScreen", "Failed to stop daemon! Error: ${stopRes.exceptionOrNull()?.message}")
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.adb_service_stop_failed),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
        ) {
            Text(
                text = stringResource(R.string.adb_service_stop_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
