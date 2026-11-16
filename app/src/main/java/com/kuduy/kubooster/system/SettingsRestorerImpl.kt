package com.kuduy.kubooster.system

import android.content.Context
import android.provider.Settings
import com.kuduy.kubooster.data.restoration.SettingsRestorer
import com.kuduy.kubooster.system.SystemSettingsManager.Companion.KEY_MIN_REFRESH_RATE
import com.kuduy.kubooster.system.SystemSettingsManager.Companion.KEY_PEAK_REFRESH_RATE

/**
 * Concrete restoration backed by SystemSettingsManager. The key list is fixed;
 * an unknown key restores nothing and reports failure honestly.
 */
class SettingsRestorerImpl(
    private val context: Context,
    private val settings: SystemSettingsManager,
) : SettingsRestorer {

    override fun readCurrent(key: String): String? = when (key) {
        KEY_PEAK_REFRESH_RATE, KEY_MIN_REFRESH_RATE -> {
            settings.readSystemFloat(key)?.toString()
        }
        Settings.System.SCREEN_BRIGHTNESS -> settings.readBrightness()?.toString()
        SystemSettingsManager.KEY_LOW_POWER,
        Settings.Global.WINDOW_ANIMATION_SCALE,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        -> settings.readGlobalFloat(key)?.toString()
        // The combined animation-scale key is restored as a unit.
        ANIMATION_SCALES_KEY -> listOfNotNull(
            settings.readGlobalFloat(Settings.Global.WINDOW_ANIMATION_SCALE),
            settings.readGlobalFloat(Settings.Global.TRANSITION_ANIMATION_SCALE),
            settings.readGlobalFloat(Settings.Global.ANIMATOR_DURATION_SCALE),
        ).joinToString(",")
        else -> null
    }

    override fun restore(key: String, originalValue: String?): Boolean = when (key) {
        KEY_PEAK_REFRESH_RATE, KEY_MIN_REFRESH_RATE -> {
            val value = originalValue?.toFloatOrNull()
            value != null && settings.writeSystemFloat(key, value)
        }
        Settings.System.SCREEN_BRIGHTNESS -> {
            val value = originalValue?.toIntOrNull()
            value != null && settings.writeBrightness(value)
        }
        SystemSettingsManager.KEY_LOW_POWER -> {
            val value = originalValue?.toFloatOrNull()
            value != null && settings.writeGlobalFloat(SystemSettingsManager.KEY_LOW_POWER, value)
        }
        ANIMATION_SCALES_KEY -> {
            val parts = (originalValue ?: "").split(',')
            if (parts.size == 3) {
                val w = parts[0].toFloatOrNull() ?: return false
                val t = parts[1].toFloatOrNull() ?: return false
                val a = parts[2].toFloatOrNull() ?: return false
                settings.writeGlobalFloat(Settings.Global.WINDOW_ANIMATION_SCALE, w) &&
                    settings.writeGlobalFloat(Settings.Global.TRANSITION_ANIMATION_SCALE, t) &&
                    settings.writeGlobalFloat(Settings.Global.ANIMATOR_DURATION_SCALE, a)
            } else {
                false
            }
        }
        else -> false
    }

    companion object {
        const val ANIMATION_SCALES_KEY = "animation_scales"
    }
}

private fun SystemSettingsManager.writeSystemFloat(key: String, value: Float): Boolean = when (key) {
    KEY_PEAK_REFRESH_RATE -> writePeakRefreshRate(value)
    KEY_MIN_REFRESH_RATE -> writeMinRefreshRate(value)
    else -> false
}
