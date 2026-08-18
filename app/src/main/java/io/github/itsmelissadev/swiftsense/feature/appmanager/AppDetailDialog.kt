package io.github.itsmelissadev.swiftsense.feature.appmanager

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
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.service.shizuku.ShizukuShellRunner
import io.github.itsmelissadev.swiftsense.ui.components.ShadcnDialog
import io.github.itsmelissadev.swiftsense.ui.components.ShadcnDialogButton
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseButton
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseOutlinedButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppDetailDialog(
    app: RichAppInfo,
    isInActiveList: Boolean,
    isShizukuReady: Boolean,
    onToggleActiveList: () -> Unit,
    onStatusChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val clipboardManager = LocalClipboardManager.current
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

    ShadcnDialog(
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
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(app.packageName))
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
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (currentEnabledState) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        border = BorderStroke(
                            1.dp,
                            if (currentEnabledState) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                        )
                    ) {
                        Text(
                            text = if (currentEnabledState) stringResource(R.string.app_status_enabled)
                            else stringResource(R.string.app_status_disabled),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentEnabledState) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Text(
                            text = if (app.isSystem) stringResource(R.string.app_type_system)
                            else stringResource(R.string.app_type_user),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (app.targetSdkVersion > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        ) {
                            Text(
                                text = stringResource(R.string.target_sdk_info, app.targetSdkVersion),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (app.versionName.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.version_info, app.versionName, app.versionCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                if (isShizukuReady) {
                    SwiftSenseButton(
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
                                    ShizukuShellRunner.runCommand("pm $cmd ${app.packageName}")
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
                        SwiftSenseOutlinedButton(
                            text = stringResource(R.string.open_app),
                            icon = Icons.Default.OpenInNew,
                            onClick = {
                                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SwiftSenseOutlinedButton(
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

                SwiftSenseOutlinedButton(
                    text = if (isInActiveList) stringResource(R.string.remove_from_list)
                    else stringResource(R.string.add_to_list),
                    icon = if (isInActiveList) Icons.Default.Check else null,
                    onClick = { onToggleActiveList() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            ShadcnDialogButton(
                text = stringResource(R.string.action_cancel),
                isPrimary = false,
                onClick = onDismiss
            )
        }
    )
}
