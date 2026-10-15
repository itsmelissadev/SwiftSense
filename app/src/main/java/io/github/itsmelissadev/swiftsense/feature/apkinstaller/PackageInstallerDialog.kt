package io.github.itsmelissadev.swiftsense.feature.apkinstaller

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.itsmelissadev.swiftsense.R
import kotlinx.coroutines.launch

sealed interface DialogInstallState {
    data object Staging : DialogInstallState
    data class Ready(val details: ApkPackageDetails) : DialogInstallState
    data class Installing(val details: ApkPackageDetails) : DialogInstallState
    data class Success(val details: ApkPackageDetails) : DialogInstallState
    data class Error(val details: ApkPackageDetails?, val message: String) : DialogInstallState
}

@Composable
fun PackageInstallerDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onOpenApp: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<DialogInstallState>(DialogInstallState.Staging) }

    LaunchedEffect(uri) {
        ApkInstallerHelper.cleanAllStagingDirs(context)
        val parseResult = ApkInstallerHelper.parsePackage(context, uri)
        if (parseResult.isSuccess) {
            state = DialogInstallState.Ready(parseResult.getOrThrow())
        } else {
            val rawErr = parseResult.exceptionOrNull()?.message
            val err = ApkInstallerHelper.getLocalizedInstallError(context, rawErr)
            state = DialogInstallState.Error(null, err)
        }
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .heightIn(max = 640.dp)
            .wrapContentHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            when (val currentState = state) {
                is DialogInstallState.Staging -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.installer_dialog_staging),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is DialogInstallState.Ready -> {
                    val details = currentState.details
                    val installedInfo = details.installedAppInfo
                    val updateType = installedInfo?.updateType ?: UpdateType.NEW_INSTALL

                    var showAdvancedOptions by remember { mutableStateOf(false) }
                    var pendingSecurityRisk by remember { mutableStateOf<ApkSecurityRisk?>(null) }
                    var options by remember(details) {
                        mutableStateOf(
                            ApkInstallOptions(
                                reinstall = true,
                                allowDowngrade = updateType == UpdateType.DOWNGRADE,
                                bypassLowSdkBlock = true,
                                allowTestOnly = true,
                                grantRuntimePermissions = false,
                                skipVerification = false
                            )
                        )
                    }

                    PackageDialogHeader(
                        label = details.label,
                        packageName = details.packageName,
                        formattedSize = details.formattedSize,
                        icon = details.icon
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    when (updateType) {
                        UpdateType.NEW_INSTALL -> {
                            Text(
                                text = stringResource(R.string.installer_dialog_prompt_install),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.apk_version, details.versionName, details.versionCode),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        UpdateType.UPDATE -> {
                            Text(
                                text = stringResource(R.string.installer_dialog_prompt_update),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.installer_dialog_version_transition,
                                        installedInfo?.versionName ?: "",
                                        details.versionName
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        UpdateType.DOWNGRADE -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.installer_dialog_prompt_downgrade),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = stringResource(
                                                R.string.installer_dialog_version_transition,
                                                "${installedInfo?.versionName} (${installedInfo?.versionCode})",
                                                "${details.versionName} (${details.versionCode})"
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        UpdateType.SAME_VERSION -> {
                            Text(
                                text = stringResource(R.string.installer_dialog_prompt_reinstall),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${details.versionName} (${details.versionCode})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (details.certMatch == false) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.installer_dialog_cert_mismatch_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showAdvancedOptions = !showAdvancedOptions }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.installer_dialog_advanced_options),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = if (showAdvancedOptions) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (showAdvancedOptions) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DialogOptionRow(
                                title = stringResource(R.string.apk_reinstall_option),

                                description = stringResource(R.string.apk_reinstall_desc),
                                checked = options.reinstall,
                                onCheckedChange = { options = options.copy(reinstall = it) }
                            )
                            DialogOptionRow(
                                title = stringResource(R.string.apk_downgrade_option),
                                description = stringResource(R.string.apk_downgrade_desc),
                                checked = options.allowDowngrade,
                                onCheckedChange = { options = options.copy(allowDowngrade = it) }
                            )
                            DialogOptionRow(
                                title = stringResource(R.string.apk_bypass_sdk_option),
                                description = stringResource(R.string.apk_bypass_sdk_desc),
                                checked = options.bypassLowSdkBlock,
                                onCheckedChange = { options = options.copy(bypassLowSdkBlock = it) }
                            )
                            DialogOptionRow(
                                title = stringResource(R.string.apk_grant_perms_option),
                                description = stringResource(R.string.apk_grant_perms_desc),
                                checked = options.grantRuntimePermissions,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        pendingSecurityRisk = ApkSecurityRisk.GRANT_PERMISSIONS
                                    } else {
                                        options = options.copy(grantRuntimePermissions = false)
                                    }
                                }
                            )
                            DialogOptionRow(
                                title = stringResource(R.string.apk_test_only_option),
                                description = stringResource(R.string.apk_test_only_desc),
                                checked = options.allowTestOnly,
                                onCheckedChange = { options = options.copy(allowTestOnly = it) }
                            )
                            DialogOptionRow(
                                title = stringResource(R.string.apk_skip_verification_option),
                                description = stringResource(R.string.apk_skip_verification_desc),
                                checked = options.skipVerification,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        pendingSecurityRisk = ApkSecurityRisk.SKIP_VERIFICATION
                                    } else {
                                        options = options.copy(skipVerification = false)
                                    }
                                }
                            )
                            DialogOptionRow(
                                title = stringResource(R.string.apk_user_profile_option),
                                description = stringResource(R.string.apk_user_profile_desc),
                                checked = options.targetUserId == null,
                                onCheckedChange = { installForAll ->
                                    options = options.copy(targetUserId = if (installForAll) null else 0)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val actionText = when (updateType) {
                        UpdateType.NEW_INSTALL -> stringResource(R.string.installer_dialog_action_install)
                        UpdateType.UPDATE -> stringResource(R.string.installer_dialog_action_update)
                        UpdateType.DOWNGRADE -> stringResource(R.string.installer_dialog_action_downgrade)
                        UpdateType.SAME_VERSION -> stringResource(R.string.installer_dialog_action_reinstall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.installer_dialog_action_cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                state = DialogInstallState.Installing(details)
                                scope.launch {
                                    val res = ApkInstallerHelper.installPackage(context, details, options)
                                    if (res.isSuccess) {
                                        state = DialogInstallState.Success(details)
                                    } else {
                                        val rawErr = res.exceptionOrNull()?.message
                                        val err = ApkInstallerHelper.getLocalizedInstallError(context, rawErr)
                                        state = DialogInstallState.Error(details, err)
                                    }
                                }
                            },
                            colors = if (options.allowDowngrade || updateType == UpdateType.DOWNGRADE) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Text(actionText)
                        }
                    }

                    if (pendingSecurityRisk != null) {
                        ApkSecurityWarningDialog(
                            risk = pendingSecurityRisk!!,
                            onConfirm = {
                                val r = pendingSecurityRisk
                                pendingSecurityRisk = null
                                if (r == ApkSecurityRisk.GRANT_PERMISSIONS) {
                                    options = options.copy(grantRuntimePermissions = true)
                                } else if (r == ApkSecurityRisk.SKIP_VERIFICATION) {
                                    options = options.copy(skipVerification = true)
                                }
                            },
                            onDismiss = { pendingSecurityRisk = null }
                        )
                    }
                }

                is DialogInstallState.Installing -> {
                    val details = currentState.details

                    PackageDialogHeader(
                        label = details.label,
                        packageName = details.packageName,
                        formattedSize = details.formattedSize,
                        icon = details.icon
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.installer_dialog_installing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {}, enabled = false) {
                            Text(stringResource(R.string.installer_dialog_action_cancel))
                        }
                    }
                }

                is DialogInstallState.Success -> {
                    val details = currentState.details

                    PackageDialogHeader(
                        label = details.label,
                        packageName = details.packageName,
                        formattedSize = details.formattedSize,
                        icon = details.icon
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.installer_dialog_installed),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val canLaunch = context.packageManager.getLaunchIntentForPackage(details.packageName) != null

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.installer_dialog_action_done))
                        }
                        if (canLaunch) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { onOpenApp(details.packageName) }) {
                                Text(stringResource(R.string.installer_dialog_action_open))
                            }
                        }
                    }
                }

                is DialogInstallState.Error -> {
                    if (currentState.details != null) {
                        PackageDialogHeader(
                            label = currentState.details.label,
                            packageName = currentState.details.packageName,
                            formattedSize = currentState.details.formattedSize,
                            icon = currentState.details.icon
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.apk_install_failed_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.installer_dialog_not_installed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onDismiss) {
                            Text(stringResource(R.string.installer_dialog_action_done))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogOptionRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PackageDialogHeader(
    label: String,
    packageName: String,
    formattedSize: String,
    icon: Drawable?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(
                bitmap = icon.toBitmap(128, 128).asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$packageName • $formattedSize",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ApkSecurityWarningDialog(
    risk: ApkSecurityRisk,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(
                    if (risk == ApkSecurityRisk.GRANT_PERMISSIONS) {
                        R.string.apk_warn_grant_perms_title
                    } else {
                        R.string.apk_warn_skip_verification_title
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = stringResource(
                    if (risk == ApkSecurityRisk.GRANT_PERMISSIONS) {
                        R.string.apk_warn_grant_perms_message
                    } else {
                        R.string.apk_warn_skip_verification_message
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.apk_action_accept_risk))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
