package com.kuduy.kubooster.domain.model

/**
 * Identity of the physical device, detected exclusively through public Android
 * APIs (Build, PackageManager, ActivityManager). When the OS does not expose a
 * value the field is null — the UI renders "Unknown". Nothing here is guessed
 * from a device-name database.
 */
data class DeviceIdentity(
    val manufacturer: String,
    val brand: String,
    val model: String,
    /** Marketing/codename from Build.DEVICE, may differ from the marketing model. */
    val deviceCodename: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatchLevel: String?,
)

data class CpuCapabilities(
    /** Number of logical processors as reported by Runtime. */
    val processorCount: Int,
    /** ABIs the app can run as, e.g. arm64-v8a. First entry is the preferred ABI. */
    val supportedAbis: List<String>,
    /** 64-bit device if the preferred ABI is a 64-bit one. */
    val is64Bit: Boolean,
)

data class GpuCapabilities(
    /** GL_RENDERER string from EGL, e.g. "Adreno (TM) 740". Null if unavailable. */
    val renderer: String?,
    /** GL_VENDOR string from EGL, e.g. "Qualcomm". Null if unavailable. */
    val vendor: String?,
    /** Highest OpenGL ES major version exposed by the EGL config, e.g. 3. Null if unavailable. */
    val openGlEsMajorVersion: Int?,
    /** Highest OpenGL ES minor version, e.g. 2. Null if unavailable. */
    val openGlEsMinorVersion: Int?,
    /**
     * True only when the device reports Vulkan support through PackageManager.
     * Null when the PackageManager feature query cannot be trusted.
     */
    val supportsVulkan: Boolean?,
    /**
     * Vulkan API version advertised by PackageManager.FEATURE_VULKAN_HARDWARE_VERSION,
     * encoded as the Android packed version. Null when not exposed.
     */
    val vulkanVersion: Long?,
)

data class MemoryCapabilities(
    /** Total device RAM in bytes as advertised by ActivityManager.MemoryInfo. */
    val totalRamBytes: Long,
    /** Currently available RAM in bytes. */
    val availableRamBytes: Long,
    val lowMemoryThresholdBytes: Long,
    /** True when the system reports active memory pressure. */
    val isLowMemory: Boolean,
    /** Coarse tier used for compatibility heuristics. */
    val ramTier: RamTier,
)

enum class RamTier {
    /** 4 GB or less. */
    ENTRY,
    /** 6 GB. */
    MID,
    /** 8 GB. */
    HIGH,
    /** 12 GB and above. */
    FLAGSHIP,
    /** RAM could not be read. */
    UNKNOWN,
    ;

    companion object {
        fun fromTotalBytes(totalRamBytes: Long?): RamTier = when {
            totalRamBytes == null -> UNKNOWN
            totalRamBytes <= 4L * GB -> ENTRY
            totalRamBytes <= 6L * GB -> MID
            totalRamBytes <= 8L * GB -> HIGH
            else -> FLAGSHIP
        }

        const val GB: Long = 1024L * 1024L * 1024L
    }
}

data class DisplayCapabilities(
    /** Display unique id, when available. */
    val displayId: Int,
    val widthPixels: Int,
    val heightPixels: Int,
    val densityDpi: Int,
    /** Refresh rate the display is running at right now, in Hz. */
    val currentRefreshRateHz: Float,
    /** All refresh rates the display advertises through Display.Mode, sorted ascending. */
    val supportedRefreshRatesHz: List<Float>,
    /**
     * True when the display advertises multiple modes with differing refresh rates
     * (variable refresh rate behavior).
     */
    val supportsVariableRefreshRate: Boolean,
) {
    val maxRefreshRateHz: Float
        get() = supportedRefreshRatesHz.maxOrNull() ?: currentRefreshRateHz
}

enum class ThermalStatus {
    NONE,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN,
    /** Thermal service unavailable on this device. */
    UNAVAILABLE,
}

data class ThermalCapabilities(
    val status: ThermalStatus,
    /**
     * Exact temperature in Celsius only when the device exposes a supported,
     * documented sensor (HardwarePropertiesManager device temperature). Null otherwise.
     */
    val deviceTemperatureCelsius: Float?,
)

enum class BatteryChargingState { CHARGING, DISCHARGING, FULL, UNKNOWN }

data class BatteryCapabilities(
    /** Charge level 0..100, or null when unobtainable. */
    val percentage: Int?,
    val chargingState: BatteryChargingState,
    /** True when the OS battery saver is currently active. */
    val isBatterySaverActive: Boolean,
    /**
     * Battery health only when the OS exposes it (Android 14
     * BatteryManager BATTERY_PROPERTY_STATE_OF_HEALTH). Null otherwise.
     */
    val healthPercent: Int?,
)

/**
 * Android version dependent capability switches used by the compatibility engine.
 */
data class AndroidCapabilities(
    val supportsDisplayModes: Boolean,
    val supportsThermalStatusApi: Boolean,
    val supportsHapticEngine: Boolean,
    val supportsBatteryPropertyStateOfHealth: Boolean,
)

/**
 * Immutable aggregate of everything the Device Intelligence Engine knows.
 */
data class DeviceCapabilities(
    val identity: DeviceIdentity,
    val cpu: CpuCapabilities,
    val gpu: GpuCapabilities,
    val memory: MemoryCapabilities,
    val display: DisplayCapabilities,
    val thermal: ThermalCapabilities,
    val battery: BatteryCapabilities,
    val android: AndroidCapabilities,
)
