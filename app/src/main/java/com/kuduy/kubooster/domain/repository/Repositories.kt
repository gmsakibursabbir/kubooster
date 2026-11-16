package com.kuduy.kubooster.domain.repository

import com.kuduy.kubooster.domain.model.BatteryCapabilities
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.DisplayCapabilities
import com.kuduy.kubooster.domain.model.ThermalCapabilities
import kotlinx.coroutines.flow.StateFlow

interface DeviceInfoRepository {
    /** Full snapshot of device capabilities; runs on IO and returns quickly. */
    suspend fun getDeviceCapabilities(): DeviceCapabilities

    /** Live memory snapshot for periodic dashboard refreshes. */
    suspend fun getAvailableRamBytes(): Long

    /** Live thermal status. */
    suspend fun getThermalCapabilities(): ThermalCapabilities

    /** Live battery status. */
    suspend fun getBatteryCapabilities(): BatteryCapabilities
}

interface DisplayInfoRepository {
    suspend fun getDisplayCapabilities(): DisplayCapabilities

    /**
     * Attempt to prefer the maximum advertised refresh rate through a reversible,
     * documented API when the device honors it. Returns true only when the write
     * was accepted AND a read-back confirms the change.
     */
    suspend fun preferHighestRefreshRate(): Boolean

    /** True when the standard-mode refresh-rate preference can be modified. */
    fun canModifyRefreshRatePreference(): Boolean
}

interface GameDetectionRepository {
    /** Detects the best-known installed PUBG variant. */
    suspend fun detectInstalledGame(): com.kuduy.kubooster.domain.model.GameMetadata

    /** Detect a specific variant. */
    suspend fun detectVariant(variant: com.kuduy.kubooster.domain.model.GameVariant): com.kuduy.kubooster.domain.model.GameMetadata

    /** All variants with install state. */
    suspend fun detectAllVariants(): List<com.kuduy.kubooster.domain.model.GameMetadata>
}

interface CompatibilityRepository {
    suspend fun buildReport(device: DeviceCapabilities, display: DisplayCapabilities): com.kuduy.kubooster.domain.model.CompatibilityReport
}

interface GraphicsProfileRepository {
    suspend fun getRecommendations(device: DeviceCapabilities): List<com.kuduy.kubooster.domain.model.GraphicsRecommendation>
    suspend fun getCustomProfile(): com.kuduy.kubooster.domain.model.CustomGraphicsProfile
    suspend fun saveCustomProfile(profile: com.kuduy.kubooster.domain.model.CustomGraphicsProfile)
}

interface ShizukuRepository {
    /** Shizuku app installed on the device. */
    fun isInstalled(): Boolean

    /** Shizuku service is running and binder available. */
    fun isServiceAvailable(): Boolean

    /** Permission granted for this app. */
    fun hasPermission(): Boolean

    /** Request permission (fire and forget; observe via [permissionEvents]). */
    fun requestPermission()

    /** Hot stream of permission request outcomes. */
    val permissionEvents: kotlinx.coroutines.flow.SharedFlow<Int>

    /** Release listeners. */
    fun dispose()
}

interface OptimizationRepository {
    suspend fun buildProposals(
        device: DeviceCapabilities,
        display: DisplayCapabilities,
    ): List<com.kuduy.kubooster.domain.model.OptimizationProposal>

    suspend fun apply(proposals: List<com.kuduy.kubooster.domain.model.OptimizationProposal>): com.kuduy.kubooster.domain.model.OptimizationReport
}

interface RestorationRepository {
    suspend fun history(): List<com.kuduy.kubooster.domain.model.RestorationTransaction>
    suspend fun restoreOne(operationId: com.kuduy.kubooster.domain.model.OptimizationOperationId): com.kuduy.kubooster.domain.model.RestorationResult
    suspend fun restoreAll(): List<com.kuduy.kubooster.domain.model.RestorationResult>
    suspend fun pendingCount(): Int
}
