package io.github.itsmelissadev.swiftsense.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val PistachioLightPrimary = Color(0xFF386A20)
val PistachioLightOnPrimary = Color(0xFFFFFFFF)
val PistachioLightPrimaryContainer = Color(0xFFB8F397)
val PistachioLightOnPrimaryContainer = Color(0xFF072100)
val PistachioLightSecondary = Color(0xFF55624C)
val PistachioLightOnSecondary = Color(0xFFFFFFFF)
val PistachioLightSecondaryContainer = Color(0xFFD9E7CB)
val PistachioLightOnSecondaryContainer = Color(0xFF131F0D)
val PistachioLightTertiary = Color(0xFF386666)
val PistachioLightOnTertiary = Color(0xFFFFFFFF)
val PistachioLightTertiaryContainer = Color(0xFFBBEBEB)
val PistachioLightOnTertiaryContainer = Color(0xFF002021)
val PistachioLightBackground = Color(0xFFF7FAF0)
val PistachioLightOnBackground = Color(0xFF181D15)
val PistachioLightSurface = Color(0xFFF7FAF0)
val PistachioLightOnSurface = Color(0xFF181D15)
val PistachioLightSurfaceVariant = Color(0xFFE0E5D8)
val PistachioLightOnSurfaceVariant = Color(0xFF43483E)
val PistachioLightOutline = Color(0xFF73796D)
val PistachioLightOutlineVariant = Color(0xFFC3C8BC)
val PistachioLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val PistachioLightSurfaceContainerLow = Color(0xFFF1F5EB)
val PistachioLightSurfaceContainer = Color(0xFFEBEFE5)
val PistachioLightSurfaceContainerHigh = Color(0xFFE5EAE0)
val PistachioLightSurfaceContainerHighest = Color(0xFFDFE4DA)

val PistachioDarkPrimary = Color(0xFF9DD67D)
val PistachioDarkOnPrimary = Color(0xFF123800)
val PistachioDarkPrimaryContainer = Color(0xFF235108)
val PistachioDarkOnPrimaryContainer = Color(0xFFB8F397)
val PistachioDarkSecondary = Color(0xFFBDCBB0)
val PistachioDarkOnSecondary = Color(0xFF283420)
val PistachioDarkSecondaryContainer = Color(0xFF3E4A35)
val PistachioDarkOnSecondaryContainer = Color(0xFFD9E7CB)
val PistachioDarkTertiary = Color(0xFFA0CFCF)
val PistachioDarkOnTertiary = Color(0xFF003738)
val PistachioDarkTertiaryContainer = Color(0xFF1F4E4E)
val PistachioDarkOnTertiaryContainer = Color(0xFFBBEBEB)
val PistachioDarkBackground = Color(0xFF11140E)
val PistachioDarkOnBackground = Color(0xFFDFE4DA)
val PistachioDarkSurface = Color(0xFF11140E)
val PistachioDarkOnSurface = Color(0xFFDFE4DA)
val PistachioDarkSurfaceVariant = Color(0xFF43483E)
val PistachioDarkOnSurfaceVariant = Color(0xFFC3C8BC)
val PistachioDarkOutline = Color(0xFF8D9387)
val PistachioDarkOutlineVariant = Color(0xFF43483E)
val PistachioDarkSurfaceContainerLowest = Color(0xFF0B0F09)
val PistachioDarkSurfaceContainerLow = Color(0xFF181D15)
val PistachioDarkSurfaceContainer = Color(0xFF1D2119)
val PistachioDarkSurfaceContainerHigh = Color(0xFF272C23)
val PistachioDarkSurfaceContainerHighest = Color(0xFF32372E)

val OceanLightPrimary = Color(0xFF006590)
val OceanLightPrimaryContainer = Color(0xFFC8E6FF)
val OceanLightBackground = Color(0xFFF7F9FF)
val OceanLightSurfaceContainer = Color(0xFFE7EEFA)

val OceanDarkPrimary = Color(0xFF87CEFF)
val OceanDarkPrimaryContainer = Color(0xFF004C6E)
val OceanDarkBackground = Color(0xFF0E141B)
val OceanDarkSurfaceContainer = Color(0xFF1A2129)

val VioletLightPrimary = Color(0xFF6B4EA2)
val VioletLightPrimaryContainer = Color(0xFFECDCFF)
val VioletLightBackground = Color(0xFFFAF7FF)
val VioletLightSurfaceContainer = Color(0xFFEEE7F6)

