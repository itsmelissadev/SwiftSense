package io.github.itsmelissadev.swiftsense.feature.apkinstaller

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.shell.ShellBridge
import io.github.itsmelissadev.swiftsense.ui.theme.SwiftSenseTheme
import java.util.Locale

class PackageInstallerDialogActivity : ComponentActivity() {

    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val uri = intent?.data ?: intent?.clipData?.getItemAt(0)?.uri
        if (uri == null) {
            finish()
            return
        }

        preferenceManager = PreferenceManager(this)
        ShellBridge.init(this)

        setContent {
            val themeMode by preferenceManager.themeMode.collectAsState(initial = "system")
            val dynamicColor by preferenceManager.dynamicColor.collectAsState(initial = false)
            val themeAccentColor by preferenceManager.themeAccentColor.collectAsState(initial = "pistachio")
            val language by preferenceManager.language.collectAsState(initial = "system")

            LaunchedEffect(language) {
                val config = Configuration(resources.configuration)
                val locale = if (language == "system") {
                    config.locales[0] ?: Locale.getDefault()
                } else {
                    Locale.forLanguageTag(language)
                }
                Locale.setDefault(locale)
                config.setLocale(locale)
                @Suppress("DEPRECATION")
                resources.updateConfiguration(config, resources.displayMetrics)
            }

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
            val context = LocalContext.current
            val localizedContext = remember(localizedConfiguration) {
                context.createConfigurationContext(localizedConfiguration)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration
            ) {
                SwiftSenseTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    themeAccentColor = themeAccentColor
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                finish()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PackageInstallerDialog(
                            uri = uri,
                            onDismiss = { finish() },
                            onOpenApp = { pkg ->
                                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)?.apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                if (launchIntent != null) {
                                    startActivity(launchIntent)
                                }
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}
