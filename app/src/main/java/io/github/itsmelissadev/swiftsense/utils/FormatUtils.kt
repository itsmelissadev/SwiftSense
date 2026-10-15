package io.github.itsmelissadev.swiftsense.utils

import java.util.Locale

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", bytesPerSec.toFloat() / (1024 * 1024))
            bytesPerSec >= 1024 -> String.format(Locale.US, "%.0f KB/s", bytesPerSec.toFloat() / 1024)
            else -> "$bytesPerSec B/s"
        }
    }
}
