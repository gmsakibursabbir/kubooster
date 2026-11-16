package com.kuduy.kubooster.data.optimization

import android.content.Context
import com.kuduy.kubooster.core.common.AppDispatchers
import com.kuduy.kubooster.data.device.DeviceInfoRepositoryImpl
import com.kuduy.kubooster.data.restoration.RestorationRepositoryImpl
import com.kuduy.kubooster.data.restoration.SettingsRestorer
import com.kuduy.kubooster.domain.model.BatteryChargingState
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.DisplayCapabilities
import com.kuduy.kubooster.domain.model.OptimizationCategory
import com.kuduy.kubooster.domain.model.OptimizationOperationId
import com.kuduy.kubooster.domain.model.OptimizationProposal
import com.kuduy.kubooster.domain.model.OptimizationReport
import com.kuduy.kubooster.domain.model.OptimizationResult
import com.kuduy.kubooster.domain.model.ThermalStatus
import com.kuduy.kubooster.domain.repository.OptimizationRepository
import com.kuduy.kubooster.system.SystemSettingsManager
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Safe Optimization Engine.
 *
 * Every operation is a predefined, documented, reversible Android settings
 * change — never a game modification and never a process kill. Each apply is
 * verified by reading the value back; results are reported honestly.
 */
class OptimizationRepositoryImpl(
    private val context: Context,
    private val dispatchers: AppDispatchers,
    private val settings: SystemSettingsManager,
    private val restoration: RestorationRepositoryImpl,
    private val deviceRepository: DeviceInfoRepositoryImpl,
) : OptimizationRepository {

    override suspend fun buildProposals(
        device: DeviceCapabilities,
        display: DisplayCapabilities,
    ): List<OptimizationProposal> = withContext(dispatchers.io) {
        val maxRate = display.supportedRefreshRatesHz.maxOrNull() ?: 0f
        val currentRate = display.currentRefreshRateHz
        val displayCanRaise = maxRate > currentRate + 0.01f && settings.canWriteSystemSettings() &&
            settings.readSystemFloat(SystemSettingsManager.KEY_PEAK_REFRESH_RATE) != null

        buildList {
            add(
                OptimizationProposal(
                    operationId = OptimizationOperationId.SET_PEAK_REFRESH_RATE,
                    category = OptimizationCategory.DISPLAY,
                    title = "Prefer ${maxRate.roundToInt()} Hz display mode",
                    description = "Asks the system to use the highest refresh rate your display advertises. This changes the display mode only — the frame rate PUBG renders depends on the game and your device.",
                    supported = displayCanRaise,
                    unsupportedReason = when {
                        maxRate <= currentRate + 0.01f -> "The display already runs at its highest advertised rate"
                        !settings.canWriteSystemSettings() -> "Modify system settings permission is not granted"
                        settings.readSystemFloat(SystemSettingsManager.KEY_PEAK_REFRESH_RATE) == null ->
                            "This device does not expose the standard refresh-rate preference key"
                        else -> null
                    },
                    reversible = true,
                ),
            )
            add(
                OptimizationProposal(
                    operationId = OptimizationOperationId.SET_MIN_REFRESH_RATE,
                    category = OptimizationCategory.DISPLAY,
                    title = "Keep the display at high refresh rate",
                    description = "Sets the minimum refresh rate to the current rate so the panel does not drop modes during gameplay.",
                    supported = displayCanRaise,
                    unsupportedReason = if (displayCanRaise) null else "Follows the peak refresh rate change — same device constraints apply",
                    reversible = true,
                ),
            )

            val battery = device.battery
            if (battery.isBatterySaverActive) {
                add(
                    OptimizationProposal(
                        operationId = OptimizationOperationId.DISABLE_BATTERY_SAVER,
                        category = OptimizationCategory.POWER,
                        title = "Turn off battery saver",
                        description = "Battery saver throttles CPU and caps performance. Turning it off during a gaming session removes those limits (turn it back on afterwards).",
                        supported = canWriteSecure(),
                        unsupportedReason = if (canWriteSecure()) null else "Requires WRITE_SECURE_SETTINGS granted through Shizuku",
                        reversible = true,
                    ),
                )
            }

            val brightnessOk = settings.canWriteSystemSettings() &&
                settings.readSystemFloat(SystemSettingsManager.KEY_PEAK_REFRESH_RATE) != null &&
                settings.readBrightness() != null
            add(
                OptimizationProposal(
                    operationId = OptimizationOperationId.SET_REDUCED_BRIGHTNESS,
                    category = OptimizationCategory.POWER,
                    title = "Reduce screen brightness for gaming",
                    description = "Lowers brightness to a comfortable gaming level, reducing heat and battery drain. Your original brightness is recorded and can be restored.",
                    supported = brightnessOk,
                    unsupportedReason = if (brightnessOk) null else "System settings write permission unavailable",
                    reversible = true,
                ),
            )

            add(
                OptimizationProposal(
                    operationId = OptimizationOperationId.SET_ANIMATION_SCALES,
                    category = OptimizationCategory.UI_RESPONSE,
                    title = "Speed up system animations",
                    description = "Shortens the three Android animation scale settings (window, transition, animator) from 1.0 to 0.5, making UI transitions faster during gaming sessions.",
                    supported = canWriteSecure(),
                    unsupportedReason = if (canWriteSecure()) null else "Requires WRITE_SECURE_SETTINGS granted through Shizuku",
                    reversible = true,
                ),
            )
        }
    }

    private fun canWriteSecure(): Boolean =
        settings.readGlobalFloat(SystemSettingsManager.KEY_LOW_POWER) != null && secureWritePossible

    /**
     * Only true after a real probe write of the current low_power value succeeded.
     * Set once per process after the Shizuku flow grants WRITE_SECURE_SETTINGS.
     */
    @Volatile
    var secureWritePossible: Boolean = false
        private set

    fun markSecureWriteAvailable() {
        secureWritePossible = settings.canWriteSecureSettings().also { probed = it }
    }

    @Volatile
    private var probed: Boolean = false

    override suspend fun apply(
        proposals: List<OptimizationProposal>,
    ): OptimizationReport = withContext(dispatchers.io) {
        val results = proposals.filter { it.supported }.map { proposal ->
            applyOne(proposal)
        }
        val applied = results.count { it.applied }
        val verified = results.count { it.applied && it.verified }
        OptimizationReport(
            results = results,
            successfulCount = verified,
            failedCount = results.count { !it.applied },
            skippedCount = proposals.count { !it.supported },
            appliedAtEpochMs = System.currentTimeMillis(),
        )
    }

    private suspend fun applyOne(proposal: OptimizationProposal): OptimizationResult {
        val operationId = proposal.operationId
        return when (operationId) {
            OptimizationOperationId.SET_PEAK_REFRESH_RATE -> {
                val maxRate = settings.let { ctx ->
                    deviceRepository.getDeviceCapabilities().display.supportedRefreshRatesHz.maxOrNull()
                } ?: return failed(operationId, "Display refresh rates unreadable")
                val original = settings.readSystemFloat(SystemSettingsManager.KEY_PEAK_REFRESH_RATE)?.toString()
                val ok = settings.writePeakRefreshRate(maxRate)
                val verified = ok && settings.readSystemFloat(SystemSettingsManager.KEY_PEAK_REFRESH_RATE) == maxRate
                record(operationId, SystemSettingsManager.KEY_PEAK_REFRESH_RATE, original, maxRate.toString(), ok, verified)
                result(operationId, ok, verified, "Peak refresh rate preference set to ${maxRate.roundToInt()} Hz")
            }
            OptimizationOperationId.SET_MIN_REFRESH_RATE -> {
                val maxRate = settings.let {
                    deviceRepository.getDeviceCapabilities().display.supportedRefreshRatesHz.maxOrNull()
                } ?: return failed(operationId, "Display refresh rates unreadable")
                val original = settings.readSystemFloat(SystemSettingsManager.KEY_MIN_REFRESH_RATE)?.toString()
                val ok = settings.writeMinRefreshRate(maxRate)
                val verified = ok && settings.readSystemFloat(SystemSettingsManager.KEY_MIN_REFRESH_RATE) == maxRate
                record(operationId, SystemSettingsManager.KEY_MIN_REFRESH_RATE, original, maxRate.toString(), ok, verified)
                result(operationId, ok, verified, "Minimum refresh rate preference set to ${maxRate.roundToInt()} Hz")
            }
            OptimizationOperationId.SET_REDUCED_BRIGHTNESS -> {
                val original = settings.readBrightness()
                // 40% of 0..255 range ≈ 102 — comfortable gaming brightness.
                val target = 102
                val ok = settings.writeBrightness(target)
                val verified = ok && settings.readBrightness() == target
                record(operationId, SettingsKey.SCREEN_BRIGHTNESS, original?.toString(), target.toString(), ok, verified)
                result(operationId, ok, verified, "Brightness reduced to ~40%")
            }
            OptimizationOperationId.DISABLE_BATTERY_SAVER -> {
                val original = settings.readGlobalFloat(SystemSettingsManager.KEY_LOW_POWER)?.toString()
                val ok = settings.writeGlobalFloat(SystemSettingsManager.KEY_LOW_POWER, 0f)
                val verified = ok && settings.readGlobalFloat(SystemSettingsManager.KEY_LOW_POWER) == 0f
                record(operationId, SystemSettingsManager.KEY_LOW_POWER, original, "0", ok, verified)
                result(operationId, ok, verified, "Battery saver disabled")
            }
            OptimizationOperationId.SET_ANIMATION_SCALES -> {
                val originalWindow = settings.readGlobalFloat(android.provider.Settings.Global.WINDOW_ANIMATION_SCALE)
                val originalTransition = settings.readGlobalFloat(android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE)
                val originalAnimator = settings.readGlobalFloat(android.provider.Settings.Global.ANIMATOR_DURATION_SCALE)
                val ok = settings.writeAnimationScale(0.5f)
                val verified = ok &&
                    settings.readGlobalFloat(android.provider.Settings.Global.WINDOW_ANIMATION_SCALE) == 0.5f
                record(
                    operationId,
                    "window_animation_scale,transition_animation_scale,animator_duration_scale",
                    listOfNotNull(originalWindow, originalTransition, originalAnimator).joinToString(","),
                    "0.5,0.5,0.5",
                    ok,
                    verified,
                )
                result(operationId, ok, verified, "System animations set to 0.5x")
            }
        }
    }

    private suspend fun record(
        operationId: OptimizationOperationId,
        key: String,
        original: String?,
        applied: String,
        ok: Boolean,
        verified: Boolean,
    ) {
        if (ok) {
            restoration.record(operationId, key, original, applied, verified, restorable = true)
        }
    }

    private fun result(operationId: OptimizationOperationId, applied: Boolean, verified: Boolean, message: String) =
        OptimizationResult(
            operationId = operationId,
            applied = applied,
            verified = verified,
            message = if (applied && verified) message
            else if (applied) "$message — but verification read-back did not match"
            else "System rejected the change",
            restorationId = if (applied) operationId.name else null,
        )

    private fun failed(operationId: OptimizationOperationId, message: String) =
        OptimizationResult(operationId, applied = false, verified = false, message = message, restorationId = null)

    /** Concrete settings keys used by the journal. */
    private object SettingsKey {
        const val SCREEN_BRIGHTNESS = "screen_brightness"
    }
}
