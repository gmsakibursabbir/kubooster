package com.kuduy.kubooster.testing

import com.kuduy.kubooster.domain.model.AndroidCapabilities
import com.kuduy.kubooster.domain.model.BatteryCapabilities
import com.kuduy.kubooster.domain.model.BatteryChargingState
import com.kuduy.kubooster.domain.model.CpuCapabilities
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.DeviceIdentity
import com.kuduy.kubooster.domain.model.DisplayCapabilities
import com.kuduy.kubooster.domain.model.GpuCapabilities
import com.kuduy.kubooster.domain.model.MemoryCapabilities
import com.kuduy.kubooster.domain.model.RamTier
import com.kuduy.kubooster.domain.model.ThermalCapabilities
import com.kuduy.kubooster.domain.model.ThermalStatus

/**
 * Test fixture builders. Fixed, documented values — never random, so failures
 * are reproducible.
 */
object Fixtures {

    fun display(
        rates: List<Float> = listOf(60f, 90f, 120f),
        current: Float = 60f,
        width: Int = 1080,
        height: Int = 2400,
    ) = DisplayCapabilities(
        displayId = 0,
        widthPixels = width,
        heightPixels = height,
        densityDpi = 400,
        currentRefreshRateHz = current,
        supportedRefreshRatesHz = rates,
        supportsVariableRefreshRate = rates.size > 1,
    )

    fun device(
        display: DisplayCapabilities = display(),
        ramBytes: Long = 8L * RamTier.GB,
        is64Bit: Boolean = true,
        thermal: ThermalStatus = ThermalStatus.NONE,
    ) = DeviceCapabilities(
        identity = DeviceIdentity(
            manufacturer = "TestManufacturer",
            brand = "TestBrand",
            model = "TestModel",
            deviceCodename = "testcodename",
            androidVersion = "14",
            apiLevel = 34,
            securityPatchLevel = "2024-11-01",
        ),
        cpu = CpuCapabilities(processorCount = 8, supportedAbis = listOf("arm64-v8a"), is64Bit = is64Bit),
        gpu = GpuCapabilities(
            renderer = "Adreno (TM) 740 (Test)",
            vendor = "Qualcomm (Test)",
            openGlEsMajorVersion = 3,
            openGlEsMinorVersion = 2,
            supportsVulkan = true,
            vulkanVersion = 0x403000,
        ),
        memory = MemoryCapabilities(
            totalRamBytes = ramBytes,
            availableRamBytes = ramBytes / 2,
            lowMemoryThresholdBytes = 256L * 1024 * 1024,
            isLowMemory = false,
            ramTier = RamTier.fromTotalBytes(ramBytes),
        ),
        display = display,
        thermal = ThermalCapabilities(status = thermal, deviceTemperatureCelsius = null),
        battery = BatteryCapabilities(
            percentage = 80,
            chargingState = BatteryChargingState.DISCHARGING,
            isBatterySaverActive = false,
            healthPercent = null,
        ),
        android = AndroidCapabilities(
            supportsDisplayModes = true,
            supportsThermalStatusApi = true,
            supportsHapticEngine = true,
            supportsBatteryPropertyStateOfHealth = true,
        ),
    )
}
