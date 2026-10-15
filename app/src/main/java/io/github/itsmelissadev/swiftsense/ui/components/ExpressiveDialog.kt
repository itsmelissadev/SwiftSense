package io.github.itsmelissadev.swiftsense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.itsmelissadev.swiftsense.R

@Composable
fun ExpressiveDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp),
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable (() -> Unit)? = null
) {
    val currentContext = androidx.compose.ui.platform.LocalContext.current
    val currentConfig = androidx.compose.ui.platform.LocalConfiguration.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalContext provides currentContext,
            androidx.compose.ui.platform.LocalConfiguration provides currentConfig
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 560.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (description != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (content != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(contentPadding)
                    ) {
                        content()
                    }
                }

                if (confirmButton != null || dismissButton != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 24.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (confirmButton != null) {
                            confirmButton()
                        }
                    }
                } else {
                    if (content == null) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
}

@Composable
fun ExpressiveDialogButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean = true,
    enabled: Boolean = true
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun <T> ExpressiveSelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelectedChange: (T) -> Unit,
    onDismissRequest: () -> Unit
) {
    ExpressiveDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        contentPadding = PaddingValues(vertical = 8.dp),
        dismissButton = {
            ExpressiveDialogButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismissRequest,
                isPrimary = false
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { (value, label) ->
                    val isSelected = selected == value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectedChange(value)
                                onDismissRequest()
                            }
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onSelectedChange(value)
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        }
    )
}
