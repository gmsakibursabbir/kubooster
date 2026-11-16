package com.kuduy.kubooster.data.device

import android.content.Context
import android.os.HardwarePropertiesManager
import android.os.Build
import android.os.PowerManager

/**
 * Reads the device temperature only through the documented
 * HardwarePropertiesManager DEVICE_TEMPERATURE source. When the platform
 * returns UNAVAILABLE (common on many OEMs) the caller receives null and the
 * UI shows "unavailable" — never a fabricated number.
 */
object HardwareTemperature {

    fun deviceTemperatureCelsius(context: Context): Float? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
        val manager = context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE) as? HardwarePropertiesManager
            ?: return null
        return try {
            val values = manager.getDeviceTemperatures(
                HardwarePropertiesManager.DEVICE_TEMPERATURE_SKIN,
                HardwarePropertiesManager.TEMPERATURE_CURRENT,
            )
            val value = values?.firstOrNull { it.isFinite() }
            // UNAVAILABLE sentinel and physically impossible values are rejected.
            if (value != null && value != Float.MIN_VALUE && value in -20f..90f) value else null
        } catch (_: Exception) {
            null
        }
    }
}
