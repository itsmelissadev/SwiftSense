package io.github.itsmelissadev.swiftsense.feature.alwaysondisplay

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.TextUtils
import io.github.itsmelissadev.swiftsense.service.shell.ShellBridge

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

    fun isShizukuAvailable(): Boolean {
        return ShellBridge.ready.value
    }

    fun toggleAccessibilityServiceWithShizuku(context: Context, accessibilityService: Class<*>, enable: Boolean): Boolean {
        if (!isShizukuAvailable()) return false
        val serviceComponent = context.packageName + "/" + accessibilityService.canonicalName

        val getCmd = "settings get secure enabled_accessibility_services"
        val result = ShellBridge.run(getCmd)
        val currentServices = if (result.isSuccess) {
            result.getOrNull()?.trim()?.takeIf { it != "null" } ?: ""
        } else ""

        val serviceList = currentServices.split(":").filter { it.isNotBlank() }.toMutableSet()
        if (enable) {
            serviceList.add(serviceComponent)
        } else {
            serviceList.remove(serviceComponent)
        }

        val newServicesString = serviceList.joinToString(":")
        val putCmd = if (newServicesString.isEmpty()) {
            "settings put secure enabled_accessibility_services '\"\"' && settings put secure accessibility_enabled 0"
        } else {
            "settings put secure enabled_accessibility_services \"$newServicesString\" && settings put secure accessibility_enabled 1"
        }

        val putResult = ShellBridge.run(putCmd)
        return putResult.isSuccess
    }
}
