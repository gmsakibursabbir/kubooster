package com.kuduy.kubooster.system

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display
import com.kuduy.kubooster.domain.model.DisplayCapabilities

/**
 * Display Capability Engine source. Reads DisplayManager and Display.Mode —
 * public APIs. Distinguishes the *current* refresh rate from the *advertised*
 * refresh-rate set; those are different facts and the UI treats them as such.
 */
class DisplayCapabilitiesProvider(private val context: Context) {

    fun read(): DisplayCapabilities {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            ?: return DisplayCapabilities(
                displayId = -1,
                widthPixels = 0,
                heightPixels = 0,
                densityDpi = 0,
                currentRefreshRateHz = 0f,
                supportedRefreshRatesHz = emptyList(),
                supportsVariableRefreshRate = false,
            )
        val mode = display.mode
        val density = context.resources.displayMetrics.densityDpi
        val rates = display.supportedModes
            .map { it.refreshRate }
            .filter { it > 0f }
            .distinct()
            .sorted()
        return DisplayCapabilities(
            displayId = display.displayId,
            widthPixels = mode.physicalWidth,
            heightPixels = mode.physicalHeight,
            densityDpi = density,
            currentRefreshRateHz = display.refreshRate,
            supportedRefreshRatesHz = rates,
            supportsVariableRefreshRate = rates.distinct().size > 1,
        )
    }

    companion object {
        /** Settings.System keys used on devices that honor the standard refresh-rate API. */
        const val KEY_PEAK_REFRESH_RATE = "peak_refresh_rate"
        const val KEY_MIN_REFRESH_RATE = "min_refresh_rate"
    }
}
