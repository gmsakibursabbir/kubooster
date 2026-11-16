package com.kuduy.kubooster.data.device

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import com.kuduy.kubooster.core.common.AppDispatchers
import com.kuduy.kubooster.domain.model.AndroidCapabilities
import com.kuduy.kubooster.domain.model.BatteryCapabilities
import com.kuduy.kubooster.domain.model.BatteryChargingState
import com.kuduy.kubooster.domain.model.CpuCapabilities
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.DeviceIdentity
import com.kuduy.kubooster.domain.model.GpuCapabilities
import com.kuduy.kubooster.domain.model.MemoryCapabilities
import com.kuduy.kubooster.domain.model.RamTier
import com.kuduy.kubooster.domain.model.ThermalCapabilities
import com.kuduy.kubooster.domain.model.ThermalStatus
import com.kuduy.kubooster.domain.repository.DeviceInfoRepository
import com.kuduy.kubooster.system.DisplayCapabilitiesProvider
import kotlinx.coroutines.withContext

/**
 * Device Intelligence Engine. All detection uses public Android APIs; every
 * unavailable value stays null and the UI renders "Unknown" — nothing is guessed.
 */
class DeviceInfoRepositoryImpl(
    private val context: Context,
    private val dispatchers: AppDispatchers,
    private val displayProvider: DisplayCapabilitiesProvider,
) : DeviceInfoRepository {

    override suspend fun getDeviceCapabilities(): DeviceCapabilities = withContext(dispatchers.io) {
        DeviceCapabilities(
            identity = readIdentity(),
            cpu = readCpu(),
            gpu = readGpu(),
            memory = readMemory(),
            display = displayProvider.read(),
            thermal = readThermal(),
            battery = readBattery(),
            android = readAndroidCapabilities(),
        )
    }

    override suspend fun getAvailableRamBytes(): Long = withContext(dispatchers.io) {
        readMemory().availableRamBytes
    }

    override suspend fun getThermalCapabilities(): ThermalCapabilities = withContext(dispatchers.io) { readThermal() }

    override suspend fun getBatteryCapabilities(): BatteryCapabilities = withContext(dispatchers.io) { readBattery() }

    private fun readIdentity(): DeviceIdentity = DeviceIdentity(
        manufacturer = Build.MANUFACTURER.ifBlank { "Unknown" },
        brand = Build.BRAND.ifBlank { "Unknown" },
        model = Build.MODEL.ifBlank { "Unknown" },
        deviceCodename = Build.DEVICE.ifBlank { "Unknown" },
        androidVersion = Build.VERSION.RELEASE.orEmpty().ifBlank { "Unknown" },
        apiLevel = Build.VERSION.SDK_INT,
        securityPatchLevel = Build.VERSION.SECURITY_PATCH.takeIf { !it.isNullOrBlank() },
    )

    private fun readCpu(): CpuCapabilities {
        val abis = Build.SUPPORTED_ABIS?.filter { it.isNotBlank() }.orEmpty().ifEmpty { listOf("Unknown") }
        return CpuCapabilities(
            processorCount = Runtime.getRuntime().availableProcessors(),
            supportedAbis = abis,
            is64Bit = abis.firstOrNull()?.endsWith("64") == true,
        )
    }

    private fun readGpu(): GpuCapabilities {
        val pkgManager = context.packageManager
        val supportsVulkan = pkgManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        val vulkanVersion = if (supportsVulkan) {
            @Suppress("Deprecation") // The documented way to read the advertised Vulkan version.
            pkgManager.getSystemFeatureVersionOrNull(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
        } else {
            null
        }
        return GpuCapabilities(
            renderer = EglInfo.renderer(),
            vendor = EglInfo.vendor(),
            openGlEsMajorVersion = EglInfo.glesVersion()?.major,
            openGlEsMinorVersion = EglInfo.glesVersion()?.minor,
            supportsVulkan = supportsVulkan,
            vulkanVersion = vulkanVersion,
        )
    }

    private fun readMemory(): MemoryCapabilities {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        val totalRam = memoryInfo.totalMem
        return MemoryCapabilities(
            totalRamBytes = totalRam,
            availableRamBytes = memoryInfo.availMem,
            lowMemoryThresholdBytes = memoryInfo.threshold,
            isLowMemory = memoryInfo.lowMemory,
            ramTier = RamTier.fromTotalBytes(totalRam),
        )
    }

    private fun readThermal(): ThermalCapabilities {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager.currentThermalStatus >= 0) {
            mapThermalStatus(powerManager.currentThermalStatus)
        } else {
            ThermalStatus.UNAVAILABLE
        }
        val temperature = HardwareTemperature.deviceTemperatureCelsius(context)
        return ThermalCapabilities(status = status, deviceTemperatureCelsius = temperature)
    }

    private fun mapThermalStatus(apiStatus: Int): ThermalStatus = when (apiStatus) {
        PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.NONE
        PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
        PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
        PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.CRITICAL
        PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.EMERGENCY
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.SHUTDOWN
        else -> ThermalStatus.UNAVAILABLE
    }

    @SuppressLint("BatteryLife") // Sticky battery intent is a documented read-only query.
    private fun readBattery(): BatteryCapabilities {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percentage = if (level in 0..Int.MAX_VALUE && scale > 0) {
            ((level * 100.0) / scale).toInt().coerceIn(0, 100)
        } else {
            null
        }
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val chargingState = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> BatteryChargingState.CHARGING
            BatteryManager.BATTERY_STATUS_FULL -> BatteryChargingState.FULL
            BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryChargingState.DISCHARGING
            else -> BatteryChargingState.UNKNOWN
        }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isSaverActive = powerManager.isPowerSaveMode
        // Battery health: Android 14's STATE_OF_HEALTH property is not part of the
        // public SDK surface, so we do not read it. The UI reports "not exposed"
        // instead of an undocumented value.
        val health: Int? = null
        return BatteryCapabilities(
            percentage = percentage,
            chargingState = chargingState,
            isBatterySaverActive = isSaverActive,
            healthPercent = health,
        )
    }

    private fun readAndroidCapabilities(): AndroidCapabilities = AndroidCapabilities(
        supportsDisplayModes = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M,
        supportsThermalStatusApi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        supportsHapticEngine = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1,
        supportsBatteryPropertyStateOfHealth = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    )
}

private fun PackageManager.getSystemFeatureVersionOrNull(feature: String): Long? = try {
    @Suppress("DEPRECATION") // FeatureInfo.version is the documented Vulkan version carrier.
    getSystemAvailableFeatures()?.firstOrNull { it.name == feature }?.version?.let { if (it > 0) it.toLong() else null }
} catch (_: Exception) {
    null
}
