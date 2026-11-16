package com.kuduy.kubooster.data.graphics

import com.kuduy.kubooster.data.compatibility.GraphicsRecommendationEngine
import com.kuduy.kubooster.data.preferences.UserPreferencesRepository
import com.kuduy.kubooster.domain.model.CustomGraphicsProfile
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.DisplayCapabilities
import com.kuduy.kubooster.domain.model.GraphicsPreset
import com.kuduy.kubooster.domain.model.GraphicsRecommendation
import com.kuduy.kubooster.domain.model.ThermalStatus
import com.kuduy.kubooster.domain.repository.GraphicsProfileRepository
import kotlinx.coroutines.flow.first

/**
 * Graphics presets + custom profile persistence. Recommendations come from the
 * engine; the custom profile lives in DataStore.
 */
class GraphicsProfileRepositoryImpl(
    private val engine: GraphicsRecommendationEngine,
    private val preferences: UserPreferencesRepository,
) : GraphicsProfileRepository {

    override suspend fun getRecommendations(device: DeviceCapabilities): List<GraphicsRecommendation> =
        engine.recommend(device, device.display)

    override suspend fun getCustomProfile(): CustomGraphicsProfile = preferences.customProfile.first()

    override suspend fun saveCustomProfile(profile: CustomGraphicsProfile) {
        preferences.saveCustomProfile(profile)
    }

    /**
     * Resolves which preset the dashboard should surface, adjusting for live
     * thermal status without mutating the stored preference.
     */
    suspend fun effectivePreset(device: DeviceCapabilities): GraphicsPreset {
        val stored = getCustomProfile().selectedPreset
        val hot = device.thermal.status == ThermalStatus.SEVERE ||
            device.thermal.status == ThermalStatus.CRITICAL ||
            device.thermal.status == ThermalStatus.EMERGENCY
        return if (hot && stored == GraphicsPreset.HIGH_QUALITY) GraphicsPreset.SMOOTH_PERFORMANCE else stored
    }

    companion object {
        val displayCapsForReference: (DisplayCapabilities) -> Unit = {}
    }
}
