package com.kuduy.kubooster.data.compatibility

import android.content.Context
import com.kuduy.kubooster.core.common.AppDispatchers
import com.kuduy.kubooster.data.game.GameDetectionRepositoryImpl
import com.kuduy.kubooster.domain.model.CompatibilityManifest
import com.kuduy.kubooster.domain.model.CompatibilityReport
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.DisplayCapabilities
import com.kuduy.kubooster.domain.repository.CompatibilityRepository
import kotlinx.coroutines.withContext

/**
 * Loads the bundled manifest from assets and delegates report construction to
 * the CompatibilityEngine. Fully offline.
 */
class CompatibilityRepositoryImpl(
    private val context: Context,
    private val dispatchers: AppDispatchers,
    private val engine: CompatibilityEngine,
    private val parser: ManifestParser,
) : CompatibilityRepository {

    @Volatile
    private var cached: CompatibilityManifest? = null

    suspend fun loadManifest(): CompatibilityManifest = withContext(dispatchers.io) {
        cached ?: context.assets.open(MANIFEST_ASSET).bufferedReader().use { reader ->
            parser.parse(reader.readText())
        }.also { cached = it }
    }

    override suspend fun buildReport(
        device: DeviceCapabilities,
        display: DisplayCapabilities,
    ): CompatibilityReport = withContext(dispatchers.default) {
        engine.build(device, display)
    }

    companion object {
        const val MANIFEST_ASSET = "compatibility_manifest.json"
    }
}
