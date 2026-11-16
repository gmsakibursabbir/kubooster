package com.kuduy.kubooster.data.compatibility

import com.kuduy.kubooster.data.game.GameDetectionRepositoryImpl
import com.kuduy.kubooster.domain.model.CompatibilityConfidence
import com.kuduy.kubooster.domain.model.CompatibilityManifest
import com.kuduy.kubooster.domain.model.CompatibilityReport
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.DetectionState
import com.kuduy.kubooster.domain.model.DisplayCapabilities
import com.kuduy.kubooster.domain.model.FpsOption
import com.kuduy.kubooster.domain.model.FpsProfile
import com.kuduy.kubooster.domain.model.FpsProfileState
import com.kuduy.kubooster.domain.model.GameMetadata
import com.kuduy.kubooster.domain.model.GameVariant
import com.kuduy.kubooster.domain.model.GameVersionCheck
import com.kuduy.kubooster.domain.model.RamTier
import com.kuduy.kubooster.domain.model.ThermalStatus
import com.kuduy.kubooster.domain.model.VerificationStatus
import kotlin.math.roundToInt

/**
 * Compatibility Engine. Combines device capabilities, display capabilities and
 * the bundled manifest into one trustworthy report.
 *
 * Hard rules encoded here:
 *  1. Display refresh rate and game FPS are different facts.
 *  2. verifiedFpsOptions only ever contains options the manifest documents.
 *  3. Unverified combinations stay unverified.
 */
