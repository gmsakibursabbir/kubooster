package com.kuduy.kubooster.data.compatibility

import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.DisplayCapabilities
import com.kuduy.kubooster.domain.model.GraphicsPreset
import com.kuduy.kubooster.domain.model.GraphicsRecommendation
import com.kuduy.kubooster.domain.model.GraphicsSettingAdvice
import com.kuduy.kubooster.domain.model.RamTier
import com.kuduy.kubooster.domain.model.ThermalCapabilities
import com.kuduy.kubooster.domain.model.ThermalStatus

/**
 * Graphics Recommendation Engine. Produces in-game setting advice derived from
 * the real device state. Recommendations only — the engine never writes game
 * configuration values.
 */
class GraphicsRecommendationEngine {

    fun recommend(device: DeviceCapabilities, display: DisplayCapabilities): List<GraphicsRecommendation> {
        val ram = device.memory.ramTier
        val thermal = device.thermal.status
        return listOf(smooth(ram, thermal), balanced(ram, thermal), highQuality(ram, thermal, display))
    }

    private fun smooth(ram: RamTier, thermal: ThermalStatus) = GraphicsRecommendation(
        preset = GraphicsPreset.SMOOTH_PERFORMANCE,
        title = "Smooth Performance",
        summary = "Stable frame pacing and the lowest thermal load.",
        settings = listOf(
            GraphicsSettingAdvice("Graphics", "Smooth"),
            GraphicsSettingAdvice("Frame Rate", "Medium (start here, raise only if stable)"),
            GraphicsSettingAdvice("Style", "Classic"),
            GraphicsSettingAdvice("Anti-Aliasing", "Disable"),
            GraphicsSettingAdvice("Auto-adjust", "Enable"),
        ),
        rationale = buildList {
            add("Smooth rendering lowers GPU load, which helps consistent frame pacing on every device tier.")
            when (ram) {
                RamTier.ENTRY, RamTier.MID -> add("${ram.name.lowercase().replaceFirstChar { it.uppercase() }}-tier memory benefits most from lower rendering demand.")
                RamTier.UNKNOWN -> add("RAM tier unknown; smooth preset is the safe default.")
                else -> {}
            }
            if (thermal in setOf(ThermalStatus.LIGHT, ThermalStatus.MODERATE, ThermalStatus.SEVERE, ThermalStatus.CRITICAL)) {
                add("Thermal status is elevated; lower demand reduces heat buildup.")
            }
        },
    )

    private fun balanced(ram: RamTier, thermal: ThermalStatus) = GraphicsRecommendation(
        preset = GraphicsPreset.BALANCED,
        title = "Balanced",
        summary = "Reasonable visuals with stable frames.",
        settings = listOf(
            GraphicsSettingAdvice("Graphics", when (ram) {
                RamTier.FLAGSHIP -> "HD"
                RamTier.HIGH -> "HD"
                RamTier.MID -> "Smooth"
                else -> "Smooth"
            }),
            GraphicsSettingAdvice("Frame Rate", when (ram) {
                RamTier.FLAGSHIP -> "High"
                RamTier.HIGH -> "High"
                else -> "Medium"
            }),
            GraphicsSettingAdvice("Anti-Aliasing", "Disable"),
            GraphicsSettingAdvice("Shadows", when (ram) {
                RamTier.FLAGSHIP -> "Enable"
                else -> "Disable"
            }),
        ),
        rationale = buildList {
            add("HD at high frame rate is only suggested when the device has RAM headroom for it.")
            if (thermal == ThermalStatus.SEVERE || thermal == ThermalStatus.CRITICAL) {
                add("Thermal status is severe or worse; prefer the Smooth preset until the device cools.")
            }
        },
    )

    private fun highQuality(ram: RamTier, thermal: ThermalStatus, display: DisplayCapabilities) = GraphicsRecommendation(
        preset = GraphicsPreset.HIGH_QUALITY,
        title = "High Quality",
        summary = "Best visuals where the device and game support them.",
        settings = listOf(
            GraphicsSettingAdvice("Graphics", if (ram == RamTier.FLAGSHIP) "HDR" else "HD"),
            GraphicsSettingAdvice("Frame Rate", "Medium or below"),
            GraphicsSettingAdvice("Anti-Aliasing", if (ram == RamTier.FLAGSHIP) "Enable" else "Disable"),
            GraphicsSettingAdvice("Shadows", "Enable"),
            GraphicsSettingAdvice("Style", "Realistic"),
        ),
        rationale = buildList {
            if (ram == RamTier.FLAGSHIP) {
                add("Flagship-tier memory can sustain HDR-quality rendering.")
            } else {
                add("This device's RAM tier may not sustain HDR rendering; HD is the safer ceiling.")
            }
            add("High quality raises frame time and thermal output; frame rate targets are kept moderate.")
            if (!display.supportsVariableRefreshRate) {
                add("Display advertises a single refresh rate, so quality increases should be conservative.")
            }
            if (thermal == ThermalStatus.SEVERE || thermal == ThermalStatus.CRITICAL) {
                add("Device is currently hot — not a good moment for this preset.")
            }
        },
    )
}
