package io.github.itsmelissadev.swiftsense.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.adb.AdbConnectionState
import io.github.itsmelissadev.swiftsense.service.adb.AdbManager
import io.github.itsmelissadev.swiftsense.service.adb.AdbPairingService

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.KeyboardType
import io.github.itsmelissadev.swiftsense.service.adb.SwiftSenseDaemonBridge
import kotlinx.coroutines.launch

@Composable
fun AdbConnectionDialog(
    preferenceManager: PreferenceManager,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val connectionState by AdbManager.connectionState.collectAsState()
    val hasPaired by preferenceManager.hasPairedAdb.collectAsState(initial = false)
    val savedPort by preferenceManager.adbPort.collectAsState(initial = 0)
    
    var portInput by remember(savedPort) { mutableStateOf(if (savedPort > 0) savedPort.toString() else "") }
    var forcePairingView by remember { mutableStateOf(!hasPaired) }
    var isConnectingDirectly by remember { mutableStateOf(false) }

    val showPairing = forcePairingView || !hasPaired

    ExpressiveDialog(
        onDismissRequest = onDismissRequest,
        title = if (showPairing) stringResource(R.string.adb_dialog_title) else stringResource(R.string.adb_enter_connect_port),
        dismissButton = {
            ExpressiveDialogButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismissRequest,
                isPrimary = false
            )
        },
        confirmButton = {
            if (showPairing) {
                ExpressiveDialogButton(
                    text = stringResource(R.string.adb_open_dev_options),
                    onClick = {
                        AdbPairingService.start(context)
                        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                        onDismissRequest()
                    },
                    isPrimary = true
                )
            } else {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_start_service),
                    enabled = portInput.isNotBlank() && portInput.toIntOrNull() != null && !isConnectingDirectly,
                    onClick = {
                        val port = portInput.toIntOrNull() ?: return@ExpressiveDialogButton
                        isConnectingDirectly = true
                        scope.launch {
                            val host = preferenceManager.getAdbHostSync()
                            val connRes = AdbManager.connect(host, port)
                            if (connRes.isSuccess) {
                                preferenceManager.setAdbPort(port)
                                preferenceManager.setShellMode("adb")
                                SwiftSenseDaemonBridge.spawnDaemonViaAdb(context)
                                io.github.itsmelissadev.swiftsense.service.shell.ShellBridge.refreshNow()
                                onDismissRequest()
                            }
                            isConnectingDirectly = false
                        }
                    },
                    isPrimary = true
                )
            }
        },
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!showPairing) {
                    Text(
                        text = stringResource(R.string.adb_enter_connect_port_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ExpressiveTextField(
                        value = portInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) portInput = it },
                        label = stringResource(R.string.adb_port_label),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    ExpressiveOutlinedButton(
                        text = stringResource(R.string.adb_open_dev_options),
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.AutoMirrored.Filled.OpenInNew
                    )

                    ExpressiveOutlinedButton(
                        text = stringResource(R.string.adb_repair_pairing),
                        onClick = { forcePairingView = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.adb_notif_searching_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.adb_auto_pairing_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.adb_auto_pairing_step1),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.adb_auto_pairing_step2),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.adb_auto_pairing_step3),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                }

                if (connectionState is AdbConnectionState.Connected) {
                    val port = (connectionState as AdbConnectionState.Connected).port
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.adb_connected, port),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    )
}
