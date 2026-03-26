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
        private val STOPPER_APPS = stringSetPreferencesKey("stopper_apps")
        private val RESOLUTION_PLANS = stringSetPreferencesKey("resolution_plans")
        private val SYSTEM_MACROS = stringSetPreferencesKey("system_macros")
        val AMOLED_INTENSITY = floatPreferencesKey("amoled_intensity")
        val AMOLED_FILTER_TYPE = stringPreferencesKey("amoled_filter_type")
        val AMOLED_SHIFT_SPEED = intPreferencesKey("amoled_shift_speed")
        val AMOLED_OPACITY = floatPreferencesKey("amoled_opacity")
        val AMOLED_REFRESH_MODE = stringPreferencesKey("amoled_refresh_mode")
        val AMOLED_WARNING_DISMISSED = booleanPreferencesKey("amoled_warning_dismissed")
        val AMOLED_REGIONS = stringSetPreferencesKey("amoled_regions")
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

    // Intensity (Density) Level 0.0 - 1.0 (Default: 1.0 - Highest)
    val amoledIntensity: Flow<Float> = preferences.map { it[AMOLED_INTENSITY] ?: 1.0f }
    suspend fun setAmoledIntensity(intensity: Float) {
        context.dataStore.edit { it[AMOLED_INTENSITY] = intensity }
    }

    // Filter Type (Default: checker_grid)
    val amoledFilterType: Flow<String> =
        preferences.map { it[AMOLED_FILTER_TYPE] ?: "checker_grid" }

    suspend fun setAmoledFilterType(type: String) {
        context.dataStore.edit { it[AMOLED_FILTER_TYPE] = type }
    }

    // Cycle duration in seconds (1 to 600)
    val amoledShiftSpeed: Flow<Int> = preferences.map { it[AMOLED_SHIFT_SPEED] ?: 30 }
    suspend fun setAmoledShiftSpeed(speed: Int) {
        context.dataStore.edit { it[AMOLED_SHIFT_SPEED] = speed }
    }

    // Opacity (Default: 1.0 - 100%)
    val amoledOpacity: Flow<Float> = preferences.map { it[AMOLED_OPACITY] ?: 1.0f }
    suspend fun setAmoledOpacity(opacity: Float) {
        context.dataStore.edit { it[AMOLED_OPACITY] = opacity }
    }

    // Refresh Mode: "smooth" vs "jump" (Default: "jump")
    val amoledRefreshMode: Flow<String> = preferences.map { it[AMOLED_REFRESH_MODE] ?: "jump" }
    suspend fun setAmoledRefreshMode(mode: String) {
        context.dataStore.edit { it[AMOLED_REFRESH_MODE] = mode }
    }

    // Health warning dismissal state
    val amoledWarningDismissed: Flow<Boolean> =
        preferences.map { it[AMOLED_WARNING_DISMISSED] ?: false }

    suspend fun setAmoledWarningDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[AMOLED_WARNING_DISMISSED] = dismissed }
    }

    // Regions: "full_screen", "status_bar", "navigation_bar"
    val amoledRegions: Flow<Set<String>> =
        preferences.map { it[AMOLED_REGIONS] ?: setOf("full_screen") }

    suspend fun setAmoledRegions(regions: Set<String>) {
        context.dataStore.edit { it[AMOLED_REGIONS] = regions }
    }
}
