package io.github.itsmelissadev.swiftsense.ui.components

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.itsmelissadev.swiftsense.R
import rikka.shizuku.Shizuku
import kotlinx.coroutines.delay

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
        } catch (e: PackageManager.NameNotFoundException) {
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
            "Lütfen Play Store'dan Shizuku uygulamasını yükleyin.",
            Icons.Default.Error,
            MaterialTheme.colorScheme.error
        )
        !isRunning -> Quadruple(
            stringResource(R.string.shizuku_not_running),
            "Shizuku uygulaması yüklü ancak çalışmıyor.",
            Icons.Default.Warning,
            MaterialTheme.colorScheme.error
        )
        !isAuthorized -> Quadruple(
            stringResource(R.string.shizuku_not_authorized),
            "SwiftSense için Shizuku yetkisi verilmedi.",
            Icons.Default.Refresh,
            MaterialTheme.colorScheme.tertiary
        )
        else -> Quadruple(
            stringResource(R.string.shizuku_authorized),
            "Shizuku servisi aktif ve kullanıma hazır.",
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
                Button(onClick = { Shizuku.requestPermission(0) }) {
                    Text(stringResource(R.string.allow))
                }
            }
        }
    )
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
