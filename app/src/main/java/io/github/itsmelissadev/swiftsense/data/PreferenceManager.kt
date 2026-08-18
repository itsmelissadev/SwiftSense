package io.github.itsmelissadev.swiftsense.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {
    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LANGUAGE = stringPreferencesKey("language")
        private val SELECTED_MODE = stringPreferencesKey("selected_mode")
        val IS_SERVICE_RUNNING = booleanPreferencesKey("is_service_running")
        val SHOW_LIVE_HZ = booleanPreferencesKey("show_live_hz")
        val TOUCH_BOOST_ENABLED = booleanPreferencesKey("touch_boost_enabled")
        val SHIZUKU_SENSOR_BOOST = booleanPreferencesKey("shizuku_sensor_boost")
        const val SENSOR_STATES_PREFIX = "sensor_state_"
        private val DISABLED_APPS = stringSetPreferencesKey("disabled_apps")
        val APP_MANAGER_WARNING_DISMISSED = booleanPreferencesKey("app_manager_warning_dismissed")
        val APP_MANAGER_CUSTOM_LISTS = stringSetPreferencesKey("app_manager_custom_lists")
        val APP_MANAGER_ACTIVE_LIST_ID = stringPreferencesKey("app_manager_active_list_id")
        private val STOPPER_APPS = stringSetPreferencesKey("stopper_apps")
        val IS_STOPPER_SERVICE_RUNNING = booleanPreferencesKey("is_stopper_service_running")
        val STOPPER_INTERVAL = intPreferencesKey("stopper_interval")
        val STOPPER_MODE = stringPreferencesKey("stopper_mode")
        private val RESOLUTION_PLANS = stringSetPreferencesKey("resolution_plans")
        private val SYSTEM_MACROS = stringSetPreferencesKey("system_macros")
        val AMOLED_INTENSITY = floatPreferencesKey("amoled_intensity")
        val AMOLED_FILTER_TYPE = stringPreferencesKey("amoled_filter_type")
        val AMOLED_SHIFT_SPEED = intPreferencesKey("amoled_shift_speed")
        val AMOLED_OPACITY = floatPreferencesKey("amoled_opacity")
        val AMOLED_REFRESH_MODE = stringPreferencesKey("amoled_refresh_mode")
        val AMOLED_WARNING_DISMISSED = booleanPreferencesKey("amoled_warning_dismissed")
        val AMOLED_REGIONS = stringSetPreferencesKey("amoled_regions")
        val AMOLED_CUSTOM_GAP_HEIGHT = floatPreferencesKey("amoled_custom_gap_height")
        val AMOLED_CUSTOM_POSITION = floatPreferencesKey("amoled_custom_position")
        val AMOLED_TINT_ENABLED = booleanPreferencesKey("amoled_tint_enabled")
        val AMOLED_TINT_COLOR = stringPreferencesKey("amoled_tint_color")
        val AMOLED_TINT_CUSTOM_HEX = stringPreferencesKey("amoled_tint_custom_hex")
        val AMOLED_TINT_INTENSITY = floatPreferencesKey("amoled_tint_intensity")
        val GYRO_SIM_ENABLED = booleanPreferencesKey("gyro_sim_enabled")
        val GYRO_SIM_SENSITIVITY = floatPreferencesKey("gyro_sim_sensitivity")
        val GYRO_SIM_INVERT_X = booleanPreferencesKey("gyro_sim_invert_x")
        val GYRO_SIM_INVERT_Y = booleanPreferencesKey("gyro_sim_invert_y")
        val GYRO_SIM_CONTROL_X = intPreferencesKey("gyro_sim_control_x")
        val GYRO_SIM_CONTROL_Y = intPreferencesKey("gyro_sim_control_y")
        val GYRO_SIM_AREA_X = intPreferencesKey("gyro_sim_area_x")
        val GYRO_SIM_AREA_Y = intPreferencesKey("gyro_sim_area_y")
        val GYRO_SIM_AREA_WIDTH = intPreferencesKey("gyro_sim_area_width")
        val GYRO_SIM_AREA_HEIGHT = intPreferencesKey("gyro_sim_area_height")
        val GYRO_SIM_LOCKED = booleanPreferencesKey("gyro_sim_locked")
        val SENSOR_SPEED = stringPreferencesKey("sensor_speed")
        val AUTO_APPLY_MACRO_JSON = stringPreferencesKey("auto_apply_macro_json")
        val AUTO_APPLY_MACRO_INTERVAL = intPreferencesKey("auto_apply_macro_interval")
        val IS_MACRO_SERVICE_RUNNING = booleanPreferencesKey("is_macro_service_running")
        
        val AOD_ENABLED = booleanPreferencesKey("aod_enabled")
        val AOD_SHOW_CLOCK = booleanPreferencesKey("aod_show_clock")
        val AOD_SHOW_DATE = booleanPreferencesKey("aod_show_date")
        val AOD_SHOW_BATTERY = booleanPreferencesKey("aod_show_battery")
        val AOD_SHOW_NOTIFICATIONS = booleanPreferencesKey("aod_show_notifications")
        val AOD_CLOCK_STYLE = stringPreferencesKey("aod_clock_style")
        val AOD_BATTERY_STYLE = stringPreferencesKey("aod_battery_style")
        val AOD_BRIGHTNESS = floatPreferencesKey("aod_brightness")
        val AOD_ACTIVATE_ON_LOCK = booleanPreferencesKey("aod_activate_on_lock")
        val AOD_TIMEOUT_SECONDS = intPreferencesKey("aod_timeout_seconds")
        val AOD_WARNING_DISMISSED = booleanPreferencesKey("aod_warning_dismissed")
        
        val AOD_CLOCK_COLOR = intPreferencesKey("aod_clock_color")
        val AOD_BATTERY_COLOR = intPreferencesKey("aod_battery_color")
        val AOD_FONT_FAMILY = stringPreferencesKey("aod_font_family")
        val AOD_BURN_IN_PROTECTION = booleanPreferencesKey("aod_burn_in_protection")
        val AOD_BURN_IN_INTERVAL = intPreferencesKey("aod_burn_in_interval")
        val AOD_BURN_IN_MODE = stringPreferencesKey("aod_burn_in_mode")
        val AOD_BURN_IN_RGB_SHIFT = booleanPreferencesKey("aod_burn_in_rgb_shift")
        
        val AOD_DISMISS_POWER_BUTTON = booleanPreferencesKey("aod_dismiss_power_button")
        val AOD_DISMISS_DOUBLE_TAP = booleanPreferencesKey("aod_dismiss_double_tap")
        val AOD_SHOW_WATTAGE = booleanPreferencesKey("aod_show_wattage")
        val AOD_AS_FOREGROUND_SERVICE = booleanPreferencesKey("aod_as_foreground_service")
    }

    val preferences: Flow<Preferences> = context.dataStore.data

    val isOnboardingCompleted: Flow<Boolean> = preferences.map { it[ONBOARDING_COMPLETED] ?: false }
    suspend fun setOnboardingCompleted(completed: Boolean) { context.dataStore.edit { it[ONBOARDING_COMPLETED] = completed } }

    val themeMode: Flow<String> = preferences.map { it[THEME_MODE] ?: "system" }
    suspend fun setThemeMode(mode: String) { context.dataStore.edit { it[THEME_MODE] = mode } }

    val language: Flow<String> = preferences.map { it[LANGUAGE] ?: "system" }
    suspend fun setLanguage(lang: String) { context.dataStore.edit { it[LANGUAGE] = lang } }

    val selectedMode: Flow<String?> = preferences.map { it[SELECTED_MODE] }
    suspend fun setSelectedMode(mode: String) { context.dataStore.edit { it[SELECTED_MODE] = mode } }

    val isServiceRunning: Flow<Boolean> = preferences.map { it[IS_SERVICE_RUNNING] ?: false }
    suspend fun setServiceRunning(running: Boolean) { context.dataStore.edit { it[IS_SERVICE_RUNNING] = running } }

    val showLiveHz: Flow<Boolean> = preferences.map { it[SHOW_LIVE_HZ] ?: false }
    suspend fun setShowLiveHz(show: Boolean) { context.dataStore.edit { it[SHOW_LIVE_HZ] = show } }

    val touchBoostEnabled: Flow<Boolean> = preferences.map { it[TOUCH_BOOST_ENABLED] ?: false }
    suspend fun setTouchBoostEnabled(enabled: Boolean) { context.dataStore.edit { it[TOUCH_BOOST_ENABLED] = enabled } }

    val shizukuSensorBoost: Flow<Boolean> = preferences.map { it[SHIZUKU_SENSOR_BOOST] ?: false }
    suspend fun setShizukuSensorBoost(enabled: Boolean) { context.dataStore.edit { it[SHIZUKU_SENSOR_BOOST] = enabled } }

    val disabledApps: Flow<Set<String>> = preferences.map { it[DISABLED_APPS] ?: emptySet() }

    suspend fun setDisabledApps(apps: Set<String>) {
        context.dataStore.edit { it[DISABLED_APPS] = apps }
    }

    suspend fun toggleAppInList(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[DISABLED_APPS] ?: emptySet()
            if (current.contains(packageName)) {
                prefs[DISABLED_APPS] = current - packageName
            } else {
                prefs[DISABLED_APPS] = current + packageName
            }
        }
    }

    val appManagerWarningDismissed: Flow<Boolean> =
        preferences.map { it[APP_MANAGER_WARNING_DISMISSED] ?: false }

    suspend fun setAppManagerWarningDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[APP_MANAGER_WARNING_DISMISSED] = dismissed }
    }

    val appManagerCustomListsJson: Flow<Set<String>> =
        preferences.map { it[APP_MANAGER_CUSTOM_LISTS] ?: emptySet() }

    val appManagerActiveListId: Flow<String?> =
        preferences.map { it[APP_MANAGER_ACTIVE_LIST_ID] }

    suspend fun saveAppManagerCustomLists(lists: Set<String>, activeId: String?) {
        context.dataStore.edit { prefs ->
            prefs[APP_MANAGER_CUSTOM_LISTS] = lists
            if (activeId != null) {
                prefs[APP_MANAGER_ACTIVE_LIST_ID] = activeId
            }
        }
    }

    suspend fun setAppManagerActiveListId(id: String) {
        context.dataStore.edit { it[APP_MANAGER_ACTIVE_LIST_ID] = id }
    }

    val stopperApps: Flow<Set<String>> = preferences.map { it[STOPPER_APPS] ?: emptySet() }

    suspend fun setStopperApps(apps: Set<String>) {
        context.dataStore.edit { it[STOPPER_APPS] = apps }
    }

    suspend fun toggleStopperApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[STOPPER_APPS] ?: emptySet()
            if (current.contains(packageName)) {
                prefs[STOPPER_APPS] = current - packageName
            } else {
                prefs[STOPPER_APPS] = current + packageName
            }
        }
    }

    val isStopperServiceRunning: Flow<Boolean> =
        preferences.map { it[IS_STOPPER_SERVICE_RUNNING] ?: false }

    suspend fun setStopperServiceRunning(running: Boolean) {
        context.dataStore.edit { it[IS_STOPPER_SERVICE_RUNNING] = running }
    }

    val stopperInterval: Flow<Int> = preferences.map { it[STOPPER_INTERVAL] ?: 10 }

    suspend fun setStopperInterval(interval: Int) {
        context.dataStore.edit { it[STOPPER_INTERVAL] = interval }
    }

    val stopperMode: Flow<String> = preferences.map { it[STOPPER_MODE] ?: "FORCE" }

    suspend fun setStopperMode(mode: String) {
        context.dataStore.edit { it[STOPPER_MODE] = mode }
    }

    val systemMacros: Flow<Set<String>> = preferences.map { it[SYSTEM_MACROS] ?: emptySet() }

    suspend fun addSystemMacro(macroJson: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SYSTEM_MACROS] ?: emptySet()
            prefs[SYSTEM_MACROS] = current + macroJson
        }
    }

    suspend fun removeSystemMacro(macroJson: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SYSTEM_MACROS] ?: emptySet()
            prefs[SYSTEM_MACROS] = current - macroJson
        }
    }

    suspend fun updateSystemMacro(oldMacroJson: String, newMacroJson: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SYSTEM_MACROS] ?: emptySet()
            prefs[SYSTEM_MACROS] = current - oldMacroJson + newMacroJson
        }
    }

    val resolutionPlans: Flow<Set<String>> = preferences.map { it[RESOLUTION_PLANS] ?: emptySet() }

    suspend fun addResolutionPlan(planJson: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[RESOLUTION_PLANS] ?: emptySet()
            prefs[RESOLUTION_PLANS] = current + planJson
        }
    }

    suspend fun deleteResolutionPlan(planJson: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[RESOLUTION_PLANS] ?: emptySet()
            prefs[RESOLUTION_PLANS] = current - planJson
        }
    }

    fun getSensorState(sensorType: Int): Flow<Boolean> = preferences.map {
        it[booleanPreferencesKey(SENSOR_STATES_PREFIX + sensorType)] ?: false
    }
    suspend fun setSensorState(sensorType: Int, enabled: Boolean) {
        context.dataStore.edit { it[booleanPreferencesKey(SENSOR_STATES_PREFIX + sensorType)] = enabled }
    }

    val amoledIntensity: Flow<Float> = preferences.map { it[AMOLED_INTENSITY] ?: 1.0f }
    suspend fun setAmoledIntensity(intensity: Float) {
        context.dataStore.edit { it[AMOLED_INTENSITY] = intensity }
    }

    val amoledFilterType: Flow<String> =
        preferences.map { it[AMOLED_FILTER_TYPE] ?: "checker_grid" }

    suspend fun setAmoledFilterType(type: String) {
        context.dataStore.edit { it[AMOLED_FILTER_TYPE] = type }
    }

    val amoledShiftSpeed: Flow<Int> = preferences.map { it[AMOLED_SHIFT_SPEED] ?: 30 }
    suspend fun setAmoledShiftSpeed(speed: Int) {
        context.dataStore.edit { it[AMOLED_SHIFT_SPEED] = speed }
    }

    val amoledOpacity: Flow<Float> = preferences.map { it[AMOLED_OPACITY] ?: 1.0f }
    suspend fun setAmoledOpacity(opacity: Float) {
        context.dataStore.edit { it[AMOLED_OPACITY] = opacity }
    }

    val amoledRefreshMode: Flow<String> = preferences.map { it[AMOLED_REFRESH_MODE] ?: "jump" }
    suspend fun setAmoledRefreshMode(mode: String) {
        context.dataStore.edit { it[AMOLED_REFRESH_MODE] = mode }
    }

    val amoledWarningDismissed: Flow<Boolean> =
        preferences.map { it[AMOLED_WARNING_DISMISSED] ?: false }

    suspend fun setAmoledWarningDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[AMOLED_WARNING_DISMISSED] = dismissed }
    }

    val amoledRegions: Flow<Set<String>> =
        preferences.map { it[AMOLED_REGIONS] ?: setOf("full_screen") }

    suspend fun setAmoledRegions(regions: Set<String>) {
        context.dataStore.edit { it[AMOLED_REGIONS] = regions }
    }

    val amoledCustomGapHeight: Flow<Float> =
        preferences.map { it[AMOLED_CUSTOM_GAP_HEIGHT] ?: 0.40f }

    suspend fun setAmoledCustomGapHeight(height: Float) {
        context.dataStore.edit { it[AMOLED_CUSTOM_GAP_HEIGHT] = height }
    }

    val amoledCustomPosition: Flow<Float> =
        preferences.map { it[AMOLED_CUSTOM_POSITION] ?: 0.50f }

    suspend fun setAmoledCustomPosition(position: Float) {
        context.dataStore.edit { it[AMOLED_CUSTOM_POSITION] = position }
    }

    val amoledTintEnabled: Flow<Boolean> = preferences.map { it[AMOLED_TINT_ENABLED] ?: false }
    suspend fun setAmoledTintEnabled(enabled: Boolean) {
        context.dataStore.edit { it[AMOLED_TINT_ENABLED] = enabled }
    }

    val amoledTintColor: Flow<String> = preferences.map { it[AMOLED_TINT_COLOR] ?: "amber" }
    suspend fun setAmoledTintColor(color: String) {
        context.dataStore.edit { it[AMOLED_TINT_COLOR] = color }
    }

    val amoledTintCustomHex: Flow<String> = preferences.map { it[AMOLED_TINT_CUSTOM_HEX] ?: "#FFA500" }
    suspend fun setAmoledTintCustomHex(hex: String) {
        context.dataStore.edit { it[AMOLED_TINT_CUSTOM_HEX] = hex }
    }

    val amoledTintIntensity: Flow<Float> = preferences.map { it[AMOLED_TINT_INTENSITY] ?: 0.35f }
    suspend fun setAmoledTintIntensity(intensity: Float) {
        context.dataStore.edit { it[AMOLED_TINT_INTENSITY] = intensity }
    }

    val gyroSimEnabled: Flow<Boolean> = preferences.map { it[GYRO_SIM_ENABLED] ?: false }
    suspend fun setGyroSimEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GYRO_SIM_ENABLED] = enabled }
    }

    val gyroSimSensitivity: Flow<Float> = preferences.map { it[GYRO_SIM_SENSITIVITY] ?: 1.0f }
    suspend fun setGyroSimSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[GYRO_SIM_SENSITIVITY] = sensitivity }
    }

    val gyroSimInvertX: Flow<Boolean> = preferences.map { it[GYRO_SIM_INVERT_X] ?: false }
    suspend fun setGyroSimInvertX(invert: Boolean) {
        context.dataStore.edit { it[GYRO_SIM_INVERT_X] = invert }
    }

    val gyroSimInvertY: Flow<Boolean> = preferences.map { it[GYRO_SIM_INVERT_Y] ?: false }
    suspend fun setGyroSimInvertY(invert: Boolean) {
        context.dataStore.edit { it[GYRO_SIM_INVERT_Y] = invert }
    }

    val gyroSimControlX: Flow<Int> = preferences.map { it[GYRO_SIM_CONTROL_X] ?: 100 }
    val gyroSimControlY: Flow<Int> = preferences.map { it[GYRO_SIM_CONTROL_Y] ?: 100 }
    suspend fun setGyroSimControlPos(x: Int, y: Int) {
        context.dataStore.edit {
            it[GYRO_SIM_CONTROL_X] = x
            it[GYRO_SIM_CONTROL_Y] = y
        }
    }

    val gyroSimAreaX: Flow<Int> = preferences.map { it[GYRO_SIM_AREA_X] ?: 0 }
    val gyroSimAreaY: Flow<Int> = preferences.map { it[GYRO_SIM_AREA_Y] ?: 0 }
    suspend fun setGyroSimAreaPos(x: Int, y: Int) {
        context.dataStore.edit {
            it[GYRO_SIM_AREA_X] = x
            it[GYRO_SIM_AREA_Y] = y
        }
    }

    val gyroSimAreaWidth: Flow<Int> = preferences.map { it[GYRO_SIM_AREA_WIDTH] ?: 150 }
    val gyroSimAreaHeight: Flow<Int> = preferences.map { it[GYRO_SIM_AREA_HEIGHT] ?: 150 }
    suspend fun setGyroSimAreaSize(width: Int, height: Int) {
        context.dataStore.edit {
            it[GYRO_SIM_AREA_WIDTH] = width
            it[GYRO_SIM_AREA_HEIGHT] = height
        }
    }

    val gyroSimLocked: Flow<Boolean> = preferences.map { it[GYRO_SIM_LOCKED] ?: false }
    suspend fun setGyroSimLocked(locked: Boolean) {
        context.dataStore.edit { it[GYRO_SIM_LOCKED] = locked }
    }

    val sensorSpeed: Flow<String> = preferences.map { it[SENSOR_SPEED] ?: "max" }
    suspend fun setSensorSpeed(speed: String) {
        context.dataStore.edit { it[SENSOR_SPEED] = speed }
    }

    val autoApplyMacroJson: Flow<String?> = preferences.map { it[AUTO_APPLY_MACRO_JSON] }
    suspend fun setAutoApplyMacroJson(json: String?) {
        context.dataStore.edit { prefs ->
            if (json == null) {
                prefs.remove(AUTO_APPLY_MACRO_JSON)
            } else {
                prefs[AUTO_APPLY_MACRO_JSON] = json
            }
        }
    }

    val autoApplyMacroInterval: Flow<Int> = preferences.map { it[AUTO_APPLY_MACRO_INTERVAL] ?: 30 }
    suspend fun setAutoApplyMacroInterval(seconds: Int) {
        context.dataStore.edit { it[AUTO_APPLY_MACRO_INTERVAL] = seconds }
    }

    val isMacroServiceRunning: Flow<Boolean> =
        preferences.map { it[IS_MACRO_SERVICE_RUNNING] ?: false }

    suspend fun setMacroServiceRunning(running: Boolean) {
        context.dataStore.edit { it[IS_MACRO_SERVICE_RUNNING] = running }
    }

    val aodEnabled: Flow<Boolean> = preferences.map { it[AOD_ENABLED] ?: false }
    suspend fun setAodEnabled(enabled: Boolean) { context.dataStore.edit { it[AOD_ENABLED] = enabled } }

    val aodShowClock: Flow<Boolean> = preferences.map { it[AOD_SHOW_CLOCK] ?: true }
    suspend fun setAodShowClock(show: Boolean) { context.dataStore.edit { it[AOD_SHOW_CLOCK] = show } }

    val aodShowDate: Flow<Boolean> = preferences.map { it[AOD_SHOW_DATE] ?: true }
    suspend fun setAodShowDate(show: Boolean) { context.dataStore.edit { it[AOD_SHOW_DATE] = show } }

    val aodShowBattery: Flow<Boolean> = preferences.map { it[AOD_SHOW_BATTERY] ?: true }
    suspend fun setAodShowBattery(show: Boolean) { context.dataStore.edit { it[AOD_SHOW_BATTERY] = show } }

    val aodShowNotifications: Flow<Boolean> = preferences.map { it[AOD_SHOW_NOTIFICATIONS] ?: true }
    suspend fun setAodShowNotifications(show: Boolean) { context.dataStore.edit { it[AOD_SHOW_NOTIFICATIONS] = show } }

    val aodClockStyle: Flow<String> = preferences.map { it[AOD_CLOCK_STYLE] ?: "digital" }
    suspend fun setAodClockStyle(style: String) { context.dataStore.edit { it[AOD_CLOCK_STYLE] = style } }

    val aodBatteryStyle: Flow<String> = preferences.map { it[AOD_BATTERY_STYLE] ?: "horizontal_classic" }
    suspend fun setAodBatteryStyle(style: String) { context.dataStore.edit { it[AOD_BATTERY_STYLE] = style } }

    val aodBrightness: Flow<Float> = preferences.map { it[AOD_BRIGHTNESS] ?: 0.5f }
    suspend fun setAodBrightness(brightness: Float) { context.dataStore.edit { it[AOD_BRIGHTNESS] = brightness } }

    val aodActivateOnLock: Flow<Boolean> = preferences.map { it[AOD_ACTIVATE_ON_LOCK] ?: true }
    suspend fun setAodActivateOnLock(activate: Boolean) { context.dataStore.edit { it[AOD_ACTIVATE_ON_LOCK] = activate } }

    val aodTimeoutSeconds: Flow<Int> = preferences.map { it[AOD_TIMEOUT_SECONDS] ?: 0 } // 0 means never
    suspend fun setAodTimeoutSeconds(seconds: Int) { context.dataStore.edit { it[AOD_TIMEOUT_SECONDS] = seconds } }

    val aodWarningDismissed: Flow<Boolean> = preferences.map { it[AOD_WARNING_DISMISSED] ?: false }
    suspend fun setAodWarningDismissed(dismissed: Boolean) { context.dataStore.edit { it[AOD_WARNING_DISMISSED] = dismissed } }

    val aodClockColor: Flow<Int> = preferences.map { it[AOD_CLOCK_COLOR] ?: android.graphics.Color.WHITE }
    suspend fun setAodClockColor(color: Int) { context.dataStore.edit { it[AOD_CLOCK_COLOR] = color } }

    val aodBatteryColor: Flow<Int> = preferences.map { it[AOD_BATTERY_COLOR] ?: android.graphics.Color.WHITE }
    suspend fun setAodBatteryColor(color: Int) { context.dataStore.edit { it[AOD_BATTERY_COLOR] = color } }

    val aodFontFamily: Flow<String> = preferences.map { it[AOD_FONT_FAMILY] ?: "monospace" }
    suspend fun setAodFontFamily(font: String) { context.dataStore.edit { it[AOD_FONT_FAMILY] = font } }

    val aodBurnInProtection: Flow<Boolean> = preferences.map { it[AOD_BURN_IN_PROTECTION] ?: true }
    suspend fun setAodBurnInProtection(enabled: Boolean) { context.dataStore.edit { it[AOD_BURN_IN_PROTECTION] = enabled } }

    val aodBurnInInterval: Flow<Int> = preferences.map { it[AOD_BURN_IN_INTERVAL] ?: 20 }
    suspend fun setAodBurnInInterval(seconds: Int) { context.dataStore.edit { it[AOD_BURN_IN_INTERVAL] = seconds } }

    val aodBurnInMode: Flow<String> = preferences.map { it[AOD_BURN_IN_MODE] ?: "jump" }
    suspend fun setAodBurnInMode(mode: String) { context.dataStore.edit { it[AOD_BURN_IN_MODE] = mode } }

    val aodBurnInRgbShift: Flow<Boolean> = preferences.map { it[AOD_BURN_IN_RGB_SHIFT] ?: false }
    suspend fun setAodBurnInRgbShift(enabled: Boolean) { context.dataStore.edit { it[AOD_BURN_IN_RGB_SHIFT] = enabled } }

    val aodDismissPowerButton: Flow<Boolean> = preferences.map { it[AOD_DISMISS_POWER_BUTTON] ?: true }
    suspend fun setAodDismissPowerButton(enabled: Boolean) { context.dataStore.edit { it[AOD_DISMISS_POWER_BUTTON] = enabled } }

    val aodDismissDoubleTap: Flow<Boolean> = preferences.map { it[AOD_DISMISS_DOUBLE_TAP] ?: true }
    suspend fun setAodDismissDoubleTap(enabled: Boolean) { context.dataStore.edit { it[AOD_DISMISS_DOUBLE_TAP] = enabled } }

    val aodShowWattage: Flow<Boolean> = preferences.map { it[AOD_SHOW_WATTAGE] ?: false }
    suspend fun setAodShowWattage(show: Boolean) { context.dataStore.edit { it[AOD_SHOW_WATTAGE] = show } }

    val aodAsForegroundService: Flow<Boolean> = preferences.map { it[AOD_AS_FOREGROUND_SERVICE] ?: false }
    suspend fun setAodAsForegroundService(enabled: Boolean) { context.dataStore.edit { it[AOD_AS_FOREGROUND_SERVICE] = enabled } }
}
