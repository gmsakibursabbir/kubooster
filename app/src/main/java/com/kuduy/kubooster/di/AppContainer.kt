package com.kuduy.kubooster.di

import android.content.Context
import com.kuduy.kubooster.core.common.AppDispatchers
import com.kuduy.kubooster.data.compatibility.CompatibilityEngine
import com.kuduy.kubooster.domain.model.CompatibilityManifest
import com.kuduy.kubooster.data.compatibility.CompatibilityRepositoryImpl
import com.kuduy.kubooster.data.compatibility.FpsProfileEngine
import com.kuduy.kubooster.data.compatibility.GraphicsRecommendationEngine
import com.kuduy.kubooster.data.compatibility.ManifestParser
import com.kuduy.kubooster.data.device.DeviceInfoRepositoryImpl
import com.kuduy.kubooster.data.device.EglInfo
import com.kuduy.kubooster.data.display.DisplayInfoRepositoryImpl
import com.kuduy.kubooster.data.game.GameDetectionRepositoryImpl
import com.kuduy.kubooster.data.graphics.GraphicsProfileRepositoryImpl
import com.kuduy.kubooster.data.optimization.OptimizationRepositoryImpl
import com.kuduy.kubooster.data.preferences.UserPreferencesRepository
import com.kuduy.kubooster.data.restoration.SettingsRestorer
import com.kuduy.kubooster.data.restoration.RestorationRepositoryImpl
import com.kuduy.kubooster.data.shizuku.ShizukuRepositoryImpl
import com.kuduy.kubooster.data.update.GithubUpdateChecker
import com.kuduy.kubooster.system.DisplayCapabilitiesProvider
import com.kuduy.kubooster.system.SettingsRestorerImpl
import com.kuduy.kubooster.system.SystemSettingsManager
import kotlinx.coroutines.withContext

/**
 * Explicit, hand-written dependency graph. Small enough that a DI framework
 * would add indirection without adding clarity.
 */
class AppContainer(val appContext: Context) {

    val dispatchers = AppDispatchers()

    val displayProvider = DisplayCapabilitiesProvider(appContext)

    val deviceRepository = DeviceInfoRepositoryImpl(appContext, dispatchers, displayProvider)

    val displayRepository = DisplayInfoRepositoryImpl(appContext, dispatchers, displayProvider)

    val gameDetectionRepository = GameDetectionRepositoryImpl(appContext, dispatchers)

    val settingsManager = SystemSettingsManager(appContext)

    val settingsRestorer: SettingsRestorer = SettingsRestorerImpl(appContext, settingsManager)

    val restorationRepository = RestorationRepositoryImpl(appContext, settingsRestorer)

    val userPreferences = UserPreferencesRepository(appContext)

    val fpsEngine = FpsProfileEngine()

    val graphicsEngine = GraphicsRecommendationEngine()

    val compatibilityRepository: CompatibilityRepositoryImpl by lazy {
        val engine = CompatibilityEngine(
            gameDetection = gameDetectionRepository,
            manifestProvider = { loadManifest() },
            fpsEngine = fpsEngine,
            graphicsEngine = graphicsEngine,
        )
        CompatibilityRepositoryImpl(appContext, dispatchers, engine, manifestParser)
    }

    private val manifestParser = ManifestParser()

    private suspend fun loadManifest(): CompatibilityManifest = withContext(dispatchers.io) {
        cachedManifest ?: manifestParser.parse(
            appContext.assets.open("compatibility_manifest.json").bufferedReader().use { it.readText() },
        ).also { cachedManifest = it }
    }

    @Volatile
    private var cachedManifest: CompatibilityManifest? = null

    val graphicsProfileRepository = GraphicsProfileRepositoryImpl(graphicsEngine, userPreferences)

    val optimizationRepository = OptimizationRepositoryImpl(
        appContext, dispatchers, settingsManager, restorationRepository, deviceRepository,
    )

    val shizukuRepository = ShizukuRepositoryImpl(appContext)

    val updateChecker = GithubUpdateChecker(dispatchers)

    /** EglInfo warms the EGL query off the main thread during onboarding detection. */
    suspend fun warmGpuInfo() {
        kotlinx.coroutines.withContext(dispatchers.default) { EglInfo.renderer() }
    }
}