val VioletDarkPrimary = Color(0xFFD6BAFF)
val VioletDarkPrimaryContainer = Color(0xFF523688)
val VioletDarkBackground = Color(0xFF15111B)
val VioletDarkSurfaceContainer = Color(0xFF221D2A)

val AmberLightPrimary = Color(0xFF865300)
val AmberLightPrimaryContainer = Color(0xFFFFDDB6)
val AmberLightBackground = Color(0xFFFFF8F4)
val AmberLightSurfaceContainer = Color(0xFFF6ECE2)

val AmberDarkPrimary = Color(0xFFFFB86B)
val AmberDarkPrimaryContainer = Color(0xFF663E00)
val AmberDarkBackground = Color(0xFF18120B)
val AmberDarkSurfaceContainer = Color(0xFF272016)

val RoseLightPrimary = Color(0xFF904A5D)
val RoseLightPrimaryContainer = Color(0xFFFFD9E1)
val RoseLightBackground = Color(0xFFFFF8F8)
val RoseLightSurfaceContainer = Color(0xFFF6E8EA)

val RoseDarkPrimary = Color(0xFFFFAFD0)
val RoseDarkPrimaryContainer = Color(0xFF723345)
val RoseDarkBackground = Color(0xFF191013)
val RoseDarkSurfaceContainer = Color(0xFF291D22)

val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF410002)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val Success = Color(0xFF22c55e)
val Warning = Color(0xFFf59e0b)

data class BadgeColors(val container: Color, val content: Color)

object ExpressivePalette {
    val BlueLight = BadgeColors(Color(0xFFD4E3FF), Color(0xFF004785))
    val BlueDark = BadgeColors(Color(0xFF00315F), Color(0xFFA6C8FF))

    val CyanLight = BadgeColors(Color(0xFFC2ECF3), Color(0xFF004F56))
    val CyanDark = BadgeColors(Color(0xFF00373C), Color(0xFF82D5E0))

    val GreenLight = BadgeColors(Color(0xFFC7EFA8), Color(0xFF28500F))
    val GreenDark = BadgeColors(Color(0xFF1E3A08), Color(0xFFA6D686))

    val OrangeLight = BadgeColors(Color(0xFFFFDBCC), Color(0xFF7B3200))
    val OrangeDark = BadgeColors(Color(0xFF532000), Color(0xFFFFB68E))

    val AmberLight = BadgeColors(Color(0xFFFFDF9B), Color(0xFF5C4300))
    val AmberDark = BadgeColors(Color(0xFF422F00), Color(0xFFFFE087))

    val PurpleLight = BadgeColors(Color(0xFFECD9FF), Color(0xFF543384))
    val PurpleDark = BadgeColors(Color(0xFF3C1B6B), Color(0xFFD9B9FF))

    val PinkLight = BadgeColors(Color(0xFFFFD8E6), Color(0xFF74284A))
    val PinkDark = BadgeColors(Color(0xFF541031), Color(0xFFFFAFD2))

    val RedLight = BadgeColors(Color(0xFFFFDAD6), Color(0xFF93000A))
    val RedDark = BadgeColors(Color(0xFF690005), Color(0xFFFFB4AB))

    val TealLight = BadgeColors(Color(0xFFBCEBE5), Color(0xFF004F4A))
    val TealDark = BadgeColors(Color(0xFF003733), Color(0xFF7CD4CB))
}

// Ocean Light
val OceanLightOnPrimary = Color(0xFFFFFFFF)
val OceanLightOnPrimaryContainer = Color(0xFF001E2F)
val OceanLightSecondary = Color(0xFF4F606E)
val OceanLightOnSecondary = Color(0xFFFFFFFF)
val OceanLightSecondaryContainer = Color(0xFFD2E5F5)
val OceanLightOnSecondaryContainer = Color(0xFF0B1D29)
val OceanLightTertiary = Color(0xFF63597C)
val OceanLightOnTertiary = Color(0xFFFFFFFF)
val OceanLightTertiaryContainer = Color(0xFFE9DDFF)
val OceanLightOnTertiaryContainer = Color(0xFF1F1635)
val OceanLightOnBackground = Color(0xFF181C20)
val OceanLightSurface = Color(0xFFF7F9FF)
val OceanLightOnSurface = Color(0xFF181C20)
val OceanLightSurfaceVariant = Color(0xFFDDE3EA)
val OceanLightOnSurfaceVariant = Color(0xFF41474D)
val OceanLightOutline = Color(0xFF71787E)
val OceanLightOutlineVariant = Color(0xFFC1C7CE)
val OceanLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val OceanLightSurfaceContainerLow = Color(0xFFF1F4FA)
val OceanLightSurfaceContainerHigh = Color(0xFFE2E8F4)
val OceanLightSurfaceContainerHighest = Color(0xFFDCE2EE)

