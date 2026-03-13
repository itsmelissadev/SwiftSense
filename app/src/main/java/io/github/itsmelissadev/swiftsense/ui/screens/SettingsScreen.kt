package io.github.itsmelissadev.swiftsense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
import kotlinx.coroutines.launch
import io.github.itsmelissadev.swiftsense.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferenceManager: PreferenceManager,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val themeMode by preferenceManager.themeMode.collectAsState(initial = "system")
    val language by preferenceManager.language.collectAsState(initial = "system")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsHeader(title = stringResource(R.string.appearance_header))
                FeatureCard(
                    title = stringResource(R.string.theme_option),
                    description = when (themeMode) {
                        "light" -> stringResource(R.string.theme_light)
                        "dark" -> stringResource(R.string.theme_dark)
                        else -> stringResource(R.string.theme_system)
                    },
                    icon = Icons.Rounded.ColorLens,
                    onClick = {
                        val nextMode = when (themeMode) {
                            "system" -> "light"
                            "light" -> "dark"
                            else -> "system"
                        }
                        scope.launch { preferenceManager.setThemeMode(nextMode) }
                    }
                )
            }

            item {
                SettingsHeader(title = stringResource(R.string.language_header))
                FeatureCard(
                    title = stringResource(R.string.language_option),
                    description = when (language) {
                        "tr" -> stringResource(R.string.lang_tr)
                        "en" -> stringResource(R.string.lang_en)
                        else -> stringResource(R.string.theme_system)
                    },
                    icon = Icons.Rounded.Language,
                    onClick = {
                        val nextLang = when (language) {
                            "system" -> "en"
                            "en" -> "tr"
                            else -> "system"
                        }
                        scope.launch { preferenceManager.setLanguage(nextLang) }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}