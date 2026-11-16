package com.kuduy.kubooster.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kuduy.kubooster.data.shizuku.ShizukuRepositoryImpl
import com.kuduy.kubooster.di.AppContainer
import com.kuduy.kubooster.domain.model.BatteryChargingState
import com.kuduy.kubooster.domain.model.CompatibilityReport
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.DetectionState
import com.kuduy.kubooster.domain.model.FpsOption
import com.kuduy.kubooster.domain.model.FpsProfile
import com.kuduy.kubooster.domain.model.FpsProfileState
import com.kuduy.kubooster.domain.model.GameMetadata
import com.kuduy.kubooster.domain.model.RamTier
import com.kuduy.kubooster.domain.model.ThermalStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class DashboardState(
    val loading: Boolean = true,
    val device: DeviceCapabilities? = null,
    val game: GameMetadata? = null,
    val compatibility: CompatibilityReport? = null,
    /** Recommended profile computed by the FPS engine from real data. */
    val recommendedFps: FpsProfile? = null,
    val shizukuInstalled: Boolean = false,
    val shizukuRunning: Boolean = false,
    val shizukuAuthorized: Boolean = false,
    val errorMessage: String? = null,
) {
    val deviceLabel: String
        get() = device?.identity?.let { "${it.manufacturer} ${it.model}" } ?: "Detecting…"

    val refreshRateLabel: String
        get() = device?.display?.currentRefreshRateHz?.takeIf { it > 0 }
            ?.let { "${it.roundToInt()} Hz" } ?: "Unknown"

    val ramLabel: String
        get() = device?.memory?.let {
            val total = it.totalRamBytes / RamTier.GB.coerceAtLeast(1)
            if (total > 0) "$total GB" else null
        } ?: "Unknown"

    val batteryLabel: String
        get() = device?.battery?.let {
            val pct = it.percentage?.let { p -> "$p%" } ?: "?"
            when (it.chargingState) {
                BatteryChargingState.CHARGING -> "$pct charging"
                BatteryChargingState.FULL -> "$pct full"
                BatteryChargingState.DISCHARGING -> pct
                BatteryChargingState.UNKNOWN -> "Unknown"
            }
        } ?: "Unknown"

    val thermalLabel: String
        get() = device?.thermal?.status?.let { status ->
            when (status) {
                ThermalStatus.NONE -> "Cool"
                ThermalStatus.LIGHT -> "Light load"
                ThermalStatus.MODERATE -> "Warm"
                ThermalStatus.SEVERE -> "Hot"
                ThermalStatus.CRITICAL -> "Very hot"
                ThermalStatus.EMERGENCY, ThermalStatus.SHUTDOWN -> "Overheating"
                ThermalStatus.UNAVAILABLE -> "Unavailable"
            }
        } ?: "Unknown"

    val gameStatus: GameStatusTile
        get() {
            val meta = game
            return when {
                meta == null -> GameStatusTile.CHECKING
                meta.state == DetectionState.NOT_INSTALLED -> GameStatusTile.NOT_INSTALLED
                else -> GameStatusTile.INSTALLED
            }
        }
}

enum class GameStatusTile { CHECKING, INSTALLED, NOT_INSTALLED }

class DashboardViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, errorMessage = null)
            try {
                val device = container.deviceRepository.getDeviceCapabilities()
                val game = container.gameDetectionRepository.detectInstalledGame()
                val compatibility = container.compatibilityRepository.buildReport(device, device.display)
                // The FPS engine's top recommendation for this display, using only
                // manifest-verified or display-supported states.
                val verified = compatibility.verifiedFpsOptions
                val recommended: FpsProfile? = if (verified.isNotEmpty()) {
                    val top = verified.maxByOrNull { it.hz }!!
                    FpsProfile(top, FpsProfileState.VERIFIED_AVAILABLE, "Verified in the bundled compatibility manifest")
                } else {
                    val best = device.display.supportedRefreshRatesHz.maxOrNull()?.roundToInt()
                    FpsOption.entries.lastOrNull { best != null && it.hz <= best }?.let {
                        FpsProfile(
                            it,
                            FpsProfileState.DISPLAY_COMPATIBLE_GAME_UNVERIFIED,
                            "Display supports $best Hz; game support unverified",
                        )
                    }
                }
                _state.value = DashboardState(
                    loading = false,
                    device = device,
                    game = game,
                    compatibility = compatibility,
                    recommendedFps = recommended,
                    shizukuInstalled = container.shizukuRepository.isInstalled(),
                    shizukuRunning = container.shizukuRepository.isServiceAvailable(),
                    shizukuAuthorized = container.shizukuRepository.hasPermission(),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    errorMessage = "Device information could not be read (${e.javaClass.simpleName}). Close and reopen the app to retry.",
                )
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DashboardViewModel(container) as T
        }
    }
}
