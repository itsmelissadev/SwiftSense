package io.github.itsmelissadev.swiftsense.feature.appmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import io.github.itsmelissadev.swiftsense.service.shell.ShellBridge
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveDialog
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveDialogButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveOutlinedButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveTonalButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppDetailDialog(
    app: RichAppInfo,
    targetUserId: Int,
    isInActiveList: Boolean,
    isReady: Boolean,
    onToggleActiveList: () -> Unit,
    onStatusChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val scope = rememberCoroutineScope()

    var isActionInProgress by remember { mutableStateOf(false) }
    var currentEnabledState by remember(app.isEnabled) { mutableStateOf(app.isEnabled) }
    var appIcon by remember(app.packageName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(app.packageName) {
        withContext(Dispatchers.IO) {
            try {
                val icon = pm.getApplicationIcon(app.appInfo).toBitmap().asImageBitmap()
                appIcon = icon
            } catch (_: Exception) {
                appIcon = null
            }
        }
    }

    val canLaunch = remember(app.packageName) {
        pm.getLaunchIntentForPackage(app.packageName) != null
    }

    ExpressiveDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.app_details),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon!!,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.Android,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("package", app.packageName))
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.copied_to_clipboard),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentEnabledState) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = if (currentEnabledState) stringResource(R.string.app_status_enabled)
                            else stringResource(R.string.app_status_disabled),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (currentEnabledState) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            text = if (app.isSystem) stringResource(R.string.app_type_system)
                            else stringResource(R.string.app_type_user),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    if (app.targetSdkVersion > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = stringResource(R.string.target_sdk_info, app.targetSdkVersion),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (app.versionName.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.version_info, app.versionName, app.versionCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isReady) {
                    ExpressiveButton(
                        text = if (currentEnabledState) stringResource(R.string.action_disable)
                        else stringResource(R.string.action_enable),
                        icon = Icons.Default.PowerSettingsNew,
                        enabled = !isActionInProgress,
                        onClick = {
                            scope.launch {
                                isActionInProgress = true
                                val newState = !currentEnabledState
                                val cmd = if (newState) "enable" else "disable-user"
                                val result = withContext(Dispatchers.IO) {
                                    ShellBridge.run("pm $cmd --user $targetUserId ${app.packageName}")
                                }
                                if (result.isSuccess) {
                                    currentEnabledState = newState
                                    onStatusChanged(newState)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_app_status_updated),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_error_occurred),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                isActionInProgress = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (canLaunch) {
                        ExpressiveOutlinedButton(
                            text = stringResource(R.string.open_app),
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = {
                                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ExpressiveOutlinedButton(
                        text = stringResource(R.string.app_settings),
                        icon = Icons.Default.Settings,
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                ExpressiveTonalButton(
                    text = if (isInActiveList) stringResource(R.string.remove_from_list)
                    else stringResource(R.string.add_to_list),
                    icon = if (isInActiveList) Icons.Default.Check else null,
                    onClick = { onToggleActiveList() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            ExpressiveDialogButton(
                text = stringResource(R.string.action_cancel),
                isPrimary = false,
                onClick = onDismiss
            )
        }
    )
}