class CompatibilityEngine(
    private val gameDetection: GameDetectionRepositoryImpl,
    private val manifestProvider: suspend () -> CompatibilityManifest,
    private val fpsEngine: FpsProfileEngine,
    private val graphicsEngine: GraphicsRecommendationEngine,
) {

    suspend fun build(device: DeviceCapabilities, display: DisplayCapabilities): CompatibilityReport {
        val manifest = manifestProvider()
        val gameMeta = gameDetection.detectInstalledGame()
        val variant = when (gameMeta.state) {
            DetectionState.INSTALLED, DetectionState.VERSION_UNREADABLE -> gameMeta.variant
            else -> null
        }

        val targetEntry = variant?.let {
            manifest.entries.firstOrNull { e -> e.gameVariant == it.name && e.packageName == it.packageName }
        }
        val versionCheck = evaluateVersionCheck(gameMeta, variant, targetEntry?.versionName)
        val verification = evaluateManifestVerification(targetEntry, gameMeta.versionName)

        val fpsProfiles = fpsEngine.evaluate(device, display, verification.verifiedFps)
        val verifiedFps = fpsProfiles
            .filter { it.state == FpsProfileState.VERIFIED_AVAILABLE }
            .map { it.option }

        val graphics = graphicsEngine.recommend(device, display)
        val unsupported = collectUnsupported(device, display, fpsProfiles)
        val limitations = collectLimitations(gameMeta, versionCheck, verification, device)
        val deviceSupported = evaluateDeviceSupport(device)
        val confidence = evaluateConfidence(device, gameMeta, versionCheck, verification, fpsProfiles)
        val summary = buildSummary(gameMeta, versionCheck, display, fpsProfiles)

        return CompatibilityReport(
            deviceSupported = deviceSupported,
            gameDetected = gameMeta.state == DetectionState.INSTALLED,
            gameVersionVerified = verification.status == VerificationStatus.VERIFIED &&
                versionCheck == GameVersionCheck.TARGET_VERSION,
            detectedVariant = variant,
            installedVersionName = gameMeta.versionName,
            versionCheck = versionCheck,
            displayRefreshRateHz = display.currentRefreshRateHz.takeIf { it > 0 },
            verifiedFpsOptions = verifiedFps,
            graphicsRecommendations = graphics,
            unsupportedFeatures = unsupported,
            limitations = limitations,
            compatibilityConfidence = confidence,
            verificationDate = verification.verifiedAt,
            summary = summary,
            evaluatedAtEpochMs = System.currentTimeMillis(),
        )
    }

    private fun evaluateVersionCheck(
        meta: GameMetadata,
        variant: GameVariant?,
        manifestTargetVersion: String?,
    ): GameVersionCheck {
        val installed = meta.versionName
        if (variant == null || installed.isNullOrBlank() || manifestTargetVersion.isNullOrBlank()) {
            return GameVersionCheck.UNKNOWN
        }
        return when (val cmp = VersionComparison.compareVersionNames(installed, manifestTargetVersion)) {
            0 -> GameVersionCheck.TARGET_VERSION
            else -> if (cmp > 0) GameVersionCheck.NEWER_THAN_TARGET else GameVersionCheck.OLDER_THAN_TARGET
        }
    }

    private fun evaluateManifestVerification(
        targetEntry: com.kuduy.kubooster.domain.model.ManifestEntry?,
        installedVersionName: String?,
    ): ManifestEvaluation {
        if (targetEntry == null) {
            return ManifestEvaluation(VerificationStatus.UNKNOWN, emptyList(), null, null)
        }
        // An entry is verified only when the manifest itself says so, documents
        // evidence, lists verified FPS options, AND the installed version matches
        // the entry's versionName exactly.
        val versionMatches = installedVersionName != null &&
            installedVersionName == targetEntry.versionName
        val documented = targetEntry.verificationStatus.equals("verified", ignoreCase = true) &&
            !targetEntry.evidence.isNullOrBlank() &&
            targetEntry.verifiedFpsProfiles.isNotEmpty() &&
            versionMatches
        val status = if (documented) VerificationStatus.VERIFIED else VerificationStatus.UNVERIFIED
        val verifiedFps = if (documented) {
            targetEntry.verifiedFpsProfiles.mapNotNull { label ->
                FpsOption.entries.firstOrNull {
                    it.label.startsWith("$label ") || it.hz.toString() == label.trim()
                }
            }
        } else {
            emptyList()
        }
        return ManifestEvaluation(
            status = status,
            verifiedFps = verifiedFps,
            verifiedAt = targetEntry.verifiedAt,
            targetVersionName = targetEntry.versionName,
        )
    }

    private fun collectUnsupported(
        device: DeviceCapabilities,
        display: DisplayCapabilities,
        fpsProfiles: List<FpsProfile>,
    ): List<String> = buildList {
        fpsProfiles
            .filter { it.state == FpsProfileState.NOT_SUPPORTED }
            .forEach { add("${it.option.label} not supported by this display") }
        if (device.gpu.renderer == null) add("GPU renderer could not be detected on this device")
        if (device.thermal.status == ThermalStatus.UNAVAILABLE) {
            add("Thermal status is not exposed by this device")
        }
        if (device.memory.ramTier == RamTier.UNKNOWN) add("Total RAM could not be read")
        if (display.supportedRefreshRatesHz.isEmpty()) {
            add("The display does not advertise its supported refresh rates")
        }
    }

    private fun collectLimitations(
        gameMeta: GameMetadata,
        versionCheck: GameVersionCheck,
        verification: ManifestEvaluation,
        device: DeviceCapabilities,
    ): List<String> = buildList {
        if (gameMeta.state == DetectionState.NOT_INSTALLED) {
            add("PUBG Mobile is not installed — recommendations are device-level only")
        }
        if (gameMeta.state == DetectionState.INSTALLED && versionCheck == GameVersionCheck.UNKNOWN) {
            add("Installed game version could not be compared with the maintained manifest")
        }
        if (verification.status == VerificationStatus.UNVERIFIED && gameMeta.state != DetectionState.NOT_INSTALLED) {
            add(
                "Game-side FPS support for ${gameMeta.variant.displayName} " +
                    "v${verification.targetVersionName ?: "?"} is not verified in the bundled manifest",
            )
        }
        add("Display refresh rate describes the screen, not the frame rate the game actually renders")
        add("KuBooster does not modify PUBG files, game settings, or anti-cheat systems")
        if (device.thermal.status == ThermalStatus.UNAVAILABLE) {
            add("Exact CPU/GPU temperature is not available from a documented source on this device")
        }
    }

    private fun evaluateDeviceSupport(device: DeviceCapabilities): Boolean {
        // Documented floor: 64-bit CPU, at least 4 GB RAM, and a display that
        // reports its modes. Anything better is a recommendation, not a gate.
        return device.cpu.is64Bit &&
            device.memory.ramTier != RamTier.UNKNOWN &&
            device.memory.totalRamBytes >= 4L * RamTier.GB &&
            device.display.supportedRefreshRatesHz.isNotEmpty()
    }

    private fun evaluateConfidence(
        device: DeviceCapabilities,
        gameMeta: GameMetadata,
        versionCheck: GameVersionCheck,
        verification: ManifestEvaluation,
        fpsProfiles: List<FpsProfile>,
    ): CompatibilityConfidence {
        if (gameMeta.state == DetectionState.NOT_INSTALLED) return CompatibilityConfidence.NONE
        var score = 0
        if (versionCheck == GameVersionCheck.TARGET_VERSION) score += 2
        if (versionCheck == GameVersionCheck.NEWER_THAN_TARGET) score += 1
        if (verification.status == VerificationStatus.VERIFIED) score += 2
        if (device.memory.ramTier == RamTier.FLAGSHIP) score += 1
        if (fpsProfiles.any { it.state == FpsProfileState.VERIFIED_AVAILABLE }) score += 1
        return when {
            score >= 5 -> CompatibilityConfidence.HIGH
            score >= 3 -> CompatibilityConfidence.MEDIUM
            score >= 1 -> CompatibilityConfidence.LOW
            else -> CompatibilityConfidence.NONE
        }
    }

    private fun buildSummary(
        gameMeta: GameMetadata,
        versionCheck: GameVersionCheck,
        display: DisplayCapabilities,
        fpsProfiles: List<FpsProfile>,
    ): String {
        if (gameMeta.state == DetectionState.NOT_INSTALLED) {
            return "PUBG Mobile is not installed. Device-level recommendations only."
        }
        val game = gameMeta.variant.displayName + " " + (gameMeta.versionName ?: "unknown version")
        val bestFps = fpsProfiles.maxByOrNull { it.option.hz }
        val fpsText = when (bestFps?.state) {
            FpsProfileState.VERIFIED_AVAILABLE -> "verified up to ${bestFps.option.label}"
            FpsProfileState.DISPLAY_COMPATIBLE_GAME_UNVERIFIED ->
                "display supports up to ${display.maxRefreshRateHz.roundToInt()} Hz; game support unverified"
            else -> "FPS options unknown on this device"
        }
        return "$game — $fpsText"
    }
}

data class ManifestEvaluation(
    val status: VerificationStatus,
    val verifiedFps: List<FpsOption>,
    val verifiedAt: String?,
    val targetVersionName: String?,
)

object VersionComparison {
    /** Numeric comparison of dotted version names, e.g. 4.7.0 vs 4.10.0. */
    fun compareVersionNames(a: String, b: String): Int {
        val pa = a.split('.').map { it.filter(Char::isDigit).ifBlank { "0" }.toIntOrNull() ?: 0 }
        val pb = b.split('.').map { it.filter(Char::isDigit).ifBlank { "0" }.toIntOrNull() ?: 0 }
        val len = maxOf(pa.size, pb.size)
        for (i in 0 until len) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }
}