// Ocean Dark
val OceanDarkOnPrimary = Color(0xFF00344D)
val OceanDarkOnPrimaryContainer = Color(0xFFC8E6FF)
val OceanDarkSecondary = Color(0xFFB6C9D8)
val OceanDarkOnSecondary = Color(0xFF21323F)
val OceanDarkSecondaryContainer = Color(0xFF384956)
val OceanDarkOnSecondaryContainer = Color(0xFFD2E5F5)
val OceanDarkTertiary = Color(0xFFCDC0E9)
val OceanDarkOnTertiary = Color(0xFF342B4B)
val OceanDarkTertiaryContainer = Color(0xFF4B4163)
val OceanDarkOnTertiaryContainer = Color(0xFFE9DDFF)
val OceanDarkOnBackground = Color(0xFFE0E3E8)
val OceanDarkSurface = Color(0xFF0E141B)
val OceanDarkOnSurface = Color(0xFFE0E3E8)
val OceanDarkSurfaceVariant = Color(0xFF41474D)
val OceanDarkOnSurfaceVariant = Color(0xFFC1C7CE)
val OceanDarkOutline = Color(0xFF8B9198)
val OceanDarkOutlineVariant = Color(0xFF41474D)
val OceanDarkSurfaceContainerLowest = Color(0xFF090F16)
val OceanDarkSurfaceContainerLow = Color(0xFF161C24)
val OceanDarkSurfaceContainerHigh = Color(0xFF242C35)
val OceanDarkSurfaceContainerHighest = Color(0xFF2F3740)

// Violet Light
val VioletLightOnPrimary = Color(0xFFFFFFFF)
val VioletLightOnPrimaryContainer = Color(0xFF260057)
val VioletLightSecondary = Color(0xFF625B71)
val VioletLightOnSecondary = Color(0xFFFFFFFF)
val VioletLightSecondaryContainer = Color(0xFFE8DEF8)
val VioletLightOnSecondaryContainer = Color(0xFF1E192B)
val VioletLightTertiary = Color(0xFF7E5260)
val VioletLightOnTertiary = Color(0xFFFFFFFF)
val VioletLightTertiaryContainer = Color(0xFFFFD9E3)
val VioletLightOnTertiaryContainer = Color(0xFF31101D)
val VioletLightOnBackground = Color(0xFF1D1B20)
val VioletLightSurface = Color(0xFFFAF7FF)
val VioletLightOnSurface = Color(0xFF1D1B20)
val VioletLightSurfaceVariant = Color(0xFFE7E0EC)
val VioletLightOnSurfaceVariant = Color(0xFF49454E)
val VioletLightOutline = Color(0xFF7A757F)
val VioletLightOutlineVariant = Color(0xFFCAC4CF)
val VioletLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val VioletLightSurfaceContainerLow = Color(0xFFF5EEFD)
val VioletLightSurfaceContainerHigh = Color(0xFFE8E1F0)
val VioletLightSurfaceContainerHighest = Color(0xFFE2DBEA)

// Violet Dark
val VioletDarkOnPrimary = Color(0xFF3C1B71)
val VioletDarkOnPrimaryContainer = Color(0xFFECDCFF)
val VioletDarkSecondary = Color(0xFFCBC2DB)
val VioletDarkOnSecondary = Color(0xFF332D41)
val VioletDarkSecondaryContainer = Color(0xFF4A4458)
val VioletDarkOnSecondaryContainer = Color(0xFFE8DEF8)
val VioletDarkTertiary = Color(0xFFEFB8C8)
val VioletDarkOnTertiary = Color(0xFF4A2532)
val VioletDarkTertiaryContainer = Color(0xFF633B48)
val VioletDarkOnTertiaryContainer = Color(0xFFFFD9E3)
val VioletDarkOnBackground = Color(0xFFE6E0E9)
val VioletDarkSurface = Color(0xFF15111B)
val VioletDarkOnSurface = Color(0xFFE6E0E9)
val VioletDarkSurfaceVariant = Color(0xFF49454E)
val VioletDarkOnSurfaceVariant = Color(0xFFCAC4CF)
val VioletDarkOutline = Color(0xFF948F99)
val VioletDarkOutlineVariant = Color(0xFF49454E)
val VioletDarkSurfaceContainerLowest = Color(0xFF0F0B15)
val VioletDarkSurfaceContainerLow = Color(0xFF1E1924)
val VioletDarkSurfaceContainerHigh = Color(0xFF2C2735)
val VioletDarkSurfaceContainerHighest = Color(0xFF373240)

