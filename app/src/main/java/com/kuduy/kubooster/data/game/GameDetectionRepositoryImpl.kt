package com.kuduy.kubooster.data.game

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.kuduy.kubooster.core.common.AppDispatchers
import com.kuduy.kubooster.domain.model.DetectionState
import com.kuduy.kubooster.domain.model.GameMetadata
import com.kuduy.kubooster.domain.model.GameVariant
import com.kuduy.kubooster.domain.repository.GameDetectionRepository
import kotlinx.coroutines.withContext

/**
 * PUBG Game Detection Engine. Reads PackageManager metadata for the four
 * known variants. The manifest declares exactly these packages, so package
 * visibility is narrow by design.
 */
class GameDetectionRepositoryImpl(
    private val context: Context,
    private val dispatchers: AppDispatchers,
) : GameDetectionRepository {

    override suspend fun detectInstalledGame(): GameMetadata = withContext(dispatchers.io) {
        detectAllVariants().firstOrNull { it.state == DetectionState.INSTALLED }
            ?: GameMetadata(
                variant = GameVariant.GLOBAL,
                state = DetectionState.NOT_INSTALLED,
                installedPackage = null,
                versionName = null,
                versionCode = null,
                applicationLabel = null,
                installerSource = null,
                lastUpdateTime = null,
            )
    }

    override suspend fun detectVariant(variant: GameVariant): GameMetadata = withContext(dispatchers.io) {
        detectVariantInternal(variant)
    }

    override suspend fun detectAllVariants(): List<GameMetadata> = withContext(dispatchers.io) {
        GameVariant.entries.map { detectVariantInternal(it) }
    }

    private fun detectVariantInternal(variant: GameVariant): GameMetadata {
        val pm = context.packageManager
        val info: PackageInfo? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(variant.packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(variant.packageName, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }

        if (info == null) {
            return GameMetadata(
                variant = variant,
                state = DetectionState.NOT_INSTALLED,
                installedPackage = null,
                versionName = null,
                versionCode = null,
                applicationLabel = null,
                installerSource = null,
                lastUpdateTime = null,
            )
        }

        // versionCode lives in longVersionCode on all supported APIs.
        val versionCode = info.longVersionCode.takeIf { it > 0 }
        val versionName = info.versionName
        val appInfo: ApplicationInfo? = try { info.applicationInfo } catch (_: Exception) { null }
        val label = appInfo?.let {
            try { pm.getApplicationLabel(it).toString() } catch (_: Exception) { null }
        }
        val installer = appInfo?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try { pm.getInstallSourceInfo(it.packageName).installingPackageName } catch (_: Exception) { null }
            } else {
                @Suppress("DEPRECATION")
                try { pm.getInstallerPackageName(it.packageName) } catch (_: Exception) { null }
            }
        }

        val state = when {
            versionName.isNullOrBlank() && versionCode == null -> DetectionState.VERSION_UNREADABLE
            else -> DetectionState.INSTALLED
        }

        return GameMetadata(
            variant = variant,
            state = state,
            installedPackage = variant.packageName,
            versionName = versionName,
            versionCode = versionCode,
            applicationLabel = label,
            installerSource = installer,
            lastUpdateTime = info.lastUpdateTime.takeIf { it > 0 },
        )
    }
}
