package io.github.itsmelissadev.swiftsense.feature.appmanager

import android.content.pm.ApplicationInfo
import io.github.itsmelissadev.swiftsense.R
import org.json.JSONArray
import org.json.JSONObject

enum class AppFilter(val titleRes: Int) {
    ALL(R.string.filter_all),
    USER(R.string.filter_user),
    SYSTEM(R.string.filter_system),
    DISABLED(R.string.filter_disabled),
    ENABLED(R.string.filter_enabled)
}

data class RichAppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val isEnabled: Boolean,
    val appInfo: ApplicationInfo,
    val versionName: String = "",
    val versionCode: Long = 0L,
    val targetSdkVersion: Int = 0,
    val minSdkVersion: Int = 0
)

data class AppPackageList(
    val id: String,
    val name: String,
    val packages: Set<String>
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            val array = JSONArray()
            packages.forEach { array.put(it) }
            put("packages", array)
        }.toString()
    }

    companion object {
        fun fromJson(json: String): AppPackageList {
            val obj = JSONObject(json)
            val id = obj.optString("id", "")
            val name = obj.optString("name", "")
            val packages = mutableSetOf<String>()
            val array = obj.optJSONArray("packages")
            if (array != null) {
                for (i in 0 until array.length()) {
                    packages.add(array.getString(i))
                }
            }
            return AppPackageList(id = id, name = name, packages = packages)
        }
    }
}
