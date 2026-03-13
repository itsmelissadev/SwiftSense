package io.github.itsmelissadev.swiftsense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToBoostSensors: () -> Unit,
    onNavigateToAppManager: () -> Unit,
    onNavigateToScreenResolution: () -> Unit,
    onNavigateToAppStopper: () -> Unit,
    onNavigateToCacheCleaner: () -> Unit,
    onNavigateToSystemTables: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Text(
                    text = stringResource(R.string.category_performance),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                FeatureCard(
                    title = stringResource(R.string.feature_boost_sensors),
                    description = stringResource(R.string.feature_boost_sensors_desc),
                    icon = Icons.Default.Speed,
                    onClick = onNavigateToBoostSensors
                )
            }

            item {
                FeatureCard(
                    title = stringResource(R.string.feature_app_stopper),
                    description = stringResource(R.string.feature_app_stopper_desc),
                    icon = Icons.Default.PowerSettingsNew,
                    onClick = onNavigateToAppStopper
                )
            }

            item {
                FeatureCard(
                    title = stringResource(R.string.feature_cache_cleaner),
                    description = stringResource(R.string.feature_cache_cleaner_desc),
                    icon = Icons.Default.CleaningServices,
                    onClick = onNavigateToCacheCleaner
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                Text(
                    text = stringResource(R.string.category_tools),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                FeatureCard(
                    title = stringResource(R.string.feature_app_manager),
                    description = stringResource(R.string.feature_app_manager_desc),
                    icon = Icons.Default.Apps,
                    onClick = onNavigateToAppManager
                )
            }

            item {
                FeatureCard(
                    title = stringResource(R.string.feature_screen_resolution),
                    description = stringResource(R.string.feature_screen_resolution_desc),
                    icon = Icons.Default.AspectRatio,
                    onClick = onNavigateToScreenResolution
                )
            }

            item {
                FeatureCard(
                    title = stringResource(R.string.feature_system_tables),
                    description = stringResource(R.string.feature_system_tables_desc),
                    icon = Icons.Default.TableChart,
                    onClick = onNavigateToSystemTables
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
