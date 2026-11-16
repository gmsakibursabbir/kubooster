package com.kuduy.kubooster.feature.fps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kuduy.kubooster.data.compatibility.CompatibilityEngine
import com.kuduy.kubooster.data.compatibility.FpsProfileEngine
import com.kuduy.kubooster.data.compatibility.ManifestEvaluation
import com.kuduy.kubooster.di.AppContainer
import com.kuduy.kubooster.domain.model.CompatibilityManifest
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.DetectionState
import com.kuduy.kubooster.domain.model.FpsOption
import com.kuduy.kubooster.domain.model.FpsProfile
import com.kuduy.kubooster.domain.model.GameVariant
import com.kuduy.kubooster.domain.model.VerificationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class FpsScreenState(
    val loading: Boolean = true,
    val error: String? = null,
    /** Real display facts, shown separately from FPS profile states. */
    val currentRefreshRateHz: Float? = null,
    val supportedRefreshRatesHz: List<Float> = emptyList(),
    val profiles: List<FpsProfile> = emptyList(),
    val manifestStatus: VerificationStatus = VerificationStatus.UNKNOWN,
    val manifestTargetVersion: String? = null,
    val verifiedAt: String? = null,
    val gameInstalled: Boolean = false,
)

class FpsViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(FpsScreenState())
    val state: StateFlow<FpsScreenState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val device: DeviceCapabilities = container.deviceRepository.getDeviceCapabilities()
                // Honor the user-pinned variant; fall back to auto-detect when the
                // pinned game is not installed.
                val pinnedName = container.userPreferences.selectedGameVariant.firstOrNull()
                val pinnedMeta = pinnedName?.let { name ->
                    GameVariant.entries.firstOrNull { it.name == name }?.let {
                        container.gameDetectionRepository.detectVariant(it)
                    }
                }
                val game = if (pinnedMeta != null && pinnedMeta.state != DetectionState.NOT_INSTALLED) {
                    pinnedMeta
                } else {
                    container.gameDetectionRepository.detectInstalledGame()
                }
                val manifest: CompatibilityManifest = container.compatibilityRepository.loadManifest()
                val variant = if (game.state == DetectionState.INSTALLED) game.variant else GameVariant.GLOBAL
                val target = manifest.entries.firstOrNull {
                    it.gameVariant == variant.name && it.packageName == variant.packageName
                }
                // Verified only when manifest documents evidence AND version matches.
                val verified = if (
                    target != null &&
                    target.verificationStatus.equals("verified", true) &&
                    !target.evidence.isNullOrBlank() &&
                    target.verifiedFpsProfiles.isNotEmpty() &&
                    game.versionName == target.versionName
                ) {
                    target.verifiedFpsProfiles.mapNotNull { label ->
                        FpsOption.entries.firstOrNull { it.hz.toString() == label.trim() || it.label.startsWith("$label ") }
                    }
                } else emptyList()
                val engine: FpsProfileEngine = container.fpsEngine
                val profiles = engine.evaluate(device, device.display, verified)
                _state.value = FpsScreenState(
                    loading = false,
                    currentRefreshRateHz = device.display.currentRefreshRateHz.takeIf { it > 0 },
                    supportedRefreshRatesHz = device.display.supportedRefreshRatesHz,
                    profiles = profiles,
                    manifestStatus = target?.let {
                        if (it.verificationStatus.equals("verified", true) && !it.evidence.isNullOrBlank())
                            VerificationStatus.VERIFIED else VerificationStatus.UNVERIFIED
                    } ?: VerificationStatus.UNKNOWN,
                    manifestTargetVersion = target?.versionName,
                    verifiedAt = target?.verifiedAt,
                    gameInstalled = game.state == DetectionState.INSTALLED,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = "FPS capability data could not be read (${e.javaClass.simpleName}).",
                )
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FpsViewModel(container) as T
        }
    }
}
