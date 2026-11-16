package com.kuduy.kubooster.data.compatibility

import com.kuduy.kubooster.domain.model.CompatibilityManifest
import com.kuduy.kubooster.domain.model.GameVariant
import com.kuduy.kubooster.domain.model.ManifestEntry
import kotlinx.serialization.json.Json

/**
 * Loads and parses the bundled compatibility manifest. Kept as a pure, JVM
 * testable class: string in, manifest out, no Android dependencies.
 */
class ManifestParser(private val json: Json = Json { ignoreUnknownKeys = true }) {

    fun parse(raw: String): CompatibilityManifest = json.decodeFromString(raw)

    /**
     * Finds the manifest entry for a variant and version name. Matching is by
     * exact versionName; never by displayed application label.
     */
    fun findEntry(
        manifest: CompatibilityManifest,
        variant: GameVariant,
        versionName: String?,
    ): ManifestEntry? = manifest.entries.firstOrNull {
        it.gameVariant == variant.name && it.packageName == variant.packageName &&
            (versionName == null || it.versionName == versionName)
    }

    /**
     * Entry for the variant regardless of installed version (manifest target row).
     */
    fun targetEntry(manifest: CompatibilityManifest, variant: GameVariant): ManifestEntry? =
        manifest.entries.firstOrNull {
            it.gameVariant == variant.name && it.packageName == variant.packageName
        }
}
