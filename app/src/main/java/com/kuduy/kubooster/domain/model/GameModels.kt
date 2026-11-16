package com.kuduy.kubooster.domain.model

import kotlinx.serialization.Serializable

/**
 * PUBG Mobile variant registry entry. Package names are verified constants;
 * variants are never assumed to share metadata.
 */
enum class GameVariant(val packageName: String, val displayName: String) {
    GLOBAL("com.tencent.ig", "PUBG Mobile (Global)"),
    FLASH("com.tencent.igfit", "PUBG Mobile Flash"),
    KR("com.pubg.krmobile", "PUBG Mobile (Korea)"),
    VN("com.vng.pubgmobile", "PUBG Mobile (Vietnam)"),
    BGMI("com.pubg.imobile", "Battlegrounds Mobile India"),
}

enum class DetectionState {
    /** Package installed and metadata readable. */
    INSTALLED,
    /** No PUBG variant found on the device. */
    NOT_INSTALLED,
    /** Package present but versionName could not be read. */
    VERSION_UNREADABLE,
    /** Package visibility blocked by an unexpected system restriction. */
    UNABLE_TO_DETECT,
}

/**
 * Real metadata read from PackageManager. No field is fabricated.
 */
data class GameMetadata(
    val variant: GameVariant,
    val state: DetectionState,
    val installedPackage: String?,
    val versionName: String?,
    val versionCode: Long?,
    val applicationLabel: String?,
    val installerSource: String?,
    val lastUpdateTime: Long?,
)

/** Verification states used across the compatibility engine. */
enum class VerificationStatus {
    /** Evidence exists in the bundled manifest for this exact combination. */
    VERIFIED,
    /** Manifest knows the combination but carries no verified evidence yet. */
    UNVERIFIED,
    /** No entry at all. */
    UNKNOWN,
}

/**
 * One row of the bundled compatibility manifest. Every field maps to a claim we
 * can trace back to documented evidence. Empty/null evidence rows are treated as
 * UNVERIFIED and never surfaced as verified facts in the UI.
 */
@Serializable
data class ManifestEntry(
    val gameVariant: String,
    val packageName: String,
    val versionName: String,
    /** Nullable: version codes are never assumed. */
    val versionCode: Long? = null,
    /** Human readable device conditions, e.g. "arm64-v8a, Android 14". */
    val supportedDeviceConditions: String? = null,
    /**
     * FPS options with documented verification, e.g. ["60"].
     * An empty list means "no FPS option verified" — never guessed.
     */
    val verifiedFpsProfiles: List<String> = emptyList(),
    val graphicsRecommendations: String? = null,
    val evidence: String? = null,
    val verificationStatus: String = "unverified",
    /** ISO-8601 date the evidence was recorded, or null when unverified. */
    val verifiedAt: String? = null,
)

/**
 * The bundled, versioned manifest. Core functionality is offline; updates ship
 * with new app versions (an in-app network fetch is intentionally not included).
 */
@Serializable
data class CompatibilityManifest(
    val manifestVersion: Int,
    val entries: List<ManifestEntry> = emptyList(),
)

/** State of PUBG detection relative to the maintained target version. */
enum class GameVersionCheck {
    /** versionName matches the manifest target exactly. */
    TARGET_VERSION,
    /** Installed version exists and is newer than the manifest target. */
    NEWER_THAN_TARGET,
    /** Installed version is older than the manifest target. */
    OLDER_THAN_TARGET,
    /** Version could not be read. */
    UNKNOWN,
}
