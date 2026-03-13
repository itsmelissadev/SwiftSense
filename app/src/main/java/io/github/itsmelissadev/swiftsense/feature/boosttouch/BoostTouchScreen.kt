package io.github.itsmelissadev.swiftsense.feature.boosttouch

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoostTouchScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val scope = rememberCoroutineScope()
    val touchBoostEnabled by preferenceManager.touchBoostEnabled.collectAsState(initial = false)

    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.feature_boost_touch),
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FeatureCard(
                    title = stringResource(R.string.accessibility_service_status),
                    description = if (isAccessibilityEnabled) stringResource(R.string.accessibility_service_on)
                    else stringResource(R.string.accessibility_service_off),
                    icon = if (isAccessibilityEnabled) Icons.Rounded.VerifiedUser else Icons.Rounded.GppMaybe,
                    containerColor = if (isAccessibilityEnabled)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    trailingContent = {
                        if (!isAccessibilityEnabled) {
                            Button(
                                onClick = {
                                    val intent =
                                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    context.startActivity(intent)
                                },
                                shape = MaterialTheme.shapes.large,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(
                                    stringResource(R.string.setup_accessibility),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = CircleShape
                            ) {
                                Icon(
                                    Icons.Rounded.DoneAll,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp).size(20.dp)
                                )
                            }
                        }
                    }
                )
            }

            item {
                Text(
                    text = "Swift Settings",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                FeatureCard(
                    title = stringResource(R.string.touch_boost_title),
                    description = stringResource(R.string.touch_boost_desc),
                    icon = if (touchBoostEnabled) Icons.Rounded.AdsClick else Icons.Rounded.TouchApp,
                    checked = touchBoostEnabled,
                    onCheckedChange = { scope.launch { preferenceManager.setTouchBoostEnabled(it) } },
                    enabled = isAccessibilityEnabled,
                    containerColor = if (touchBoostEnabled && isAccessibilityEnabled)
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.surface
                )
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.touch_boost_info_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName =
        "${context.packageName}/io.github.itsmelissadev.swiftsense.feature.boosttouch.BoostTouchService"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServices?.contains(expectedComponentName) == true
}