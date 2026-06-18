package io.github.itsmelissadev.swiftsense.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
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
import io.github.itsmelissadev.swiftsense.BuildConfig
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSectionHeader
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSelectionDialog
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTopAppBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferenceManager: PreferenceManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by preferenceManager.themeMode.collectAsState(initial = "system")
    val language by preferenceManager.language.collectAsState(initial = "system")

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SwiftSenseTopAppBar(
                title = stringResource(R.string.settings_title),
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SwiftSenseSectionHeader(
                    title = stringResource(R.string.appearance_header),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                FeatureCard(
                    title = stringResource(R.string.theme_option),
                    description = when (themeMode) {
                        "light" -> stringResource(R.string.theme_light)
                        "dark" -> stringResource(R.string.theme_dark)
                        else -> stringResource(R.string.theme_system)
                    },
                    icon = Icons.Rounded.ColorLens,
                    onClick = { showThemeDialog = true }
                )
            }

            item {
                SwiftSenseSectionHeader(
                    title = stringResource(R.string.language_header),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                FeatureCard(
                    title = stringResource(R.string.language_option),
                    description = when (language) {
                        "tr" -> stringResource(R.string.lang_tr)
                        "en" -> stringResource(R.string.lang_en)
                        else -> stringResource(R.string.theme_system)
                    },
                    icon = Icons.Rounded.Language,
                    onClick = { showLanguageDialog = true }
                )
            }

            item {
                SwiftSenseSectionHeader(
                    title = stringResource(R.string.system_header),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                FeatureCard(
                    title = stringResource(R.string.check_updates_option),
                    description = stringResource(R.string.check_updates_desc),
                    icon = Icons.Rounded.SystemUpdate,
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/itsmelissadev/SwiftSense/releases")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = stringResource(
                            R.string.app_version,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showThemeDialog) {
        SwiftSenseSelectionDialog(
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

    if (showLanguageDialog) {
        SwiftSenseSelectionDialog(
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
}


