package io.github.itsmelissadev.swiftsense.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onNavigateToAmoledScreenProtect: () -> Unit,
    onNavigateToSettings: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Black
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.padding(end = 8.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Text(
                    text = stringResource(R.string.category_performance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
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

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Text(
                    text = stringResource(R.string.category_tools),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                FeatureCard(
                    title = stringResource(R.string.feature_amoled_protect),
                    description = stringResource(R.string.feature_amoled_protect_desc),
                    icon = Icons.Default.Shield,
                    onClick = onNavigateToAmoledScreenProtect
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

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