// Amber Light
val AmberLightOnPrimary = Color(0xFFFFFFFF)
val AmberLightOnPrimaryContainer = Color(0xFF2A1700)
val AmberLightSecondary = Color(0xFF6E5B40)
val AmberLightOnSecondary = Color(0xFFFFFFFF)
val AmberLightSecondaryContainer = Color(0xFFF9DFBB)
val AmberLightOnSecondaryContainer = Color(0xFF261904)
val AmberLightTertiary = Color(0xFF506441)
val AmberLightOnTertiary = Color(0xFFFFFFFF)
val AmberLightTertiaryContainer = Color(0xFFD2EABE)
val AmberLightOnTertiaryContainer = Color(0xFF0E2005)
val AmberLightOnBackground = Color(0xFF201B14)
val AmberLightSurface = Color(0xFFFFF8F4)
val AmberLightOnSurface = Color(0xFF201B14)
val AmberLightSurfaceVariant = Color(0xFFEFE0CF)
val AmberLightOnSurfaceVariant = Color(0xFF4E4539)
val AmberLightOutline = Color(0xFF817567)
val AmberLightOutlineVariant = Color(0xFFD2C4B4)
val AmberLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val AmberLightSurfaceContainerLow = Color(0xFFFAF2E8)
val AmberLightSurfaceContainerHigh = Color(0xFFF0E6DC)
val AmberLightSurfaceContainerHighest = Color(0xFFEAE0D6)

// Amber Dark
val AmberDarkOnPrimary = Color(0xFF462A00)
val AmberDarkOnPrimaryContainer = Color(0xFFFFDDB6)
val AmberDarkSecondary = Color(0xFFDCC3A1)
val AmberDarkOnSecondary = Color(0xFF3C2D16)
val AmberDarkSecondaryContainer = Color(0xFF54432A)
val AmberDarkOnSecondaryContainer = Color(0xFFF9DFBB)
val AmberDarkTertiary = Color(0xFFB7CEA3)
val AmberDarkOnTertiary = Color(0xFF233517)
val AmberDarkTertiaryContainer = Color(0xFF394C2B)
val AmberDarkOnTertiaryContainer = Color(0xFFD2EABE)
val AmberDarkOnBackground = Color(0xFFECE0D5)
val AmberDarkSurface = Color(0xFF18120B)
val AmberDarkOnSurface = Color(0xFFECE0D5)
val AmberDarkSurfaceVariant = Color(0xFF4E4539)
val AmberDarkOnSurfaceVariant = Color(0xFFD2C4B4)
val AmberDarkOutline = Color(0xFF9B8F80)
val AmberDarkOutlineVariant = Color(0xFF4E4539)
val AmberDarkSurfaceContainerLowest = Color(0xFF120D07)
val AmberDarkSurfaceContainerLow = Color(0xFF211A11)
val AmberDarkSurfaceContainerHigh = Color(0xFF322A1F)
val AmberDarkSurfaceContainerHighest = Color(0xFF3D3529)

// Rose Light
val RoseLightOnPrimary = Color(0xFFFFFFFF)
val RoseLightOnPrimaryContainer = Color(0xFF3B071B)
val RoseLightSecondary = Color(0xFF75565D)
val RoseLightOnSecondary = Color(0xFFFFFFFF)
val RoseLightSecondaryContainer = Color(0xFFFFD9E1)
val RoseLightOnSecondaryContainer = Color(0xFF2B151B)
val RoseLightTertiary = Color(0xFF7C5635)
val RoseLightOnTertiary = Color(0xFFFFFFFF)
val RoseLightTertiaryContainer = Color(0xFFFFDCC2)
val RoseLightOnTertiaryContainer = Color(0xFF2E1500)
val RoseLightOnBackground = Color(0xFF211A1B)
val RoseLightSurface = Color(0xFFFFF8F8)
val RoseLightOnSurface = Color(0xFF211A1B)
val RoseLightSurfaceVariant = Color(0xFFF3DDE1)
val RoseLightOnSurfaceVariant = Color(0xFF514346)
val RoseLightOutline = Color(0xFF847376)
val RoseLightOutlineVariant = Color(0xFFD6C2C5)
val RoseLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val RoseLightSurfaceContainerLow = Color(0xFFFAF0F1)
val RoseLightSurfaceContainerHigh = Color(0xFFF0E2E4)
val RoseLightSurfaceContainerHighest = Color(0xFFEADCDD)

