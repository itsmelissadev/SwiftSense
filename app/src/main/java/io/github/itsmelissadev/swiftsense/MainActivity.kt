package io.github.itsmelissadev.swiftsense

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.feature.amoledscreenprotect.AmoledScreenProtectScreen
import io.github.itsmelissadev.swiftsense.feature.appmanager.AppManagerScreen
import io.github.itsmelissadev.swiftsense.feature.appstopper.AppStopperScreen
import io.github.itsmelissadev.swiftsense.feature.boostsensors.BoostSensorsScreen
import io.github.itsmelissadev.swiftsense.feature.cachecleaner.CacheCleanerScreen
import io.github.itsmelissadev.swiftsense.feature.screenresolution.ScreenResolutionScreen
import io.github.itsmelissadev.swiftsense.feature.systemtables.SystemTableMacroScreen
import io.github.itsmelissadev.swiftsense.ui.screens.MainScreen
import io.github.itsmelissadev.swiftsense.ui.screens.OnboardingScreen
import io.github.itsmelissadev.swiftsense.ui.screens.PermissionScreen
import io.github.itsmelissadev.swiftsense.ui.screens.SettingsScreen
import io.github.itsmelissadev.swiftsense.ui.theme.SwiftSenseTheme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        preferenceManager = PreferenceManager(this)
        enableEdgeToEdge()
        
        setContent {
            val isOnboardingCompleted by preferenceManager.isOnboardingCompleted.collectAsState(initial = null)
            val themeMode by preferenceManager.themeMode.collectAsState(initial = "system")
            val language by preferenceManager.language.collectAsState(initial = "system")
            
            val scope = rememberCoroutineScope()
            val navController = rememberNavController()
            val context = LocalContext.current
            
            val configuration = LocalConfiguration.current
            val localizedConfiguration = remember(language, configuration) {
                val config = Configuration(configuration)
                val locale = if (language == "system") {
                    configuration.locales[0] ?: Locale.getDefault()
                } else {
                    Locale.forLanguageTag(language)
                }
                config.setLocale(locale)
                config
            }
            val localizedContext = remember(localizedConfiguration) {
                context.createConfigurationContext(localizedConfiguration)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
                LocalActivityResultRegistryOwner provides this
            ) {
                SwiftSenseTheme(themeMode = themeMode) {
                    if (isOnboardingCompleted != null) {
                        val startDestination = remember(isOnboardingCompleted) {
                            when {
                                isOnboardingCompleted == false -> "onboarding"
                                !checkAllPermissions(context) -> "permissions"
                                else -> "main"
                            }
                        }
                        
                        NavHost(navController = navController, startDestination = startDestination) {
                            composable("onboarding") {
                                OnboardingScreen(onFinished = {
                                    scope.launch {
                                        preferenceManager.setOnboardingCompleted(true)
                                        if (!checkAllPermissions(context)) {
                                            navController.navigate("permissions") {
                                                popUpTo("onboarding") { inclusive = true }
                                            }
                                        } else {
                                            navController.navigate("main") {
                                                popUpTo("onboarding") { inclusive = true }
                                            }
                                        }
                                    }
                                })
                            }
                            composable("permissions") {
                                PermissionScreen(onAllPermissionsGranted = {
                                    navController.navigate("main") {
                                        popUpTo("permissions") { inclusive = true }
                                    }
                                })
                            }
                            composable("main") {
                                MainScreen(
                                    onNavigateToBoostSensors = { navController.navigate("boost_sensors") },
                                    onNavigateToAppManager = { navController.navigate("app_manager") },
                                    onNavigateToScreenResolution = { navController.navigate("screen_resolution") },
                                    onNavigateToAppStopper = { navController.navigate("app_stopper") },
                                    onNavigateToCacheCleaner = { navController.navigate("cache_cleaner") },
                                    onNavigateToSystemTables = { navController.navigate("system_tables") },
                                    onNavigateToAmoledScreenProtect = { navController.navigate("amoled_screen_protect") },
                                    onNavigateToSettings = { navController.navigate("settings") }
                                )
                            }
                            composable("boost_sensors") {
                                BoostSensorsScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable("app_manager") {
                                AppManagerScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable("screen_resolution") {
                                ScreenResolutionScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable("app_stopper") {
                                AppStopperScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable("cache_cleaner") {
                                CacheCleanerScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable("system_tables") {
                                SystemTableMacroScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable("amoled_screen_protect") {
                                AmoledScreenProtectScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable("settings") {
                                SettingsScreen(
                                    preferenceManager = preferenceManager,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAllPermissions(context: Context): Boolean {
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        
        val batteryIgnored = isBatteryOptimizationDisabled(context)
        return notificationGranted && batteryIgnored
    }

    private fun isBatteryOptimizationDisabled(context: Context): Boolean {
        val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}
