package io.github.itsmelissadev.swiftsense.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mainScreen(
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
                    Column {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp).size(18.dp),
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                categorySection(
                    title = stringResource(R.string.category_performance),
                    icon = Icons.Default.Bolt
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureCard(
                            title = stringResource(R.string.feature_boost_sensors),
                            description = stringResource(R.string.feature_boost_sensors_desc),
                            icon = Icons.Default.Speed,
                            onClick = onNavigateToBoostSensors
                        )
                        FeatureCard(
                            title = stringResource(R.string.feature_app_stopper),
                            description = stringResource(R.string.feature_app_stopper_desc),
                            icon = Icons.Default.PowerSettingsNew,
                            onClick = onNavigateToAppStopper
                        )
                        FeatureCard(
                            title = stringResource(R.string.feature_cache_cleaner),
                            description = stringResource(R.string.feature_cache_cleaner_desc),
                            icon = Icons.Default.CleaningServices,
                            onClick = onNavigateToCacheCleaner
                        )
                    }
                }
            }

            // Tools Section
            item {
                categorySection(
                    title = stringResource(R.string.category_tools),
                    icon = Icons.Default.Build
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureCard(
                            title = stringResource(R.string.feature_amoled_protect),
                            description = stringResource(R.string.feature_amoled_protect_desc),
                            icon = Icons.Default.Shield,
                            onClick = onNavigateToAmoledScreenProtect
                        )
                        FeatureCard(
                            title = stringResource(R.string.feature_app_manager),
                            description = stringResource(R.string.feature_app_manager_desc),
                            icon = Icons.Default.Apps,
                            onClick = onNavigateToAppManager
                        )
                        FeatureCard(
                            title = stringResource(R.string.feature_screen_resolution),
                            description = stringResource(R.string.feature_screen_resolution_desc),
                            icon = Icons.Default.AspectRatio,
                            onClick = onNavigateToScreenResolution
                        )
                        FeatureCard(
                            title = stringResource(R.string.feature_system_tables),
                            description = stringResource(R.string.feature_system_tables_desc),
                            icon = Icons.Default.TableChart,
                            onClick = onNavigateToSystemTables
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun categorySection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp
            )
        }
        content()
    }
}
