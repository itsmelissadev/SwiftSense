package io.github.itsmelissadev.swiftsense.ui.components

import android.content.pm.PackageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.itsmelissadev.swiftsense.R
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku

@Composable
fun ShizukuStatusWidget() {
    val context = LocalContext.current
    var isInstalled by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var isAuthorized by remember { mutableStateOf(false) }

    fun updateStatus() {
        isInstalled = try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
        isRunning = Shizuku.pingBinder()
        isAuthorized = if (isRunning) Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED else false
    }

    LaunchedEffect(Unit) {
        while (true) {
            updateStatus()
            delay(2000)
        }
    }

    val (statusTitle, statusDesc, statusIcon, statusColor) = when {
        !isInstalled -> Quadruple(
            stringResource(R.string.shizuku_not_installed),
            stringResource(R.string.shizuku_not_installed_desc),
            Icons.Default.Error,
            MaterialTheme.colorScheme.error
        )
        !isRunning -> Quadruple(
            stringResource(R.string.shizuku_not_running),
            stringResource(R.string.shizuku_not_running_desc),
            Icons.Default.Warning,
            MaterialTheme.colorScheme.error
        )
        !isAuthorized -> Quadruple(
            stringResource(R.string.shizuku_not_authorized),
            stringResource(R.string.shizuku_not_authorized_desc),
            Icons.Default.Refresh,
            MaterialTheme.colorScheme.tertiary
        )
        else -> Quadruple(
            stringResource(R.string.shizuku_authorized),
            stringResource(R.string.shizuku_authorized_desc),
            Icons.Default.CheckCircle,
            MaterialTheme.colorScheme.primary
        )
    }

    FeatureCard(
        title = statusTitle,
        description = statusDesc,
        icon = statusIcon,
        containerColor = statusColor.copy(alpha = 0.1f),
        onClick = {
            if (isInstalled && isRunning && !isAuthorized) {
                Shizuku.requestPermission(0)
            }
        },
        trailingContent = {
            if (isInstalled && isRunning && !isAuthorized) {
                SwiftSenseButton(
                    text = stringResource(R.string.allow),
                    onClick = { Shizuku.requestPermission(0) }
                )
            }
        }
    )
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