// Rose Dark
val RoseDarkOnPrimary = Color(0xFF561D30)
val RoseDarkOnPrimaryContainer = Color(0xFFFFD9E1)
val RoseDarkSecondary = Color(0xFFE4BDC5)
val RoseDarkOnSecondary = Color(0xFF432930)
val RoseDarkSecondaryContainer = Color(0xFF5B3F46)
val RoseDarkOnSecondaryContainer = Color(0xFFFFD9E1)
val RoseDarkTertiary = Color(0xFFEFBD94)
val RoseDarkOnTertiary = Color(0xFF47280C)
val RoseDarkTertiaryContainer = Color(0xFF613E1F)
val RoseDarkOnTertiaryContainer = Color(0xFFFFDCC2)
val RoseDarkOnBackground = Color(0xFFEDDFE0)
val RoseDarkSurface = Color(0xFF191013)
val RoseDarkOnSurface = Color(0xFFEDDFE0)
val RoseDarkSurfaceVariant = Color(0xFF514346)
val RoseDarkOnSurfaceVariant = Color(0xFFD6C2C5)
val RoseDarkOutline = Color(0xFF9F8C90)
val RoseDarkOutlineVariant = Color(0xFF514346)
val RoseDarkSurfaceContainerLowest = Color(0xFF130B0E)
val RoseDarkSurfaceContainerLow = Color(0xFF23181C)
val RoseDarkSurfaceContainerHigh = Color(0xFF34272B)
val RoseDarkSurfaceContainerHighest = Color(0xFF403236)

