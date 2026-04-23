package io.github.itsmelissadev.swiftsense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itsmelissadev.swiftsense.BuildConfig
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
import kotlinx.coroutines.launch

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
                        stringResource(R.string.settings_title).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
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
                Spacer(modifier = Modifier.height(48.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = stringResource(
                            R.string.app_version,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE
                        ).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}
