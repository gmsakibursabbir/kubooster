package com.kuduy.kubooster.data.display

import android.content.Context
import android.provider.Settings
import com.kuduy.kubooster.core.common.AppDispatchers
import com.kuduy.kubooster.system.DisplayCapabilitiesProvider
import com.kuduy.kubooster.domain.model.DisplayCapabilities
import com.kuduy.kubooster.domain.repository.DisplayInfoRepository
import kotlinx.coroutines.withContext
import android.os.Build

/**
 * Reads display capabilities and applies the reversible refresh-rate preference
 * through Settings.System using the WRITE_SETTINGS permission the user grants
 * through the system dialog. Write is verified by read-back; failure is honest.
 */
class DisplayInfoRepositoryImpl(
    private val context: Context,
    private val dispatchers: AppDispatchers,
    private val provider: DisplayCapabilitiesProvider,
) : DisplayInfoRepository {

    override suspend fun getDisplayCapabilities(): DisplayCapabilities = withContext(dispatchers.io) {
        provider.read()
    }

    override fun canModifyRefreshRatePreference(): Boolean =
        Settings.System.canWrite(context)

    override suspend fun preferHighestRefreshRate(): Boolean = withContext(dispatchers.io) {
        val caps = provider.read()
        val maxRate = caps.supportedRefreshRatesHz.maxOrNull() ?: return@withContext false
        if (maxRate <= caps.currentRefreshRateHz + 0.01f) return@withContext false
        // Only devices that expose the documented adaptive-refresh keys are touched;
        // anything else returns false and the UI reports "not supported".
        val hasPeakKey = Settings.System.getString(context.contentResolver, KEY_PEAK) != null
        if (!hasPeakKey) return@withContext false
        val ok = try {
            Settings.System.putFloat(context.contentResolver, KEY_PEAK, maxRate)
        } catch (_: Exception) {
            false
        }
        ok && readBackPeak() == maxRate
    }

    private fun readBackPeak(): Float? =
        Settings.System.getString(context.contentResolver, KEY_PEAK)?.toFloatOrNull()

    companion object {
        const val KEY_PEAK = "peak_refresh_rate"
    }
}
