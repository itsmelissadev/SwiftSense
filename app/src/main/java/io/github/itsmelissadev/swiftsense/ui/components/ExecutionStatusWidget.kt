package io.github.itsmelissadev.swiftsense.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.adb.AdbManager
import io.github.itsmelissadev.swiftsense.service.adb.SwiftSenseDaemonBridge
import io.github.itsmelissadev.swiftsense.service.shell.BackendStatus
import io.github.itsmelissadev.swiftsense.service.shell.ShellBridge
import io.github.itsmelissadev.swiftsense.ui.theme.BadgeColors
import io.github.itsmelissadev.swiftsense.ui.theme.ExpressivePalette
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

private data class WidgetVisuals(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val palette: BadgeColors
)

@Composable
fun ExecutionStatusWidget(
    modifier: Modifier = Modifier,
    bottomSpacing: Dp = 16.dp
) {
    val status by ShellBridge.status.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferenceManager = remember { PreferenceManager(context) }
    var showAdbDialog by remember { mutableStateOf(false) }

    if (status is BackendStatus.Ready) {
        return
    }

    val (title, desc, icon, palette) = when (status) {
        BackendStatus.Ready -> WidgetVisuals("", "", Icons.Default.Warning, ExpressivePalette.GreenLight)
        BackendStatus.ShizukuNotInstalled -> WidgetVisuals(
            stringResource(R.string.shizuku_not_installed),
            stringResource(R.string.shizuku_not_installed_desc),
            Icons.Default.Error,
            ExpressivePalette.RedLight
        )
        BackendStatus.ShizukuNotRunning -> WidgetVisuals(
            stringResource(R.string.shizuku_not_running),
            stringResource(R.string.shizuku_not_running_desc),
            Icons.Default.Warning,
            ExpressivePalette.OrangeLight
        )
        BackendStatus.ShizukuUnauthorized -> WidgetVisuals(
            stringResource(R.string.shizuku_not_authorized),
            stringResource(R.string.shizuku_not_authorized_desc),
            Icons.Default.Refresh,
            ExpressivePalette.AmberLight
        )
        BackendStatus.SwiftSenseStopped -> WidgetVisuals(
            stringResource(R.string.swiftsense_service_not_running),
            stringResource(R.string.swiftsense_service_not_running_desc),
            Icons.Default.PlayArrow,
            ExpressivePalette.PurpleLight
        )
    }

    Column(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ExpressivePreferenceItem(
                    title = title,
                    description = desc,
                    icon = icon,
                    badgeColors = palette,
                    onClick = {
                        when (status) {
                            BackendStatus.ShizukuUnauthorized -> {
                                try {
                                    Shizuku.requestPermission(0)
                                } catch (_: Exception) {
                                }
                            }
                            BackendStatus.SwiftSenseStopped -> {
                                scope.launch {
                                    val res = AdbManager.connectAuto(
                                        context,
                                        preferenceManager.getAdbHostSync(),
                                        preferenceManager.getAdbPortSync()
                                    )
                                    if (res.isSuccess) {
                                        preferenceManager.setAdbPort(res.getOrThrow())
                                        SwiftSenseDaemonBridge.spawnDaemonViaAdb(context)
                                        kotlinx.coroutines.delay(600)
                                        ShellBridge.refreshNow()
                                    } else {
                                        showAdbDialog = true
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                )
            }
        }
        if (bottomSpacing > 0.dp) {
            Spacer(modifier = Modifier.height(bottomSpacing))
        }
    }

    if (showAdbDialog) {
        AdbConnectionDialog(
            preferenceManager = preferenceManager,
            onDismissRequest = {
                showAdbDialog = false
                ShellBridge.refreshNow()
            }
        )
    }
}
