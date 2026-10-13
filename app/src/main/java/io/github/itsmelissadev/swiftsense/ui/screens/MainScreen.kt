package io.github.itsmelissadev.swiftsense.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveCardGroup
import io.github.itsmelissadev.swiftsense.ui.components.ExpressivePreferenceItem
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSearchBar
import io.github.itsmelissadev.swiftsense.ui.theme.ExpressivePalette

@Composable
fun MainScreen(
    onNavigateToBoostSensors: () -> Unit,
    onNavigateToAppManager: () -> Unit,
    onNavigateToScreenResolution: () -> Unit,
    onNavigateToAppStopper: () -> Unit,
    onNavigateToCacheCleaner: () -> Unit,
    onNavigateToSystemTables: () -> Unit,
    onNavigateToAmoledScreenProtect: () -> Unit,
    onNavigateToAlwaysOnDisplay: () -> Unit,
    onNavigateToScreenRecorder: () -> Unit,
    onNavigateToApkInstaller: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val boostSensorsTitle = stringResource(R.string.feature_boost_sensors)
    val boostSensorsDesc = stringResource(R.string.feature_boost_sensors_desc)
    val appStopperTitle = stringResource(R.string.feature_app_stopper)
    val appStopperDesc = stringResource(R.string.feature_app_stopper_desc)
    val cacheCleanerTitle = stringResource(R.string.feature_cache_cleaner)
    val cacheCleanerDesc = stringResource(R.string.feature_cache_cleaner_desc)

    val amoledTitle = stringResource(R.string.feature_amoled_protect)
    val amoledDesc = stringResource(R.string.feature_amoled_protect_desc)
    val aodTitle = stringResource(R.string.feature_always_on_display)
    val aodDesc = stringResource(R.string.feature_always_on_display_desc)
    val appManagerTitle = stringResource(R.string.feature_app_manager)
    val appManagerDesc = stringResource(R.string.feature_app_manager_desc)
    val screenResTitle = stringResource(R.string.feature_screen_resolution)
    val screenResDesc = stringResource(R.string.feature_screen_resolution_desc)
    val screenRecorderTitle = stringResource(R.string.feature_screen_recorder)
    val screenRecorderDesc = stringResource(R.string.feature_screen_recorder_desc)
    val systemTablesTitle = stringResource(R.string.feature_system_tables)
    val systemTablesDesc = stringResource(R.string.feature_system_tables_desc)
    val apkInstallerTitle = stringResource(R.string.feature_apk_installer)
    val apkInstallerDesc = stringResource(R.string.feature_apk_installer_desc)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ExpressiveSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = stringResource(R.string.search_settings),
                    trailingIcon = {
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                )
            }

            val matchesPerformance = listOf(
                boostSensorsTitle to boostSensorsDesc,
                appStopperTitle to appStopperDesc,
                cacheCleanerTitle to cacheCleanerDesc
            ).any { (title, desc) ->
                searchQuery.isEmpty() || title.contains(searchQuery, ignoreCase = true) || desc.contains(searchQuery, ignoreCase = true)
            }

            if (matchesPerformance) {
                item {
                    Text(
                        text = stringResource(R.string.category_performance),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )

                    ExpressiveCardGroup {
                        if (searchQuery.isEmpty() || boostSensorsTitle.contains(searchQuery, ignoreCase = true) || boostSensorsDesc.contains(searchQuery, ignoreCase = true)) {
                            item {
                                ExpressivePreferenceItem(
                                    title = boostSensorsTitle,
                                    description = boostSensorsDesc,
                                    icon = Icons.Default.Speed,
                                    badgeColors = ExpressivePalette.BlueLight,
                                    onClick = onNavigateToBoostSensors
                                )
                            }
                        }

                        if (searchQuery.isEmpty() || appStopperTitle.contains(searchQuery, ignoreCase = true) || appStopperDesc.contains(searchQuery, ignoreCase = true)) {
                            item {
                                ExpressivePreferenceItem(
                                    title = appStopperTitle,
                                    description = appStopperDesc,
                                    icon = Icons.Default.PowerSettingsNew,
                                    badgeColors = ExpressivePalette.AmberLight,
                                    onClick = onNavigateToAppStopper
                                )
                            }
                        }

                        if (searchQuery.isEmpty() || cacheCleanerTitle.contains(searchQuery, ignoreCase = true) || cacheCleanerDesc.contains(searchQuery, ignoreCase = true)) {
                            item {
                                ExpressivePreferenceItem(
                                    title = cacheCleanerTitle,
                                    description = cacheCleanerDesc,
                                    icon = Icons.Default.CleaningServices,
                                    badgeColors = ExpressivePalette.GreenLight,
                                    onClick = onNavigateToCacheCleaner
                                )
                            }
                        }
                    }
                }
            }

            val matchesTools = listOf(
                amoledTitle to amoledDesc,
                aodTitle to aodDesc,
                appManagerTitle to appManagerDesc,
                apkInstallerTitle to apkInstallerDesc,
                screenResTitle to screenResDesc,
                screenRecorderTitle to screenRecorderDesc,
                systemTablesTitle to systemTablesDesc
            ).any { (title, desc) ->
                searchQuery.isEmpty() || title.contains(searchQuery, ignoreCase = true) || desc.contains(searchQuery, ignoreCase = true)
            }

            if (matchesTools) {
                item {
                    Text(
                        text = stringResource(R.string.category_tools),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )

                    ExpressiveCardGroup {
                        if (searchQuery.isEmpty() || amoledTitle.contains(searchQuery, ignoreCase = true) || amoledDesc.contains(searchQuery, ignoreCase = true)) {
                            item {
                                ExpressivePreferenceItem(
                                    title = amoledTitle,
                                    description = amoledDesc,
                                    icon = Icons.Default.Shield,
                                    badgeColors = ExpressivePalette.CyanLight,
                                    onClick = onNavigateToAmoledScreenProtect
                                )
                            }
                        }

                        if (searchQuery.isEmpty() || aodTitle.contains(searchQuery, ignoreCase = true) || aodDesc.contains(searchQuery, ignoreCase = true)) {
                            item {
                                ExpressivePreferenceItem(
                                    title = aodTitle,
                                    description = aodDesc,
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_aod_24px),
                                    badgeColors = ExpressivePalette.PurpleLight,
                                    onClick = onNavigateToAlwaysOnDisplay
                                )
                            }
                        }

                        if (searchQuery.isEmpty() || appManagerTitle.contains(searchQuery, ignoreCase = true) || appManagerDesc.contains(searchQuery, ignoreCase = true)) {
                            item {
                                ExpressivePreferenceItem(
                                    title = appManagerTitle,
                                    description = appManagerDesc,
                                    icon = Icons.Default.Apps,
                                    badgeColors = ExpressivePalette.OrangeLight,
                                    onClick = onNavigateToAppManager
                                )
                            }
                        }

                        if (searchQuery.isEmpty() || apkInstallerTitle.contains(searchQuery, ignoreCase = true) || apkInstallerDesc.contains(searchQuery, ignoreCase = true)) {
                            item {
                                ExpressivePreferenceItem(
                                    title = apkInstallerTitle,
                                    description = apkInstallerDesc,
                                    icon = Icons.Default.Download,
                                    badgeColors = ExpressivePalette.GreenLight,
                                    onClick = onNavigateToApkInstaller
                                )
                            }
                        }

                        if (searchQuery.isEmpty() || screenResTitle.contains(searchQuery, ignoreCase = true) || screenResDesc.contains(searchQuery, ignoreCase = true)) {
                            item {
                                ExpressivePreferenceItem(
                                    title = screenResTitle,
                                    description = screenResDesc,
                                    icon = Icons.Default.AspectRatio,
                                    badgeColors = ExpressivePalette.TealLight,
                                    onClick = onNavigateToScreenResolution
                                )
                            }
                        }

                        if (searchQuery.isEmpty() || screenRecorderTitle.contains(searchQuery, ignoreCase = true) || screenRecorderDesc.contains(searchQuery, ignoreCase = true)) {
                            item {
                                ExpressivePreferenceItem(
                                    title = screenRecorderTitle,
                                    description = screenRecorderDesc,
                                    icon = Icons.Default.Videocam,
                                    badgeColors = ExpressivePalette.RedLight,
                                    onClick = onNavigateToScreenRecorder
                                )
                            }
                        }

                        if (searchQuery.isEmpty() || systemTablesTitle.contains(searchQuery, ignoreCase = true) || systemTablesDesc.contains(searchQuery, ignoreCase = true)) {
                            item {
                                ExpressivePreferenceItem(
                                    title = systemTablesTitle,
                                    description = systemTablesDesc,
                                    icon = Icons.Default.TableChart,
                                    badgeColors = ExpressivePalette.PurpleLight,
                                    onClick = onNavigateToSystemTables
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
