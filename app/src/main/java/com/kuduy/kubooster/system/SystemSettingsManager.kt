package com.kuduy.kubooster.system

import android.content.Context
import android.provider.Settings

/**
 * Thin, audited wrapper over Settings.System / Settings.Global writes.
 *
 * Rules:
 *  - Only the constants declared here may ever be written. There is no API that
 *    accepts arbitrary keys.
 *  - Every write is performed through a typed method with a fixed key.
 *  - Callers must check the relevant canWrite() state before invoking.
 */
class SystemSettingsManager(private val context: Context) {

    // ---- Settings.System (requires WRITE_SETTINGS granted via the user dialog) ----

    fun canWriteSystemSettings(): Boolean = Settings.System.canWrite(context)

    fun readSystemFloat(key: String): Float? =
        Settings.System.getString(context.contentResolver, key)?.toFloatOrNull()

    fun readSystemInt(key: String): Int? =
        Settings.System.getInt(context.contentResolver, key).takeIf { key in WRITEABLE_SYSTEM_KEYS }

    fun writePeakRefreshRate(value: Float): Boolean = try {
        Settings.System.putFloat(context.contentResolver, KEY_PEAK_REFRESH_RATE, value)
    } catch (_: Exception) {
        false
    }

    fun writeMinRefreshRate(value: Float): Boolean = try {
        Settings.System.putFloat(context.contentResolver, KEY_MIN_REFRESH_RATE, value)
    } catch (_: Exception) {
        false
    }

    fun writeBrightness(value: Int): Boolean = try {
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value.coerceIn(1, 255))
    } catch (_: Exception) {
        false
    }

    fun readBrightness(): Int? = try {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    } catch (_: Exception) {
        null
    }

    /**
     * Bug-fix companion to [readBrightness]: while SCREEN_BRIGHTNESS_MODE is
     * AUTO, Android ignores manual brightness writes, so any write would be
     * unverifiable theater. Callers must treat AUTO as unsupported.
     */
    fun readBrightnessMode(): Int? = try {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
    } catch (_: Exception) {
        null
    }

    // ---- Settings.Global (requires WRITE_SECURE_SETTINGS; granted via Shizuku op) ----

    fun canWriteSecureSettings(): Boolean = try {
        // No-op write of the current value to an allowlisted key: succeeds only
        // when WRITE_SECURE_SETTINGS is actually granted.
        val current = Settings.Global.getFloat(context.contentResolver, KEY_LOW_POWER, 0f)
        Settings.Global.putFloat(context.contentResolver, KEY_LOW_POWER, current)
    } catch (_: Exception) {
        false
    }

    fun readGlobalString(key: String): String? =
        Settings.Global.getString(context.contentResolver, key)

    fun writeGlobalFloat(key: String, value: Float): Boolean = try {
        Settings.Global.putFloat(context.contentResolver, key, value)
    } catch (_: Exception) {
        false
    }

    fun writeAnimationScale(scale: Float): Boolean = try {
        var ok = true
        ok = ok && Settings.Global.putFloat(
            context.contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, scale,
        )
        ok = ok && Settings.Global.putFloat(
            context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, scale,
        )
        ok = ok && Settings.Global.putFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale,
        )
        ok
    } catch (_: Exception) {
        false
    }

    fun readGlobalFloat(key: String): Float? =
        Settings.Global.getFloat(context.contentResolver, key).takeIf { key in WRITEABLE_GLOBAL_KEYS }

    companion object {
        const val KEY_PEAK_REFRESH_RATE = "peak_refresh_rate"
        const val KEY_MIN_REFRESH_RATE = "min_refresh_rate"
        const val KEY_LOW_POWER = "low_power"

        /** Audit allowlist: the only Settings.Global keys this app may touch. */
        val WRITEABLE_GLOBAL_KEYS = setOf(
            KEY_LOW_POWER,
            Settings.Global.WINDOW_ANIMATION_SCALE,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            Settings.Global.ANIMATOR_DURATION_SCALE,
        )

        /** The only Settings.System keys this app may touch. */
        val WRITEABLE_SYSTEM_KEYS = setOf(
            KEY_PEAK_REFRESH_RATE,
            KEY_MIN_REFRESH_RATE,
            Settings.System.SCREEN_BRIGHTNESS,
        )
    }
}
