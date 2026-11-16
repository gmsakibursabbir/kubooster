package com.kuduy.kubooster.domain.model

/**
 * FPS options the profile engine reasons about. These are game-render targets,
 * strictly distinct from the display refresh rate.
 */
enum class FpsOption(val label: String, val hz: Int) {
    FPS_30("30 FPS", 30),
    FPS_40("40 FPS", 40),
    FPS_60("60 FPS", 60),
    FPS_90("90 FPS", 90),
    FPS_120("120 FPS", 120),
}

enum class FpsProfileState {
    /** Documented in the compatibility manifest for this game version. */
    VERIFIED_AVAILABLE,
    /**
     * The display supports at least this refresh rate, but the manifest carries
     * no verified game-side evidence. Never presented as guaranteed.
     */
    DISPLAY_COMPATIBLE_GAME_UNVERIFIED,
    /** Neither the display nor the manifest supports this option. */
    NOT_SUPPORTED,
}

data class FpsProfile(
    val option: FpsOption,
    val state: FpsProfileState,
    /** Explanation shown verbatim in the FPS screen. */
    val explanation: String,
)

/** Four graphics presets from the product spec. */
enum class GraphicsPreset { SMOOTH_PERFORMANCE, BALANCED, HIGH_QUALITY, CUSTOM }

data class GraphicsRecommendation(
    val preset: GraphicsPreset,
    val title: String,
    val summary: String,
    /** Concrete in-game settings the user should pick manually. */
    val settings: List<GraphicsSettingAdvice>,
    /** Reasoning tied to the actual device (RAM tier, thermal, display). */
    val rationale: List<String>,
)

data class GraphicsSettingAdvice(
    val setting: String,
    val value: String,
)

/** Custom profile persisted with DataStore. */
data class CustomGraphicsProfile(
    val preferredFpsOption: FpsOption?,
    val prioritizeBattery: Boolean,
    val prioritizeThermal: Boolean,
    val selectedPreset: GraphicsPreset,
)
