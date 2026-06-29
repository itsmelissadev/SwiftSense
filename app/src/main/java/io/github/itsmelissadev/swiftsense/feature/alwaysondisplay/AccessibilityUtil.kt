package io.github.itsmelissadev.swiftsense.feature.alwaysondisplay

import android.content.Context
import android.provider.Settings
import android.text.TextUtils

object AccessibilityUtil {
    fun isAccessibilityServiceEnabled(context: Context, accessibilityService: Class<*>): Boolean {
        var accessibilityEnabled = 0
        val service = context.packageName + "/" + accessibilityService.canonicalName
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {}
        val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityServiceStr = mStringColonSplitter.next()
                    if (accessibilityServiceStr.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
