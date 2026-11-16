package com.kuduy.kubooster.domain.model

/** Confidence used in the compatibility report. */
enum class CompatibilityConfidence { HIGH, MEDIUM, LOW, NONE }

/**
 * Central output of the CompatibilityEngine consumed by the UI.
 * Every field states what is known and what is explicitly unknown.
 */
data class CompatibilityReport(
    val deviceSupported: Boolean,
    val gameDetected: Boolean,
    val gameVersionVerified: Boolean,
    val detectedVariant: GameVariant?,
    val installedVersionName: String?,
    val versionCheck: GameVersionCheck,
    /** Display refresh rate currently active, in Hz. Null when unreadable. */
    val displayRefreshRateHz: Float?,
    /**
     * FPS options with documented verification. Never contains options that are
     * merely plausible; see [FpsProfileState.VERIFIED_AVAILABLE].
     */
    val verifiedFpsOptions: List<FpsOption>,
    val graphicsRecommendations: List<GraphicsRecommendation>,
    val unsupportedFeatures: List<String>,
    val limitations: List<String>,
    val compatibilityConfidence: CompatibilityConfidence,
    /** When the manifest evidence was last recorded, ISO-8601, or null. */
    val verificationDate: String?,
    /** Short human readable summary for the dashboard. */
    val summary: String,
    val evaluatedAtEpochMs: Long,
)

/**
 * One optimization action the engine can propose, evaluate and apply.
 * IDs are stable constants — never user supplied strings.
 */
enum class OptimizationOperationId {
    /** Prefer the highest refresh rate the display advertises (peak_refresh_rate). */
    SET_PEAK_REFRESH_RATE,
    /** Keep the display from dropping below the current rate (min_refresh_rate). */
    SET_MIN_REFRESH_RATE,
    /** Reduce screen brightness during gaming (reversible). */
    SET_REDUCED_BRIGHTNESS,
    /** Turn battery saver off while gaming (requires WRITE_SECURE_SETTINGS). */
    DISABLE_BATTERY_SAVER,
    /** Speed up or disable system animations (requires WRITE_SECURE_SETTINGS). */
    SET_ANIMATION_SCALES,
}

enum class OptimizationCategory { DISPLAY, POWER, UI_RESPONSE }

/**
 * Proposed change before execution.
 */
data class OptimizationProposal(
    val operationId: OptimizationOperationId,
    val category: OptimizationCategory,
    val title: String,
    val description: String,
    /** True when the operation is supported on this device right now. */
    val supported: Boolean,
    /** Why the operation is unavailable, when not supported. */
    val unsupportedReason: String?,
    /** True when a restoration mechanism exists (we can always read back originals). */
    val reversible: Boolean,
)

/**
 * Result of applying one operation. Applied only when the change was actually
 * written AND verified by reading it back. Partial results are reported as such.
 */
data class OptimizationResult(
    val operationId: OptimizationOperationId,
    val applied: Boolean,
    val verified: Boolean,
    val message: String,
    /** Restoration record id when applied and reversible, null otherwise. */
    val restorationId: String?,
)

data class OptimizationReport(
    val results: List<OptimizationResult>,
    val successfulCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val appliedAtEpochMs: Long,
)

/**
 * Transaction journal entry for the Restoration Manager.
 */
data class RestorationTransaction(
    val operationId: OptimizationOperationId,
    val settingKey: String,
    val originalValue: String?,
    val appliedValue: String,
    val appliedAtEpochMs: Long,
    val verified: Boolean,
    val restorable: Boolean,
)

data class RestorationResult(
    val operationId: OptimizationOperationId,
    val restored: Boolean,
    val verified: Boolean,
    val message: String,
)