fun getExpressiveColorScheme(themeColor: String, isDark: Boolean): ColorScheme {
    return if (isDark) {
        when (themeColor) {
            "ocean" -> darkColorScheme(
                primary = OceanDarkPrimary,
                onPrimary = OceanDarkOnPrimary,
                primaryContainer = OceanDarkPrimaryContainer,
                onPrimaryContainer = OceanDarkOnPrimaryContainer,
                secondary = OceanDarkSecondary,
                onSecondary = OceanDarkOnSecondary,
                secondaryContainer = OceanDarkSecondaryContainer,
                onSecondaryContainer = OceanDarkOnSecondaryContainer,
                tertiary = OceanDarkTertiary,
                onTertiary = OceanDarkOnTertiary,
                tertiaryContainer = OceanDarkTertiaryContainer,
                onTertiaryContainer = OceanDarkOnTertiaryContainer,
                background = OceanDarkBackground,
                onBackground = OceanDarkOnBackground,
                surface = OceanDarkSurface,
                onSurface = OceanDarkOnSurface,
                surfaceVariant = OceanDarkSurfaceVariant,
                onSurfaceVariant = OceanDarkOnSurfaceVariant,
                surfaceContainerLowest = OceanDarkSurfaceContainerLowest,
                surfaceContainerLow = OceanDarkSurfaceContainerLow,
                surfaceContainer = OceanDarkSurfaceContainer,
                surfaceContainerHigh = OceanDarkSurfaceContainerHigh,
                surfaceContainerHighest = OceanDarkSurfaceContainerHighest,
                outline = OceanDarkOutline,
                outlineVariant = OceanDarkOutlineVariant,
                error = DarkError,
                onError = DarkOnError,
                errorContainer = DarkErrorContainer,
                onErrorContainer = DarkOnErrorContainer
            )
            "violet" -> darkColorScheme(
                primary = VioletDarkPrimary,
                onPrimary = VioletDarkOnPrimary,
                primaryContainer = VioletDarkPrimaryContainer,
                onPrimaryContainer = VioletDarkOnPrimaryContainer,
                secondary = VioletDarkSecondary,
                onSecondary = VioletDarkOnSecondary,
                secondaryContainer = VioletDarkSecondaryContainer,
                onSecondaryContainer = VioletDarkOnSecondaryContainer,
                tertiary = VioletDarkTertiary,
                onTertiary = VioletDarkOnTertiary,
                tertiaryContainer = VioletDarkTertiaryContainer,
                onTertiaryContainer = VioletDarkOnTertiaryContainer,
                background = VioletDarkBackground,
                onBackground = VioletDarkOnBackground,
                surface = VioletDarkSurface,
                onSurface = VioletDarkOnSurface,
                surfaceVariant = VioletDarkSurfaceVariant,
                onSurfaceVariant = VioletDarkOnSurfaceVariant,
                surfaceContainerLowest = VioletDarkSurfaceContainerLowest,
                surfaceContainerLow = VioletDarkSurfaceContainerLow,
                surfaceContainer = VioletDarkSurfaceContainer,
                surfaceContainerHigh = VioletDarkSurfaceContainerHigh,
                surfaceContainerHighest = VioletDarkSurfaceContainerHighest,
                outline = VioletDarkOutline,
                outlineVariant = VioletDarkOutlineVariant,
                error = DarkError,
                onError = DarkOnError,
                errorContainer = DarkErrorContainer,
                onErrorContainer = DarkOnErrorContainer
            )
            "amber" -> darkColorScheme(
                primary = AmberDarkPrimary,
                onPrimary = AmberDarkOnPrimary,
                primaryContainer = AmberDarkPrimaryContainer,
                onPrimaryContainer = AmberDarkOnPrimaryContainer,
                secondary = AmberDarkSecondary,
                onSecondary = AmberDarkOnSecondary,
                secondaryContainer = AmberDarkSecondaryContainer,
                onSecondaryContainer = AmberDarkOnSecondaryContainer,
                tertiary = AmberDarkTertiary,
                onTertiary = AmberDarkOnTertiary,
                tertiaryContainer = AmberDarkTertiaryContainer,
                onTertiaryContainer = AmberDarkOnTertiaryContainer,
                background = AmberDarkBackground,
                onBackground = AmberDarkOnBackground,
                surface = AmberDarkSurface,
                onSurface = AmberDarkOnSurface,
                surfaceVariant = AmberDarkSurfaceVariant,
                onSurfaceVariant = AmberDarkOnSurfaceVariant,
                surfaceContainerLowest = AmberDarkSurfaceContainerLowest,
                surfaceContainerLow = AmberDarkSurfaceContainerLow,
                surfaceContainer = AmberDarkSurfaceContainer,
                surfaceContainerHigh = AmberDarkSurfaceContainerHigh,
                surfaceContainerHighest = AmberDarkSurfaceContainerHighest,
                outline = AmberDarkOutline,
                outlineVariant = AmberDarkOutlineVariant,
                error = DarkError,
                onError = DarkOnError,
                errorContainer = DarkErrorContainer,
                onErrorContainer = DarkOnErrorContainer
            )
            "rose" -> darkColorScheme(
                primary = RoseDarkPrimary,
                onPrimary = RoseDarkOnPrimary,
                primaryContainer = RoseDarkPrimaryContainer,
                onPrimaryContainer = RoseDarkOnPrimaryContainer,
                secondary = RoseDarkSecondary,
                onSecondary = RoseDarkOnSecondary,
                secondaryContainer = RoseDarkSecondaryContainer,
                onSecondaryContainer = RoseDarkOnSecondaryContainer,
                tertiary = RoseDarkTertiary,
                onTertiary = RoseDarkOnTertiary,
                tertiaryContainer = RoseDarkTertiaryContainer,
                onTertiaryContainer = RoseDarkOnTertiaryContainer,
                background = RoseDarkBackground,
                onBackground = RoseDarkOnBackground,
                surface = RoseDarkSurface,
                onSurface = RoseDarkOnSurface,
                surfaceVariant = RoseDarkSurfaceVariant,
                onSurfaceVariant = RoseDarkOnSurfaceVariant,
                surfaceContainerLowest = RoseDarkSurfaceContainerLowest,
                surfaceContainerLow = RoseDarkSurfaceContainerLow,
                surfaceContainer = RoseDarkSurfaceContainer,
                surfaceContainerHigh = RoseDarkSurfaceContainerHigh,
                surfaceContainerHighest = RoseDarkSurfaceContainerHighest,
                outline = RoseDarkOutline,
                outlineVariant = RoseDarkOutlineVariant,
                error = DarkError,
                onError = DarkOnError,
                errorContainer = DarkErrorContainer,
                onErrorContainer = DarkOnErrorContainer
            )
            else -> darkColorScheme(
                primary = PistachioDarkPrimary,
                onPrimary = PistachioDarkOnPrimary,
                primaryContainer = PistachioDarkPrimaryContainer,
                onPrimaryContainer = PistachioDarkOnPrimaryContainer,
                secondary = PistachioDarkSecondary,
                onSecondary = PistachioDarkOnSecondary,
                secondaryContainer = PistachioDarkSecondaryContainer,
                onSecondaryContainer = PistachioDarkOnSecondaryContainer,
                tertiary = PistachioDarkTertiary,
                onTertiary = PistachioDarkOnTertiary,
                tertiaryContainer = PistachioDarkTertiaryContainer,
                onTertiaryContainer = PistachioDarkOnTertiaryContainer,
                background = PistachioDarkBackground,
                onBackground = PistachioDarkOnBackground,
                surface = PistachioDarkSurface,
                onSurface = PistachioDarkOnSurface,
                surfaceVariant = PistachioDarkSurfaceVariant,
                onSurfaceVariant = PistachioDarkOnSurfaceVariant,
                surfaceContainerLowest = PistachioDarkSurfaceContainerLowest,
                surfaceContainerLow = PistachioDarkSurfaceContainerLow,
                surfaceContainer = PistachioDarkSurfaceContainer,
                surfaceContainerHigh = PistachioDarkSurfaceContainerHigh,
                surfaceContainerHighest = PistachioDarkSurfaceContainerHighest,
                outline = PistachioDarkOutline,
                outlineVariant = PistachioDarkOutlineVariant,
                error = DarkError,
                onError = DarkOnError,
                errorContainer = DarkErrorContainer,
                onErrorContainer = DarkOnErrorContainer
            )
        }
    } else {
        when (themeColor) {
            "ocean" -> lightColorScheme(
                primary = OceanLightPrimary,
                onPrimary = OceanLightOnPrimary,
                primaryContainer = OceanLightPrimaryContainer,
                onPrimaryContainer = OceanLightOnPrimaryContainer,
                secondary = OceanLightSecondary,
                onSecondary = OceanLightOnSecondary,
                secondaryContainer = OceanLightSecondaryContainer,
                onSecondaryContainer = OceanLightOnSecondaryContainer,
                tertiary = OceanLightTertiary,
                onTertiary = OceanLightOnTertiary,
                tertiaryContainer = OceanLightTertiaryContainer,
                onTertiaryContainer = OceanLightOnTertiaryContainer,
                background = OceanLightBackground,
                onBackground = OceanLightOnBackground,
                surface = OceanLightSurface,
                onSurface = OceanLightOnSurface,
                surfaceVariant = OceanLightSurfaceVariant,
                onSurfaceVariant = OceanLightOnSurfaceVariant,
                surfaceContainerLowest = OceanLightSurfaceContainerLowest,
                surfaceContainerLow = OceanLightSurfaceContainerLow,
                surfaceContainer = OceanLightSurfaceContainer,
                surfaceContainerHigh = OceanLightSurfaceContainerHigh,
                surfaceContainerHighest = OceanLightSurfaceContainerHighest,
                outline = OceanLightOutline,
                outlineVariant = OceanLightOutlineVariant,
                error = Error,
                onError = OnError,
                errorContainer = ErrorContainer,
                onErrorContainer = OnErrorContainer
            )
            "violet" -> lightColorScheme(
                primary = VioletLightPrimary,
                onPrimary = VioletLightOnPrimary,
                primaryContainer = VioletLightPrimaryContainer,
                onPrimaryContainer = VioletLightOnPrimaryContainer,
                secondary = VioletLightSecondary,
                onSecondary = VioletLightOnSecondary,
                secondaryContainer = VioletLightSecondaryContainer,
                onSecondaryContainer = VioletLightOnSecondaryContainer,
                tertiary = VioletLightTertiary,
                onTertiary = VioletLightOnTertiary,
                tertiaryContainer = VioletLightTertiaryContainer,
                onTertiaryContainer = VioletLightOnTertiaryContainer,
                background = VioletLightBackground,
                onBackground = VioletLightOnBackground,
                surface = VioletLightSurface,
                onSurface = VioletLightOnSurface,
                surfaceVariant = VioletLightSurfaceVariant,
                onSurfaceVariant = VioletLightOnSurfaceVariant,
                surfaceContainerLowest = VioletLightSurfaceContainerLowest,
                surfaceContainerLow = VioletLightSurfaceContainerLow,
                surfaceContainer = VioletLightSurfaceContainer,
                surfaceContainerHigh = VioletLightSurfaceContainerHigh,
                surfaceContainerHighest = VioletLightSurfaceContainerHighest,
                outline = VioletLightOutline,
                outlineVariant = VioletLightOutlineVariant,
                error = Error,
                onError = OnError,
                errorContainer = ErrorContainer,
                onErrorContainer = OnErrorContainer
            )
            "amber" -> lightColorScheme(
                primary = AmberLightPrimary,
                onPrimary = AmberLightOnPrimary,
                primaryContainer = AmberLightPrimaryContainer,
                onPrimaryContainer = AmberLightOnPrimaryContainer,
                secondary = AmberLightSecondary,
                onSecondary = AmberLightOnSecondary,
                secondaryContainer = AmberLightSecondaryContainer,
                onSecondaryContainer = AmberLightOnSecondaryContainer,
                tertiary = AmberLightTertiary,
                onTertiary = AmberLightOnTertiary,
                tertiaryContainer = AmberLightTertiaryContainer,
                onTertiaryContainer = AmberLightOnTertiaryContainer,
                background = AmberLightBackground,
                onBackground = AmberLightOnBackground,
                surface = AmberLightSurface,
                onSurface = AmberLightOnSurface,
                surfaceVariant = AmberLightSurfaceVariant,
                onSurfaceVariant = AmberLightOnSurfaceVariant,
                surfaceContainerLowest = AmberLightSurfaceContainerLowest,
                surfaceContainerLow = AmberLightSurfaceContainerLow,
                surfaceContainer = AmberLightSurfaceContainer,
                surfaceContainerHigh = AmberLightSurfaceContainerHigh,
                surfaceContainerHighest = AmberLightSurfaceContainerHighest,
                outline = AmberLightOutline,
                outlineVariant = AmberLightOutlineVariant,
                error = Error,
                onError = OnError,
                errorContainer = ErrorContainer,
                onErrorContainer = OnErrorContainer
            )
            "rose" -> lightColorScheme(
                primary = RoseLightPrimary,
                onPrimary = RoseLightOnPrimary,
                primaryContainer = RoseLightPrimaryContainer,
                onPrimaryContainer = RoseLightOnPrimaryContainer,
                secondary = RoseLightSecondary,
                onSecondary = RoseLightOnSecondary,
                secondaryContainer = RoseLightSecondaryContainer,
                onSecondaryContainer = RoseLightOnSecondaryContainer,
                tertiary = RoseLightTertiary,
                onTertiary = RoseLightOnTertiary,
                tertiaryContainer = RoseLightTertiaryContainer,
                onTertiaryContainer = RoseLightOnTertiaryContainer,
                background = RoseLightBackground,
                onBackground = RoseLightOnBackground,
                surface = RoseLightSurface,
                onSurface = RoseLightOnSurface,
                surfaceVariant = RoseLightSurfaceVariant,
                onSurfaceVariant = RoseLightOnSurfaceVariant,
                surfaceContainerLowest = RoseLightSurfaceContainerLowest,
                surfaceContainerLow = RoseLightSurfaceContainerLow,
                surfaceContainer = RoseLightSurfaceContainer,
                surfaceContainerHigh = RoseLightSurfaceContainerHigh,
                surfaceContainerHighest = RoseLightSurfaceContainerHighest,
                outline = RoseLightOutline,
                outlineVariant = RoseLightOutlineVariant,
                error = Error,
                onError = OnError,
                errorContainer = ErrorContainer,
                onErrorContainer = OnErrorContainer
            )
            else -> lightColorScheme(
                primary = PistachioLightPrimary,
                onPrimary = PistachioLightOnPrimary,
                primaryContainer = PistachioLightPrimaryContainer,
                onPrimaryContainer = PistachioLightOnPrimaryContainer,
                secondary = PistachioLightSecondary,
                onSecondary = PistachioLightOnSecondary,
                secondaryContainer = PistachioLightSecondaryContainer,
                onSecondaryContainer = PistachioLightOnSecondaryContainer,
                tertiary = PistachioLightTertiary,
                onTertiary = PistachioLightOnTertiary,
                tertiaryContainer = PistachioLightTertiaryContainer,
                onTertiaryContainer = PistachioLightOnTertiaryContainer,
                background = PistachioLightBackground,
                onBackground = PistachioLightOnBackground,
                surface = PistachioLightSurface,
                onSurface = PistachioLightOnSurface,
                surfaceVariant = PistachioLightSurfaceVariant,
                onSurfaceVariant = PistachioLightOnSurfaceVariant,
                surfaceContainerLowest = PistachioLightSurfaceContainerLowest,
                surfaceContainerLow = PistachioLightSurfaceContainerLow,
                surfaceContainer = PistachioLightSurfaceContainer,
                surfaceContainerHigh = PistachioLightSurfaceContainerHigh,
                surfaceContainerHighest = PistachioLightSurfaceContainerHighest,
                outline = PistachioLightOutline,
                outlineVariant = PistachioLightOutlineVariant,
                error = Error,
                onError = OnError,
                errorContainer = ErrorContainer,
                onErrorContainer = OnErrorContainer
            )
        }
    }
}
