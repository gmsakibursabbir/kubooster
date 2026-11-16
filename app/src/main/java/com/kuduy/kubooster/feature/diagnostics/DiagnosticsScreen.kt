package com.kuduy.kubooster.feature.diagnostics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kuduy.kubooster.core.designsystem.InfoRow
import com.kuduy.kubooster.core.designsystem.KubeCard
import com.kuduy.kubooster.core.designsystem.SectionTitle
import com.kuduy.kubooster.domain.model.BatteryChargingState
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.RamTier
import com.kuduy.kubooster.domain.model.ThermalStatus
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Device diagnostics. Grouped, left-aligned, real values only; "Unknown"
 * carries its reason when the OS withholds the value.
 */
@Composable
fun DiagnosticsScreen(
    state: DiagnosticsState,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Diagnostics", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            if (state.loading) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
        }
        Spacer(Modifier.height(8.dp))

        when {
            state.error != null -> {
                KubeCard(Modifier.fillMaxWidth()) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onRetry) { Text("Try again") }
                }
            }
            state.device == null -> {
                KubeCard(Modifier.fillMaxWidth()) {
                    Text("Reading device information…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> {
                val d = state.device
                IdentityCard(d)
                Spacer(Modifier.height(12.dp))
                CpuCard(d)
                Spacer(Modifier.height(12.dp))
                GpuCard(d)
                Spacer(Modifier.height(12.dp))
                MemoryCard(d)
                Spacer(Modifier.height(12.dp))
                DisplayCard(d)
                Spacer(Modifier.height(12.dp))
                BatteryThermalCard(d)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun IdentityCard(d: DeviceCapabilities) {
    KubeCard(Modifier.fillMaxWidth()) {
        SectionTitle("Device")
        InfoRow("Manufacturer", d.identity.manufacturer)
        InfoRow("Brand", d.identity.brand)
        InfoRow("Model", d.identity.model)
        InfoRow("Codename", d.identity.deviceCodename)
        InfoRow("Android", "${d.identity.androidVersion} (API ${d.identity.apiLevel})")
        d.identity.securityPatchLevel?.let { InfoRow("Security patch", it) }
    }
}

@Composable
private fun CpuCard(d: DeviceCapabilities) {
    KubeCard(Modifier.fillMaxWidth()) {
        SectionTitle("CPU")
        InfoRow("Cores", d.cpu.processorCount.toString())
        InfoRow("ABIs", d.cpu.supportedAbis.joinToString(", "))
        InfoRow("64-bit", if (d.cpu.is64Bit) "Yes" else "No")
    }
}

@Composable
private fun GpuCard(d: DeviceCapabilities) {
    KubeCard(Modifier.fillMaxWidth()) {
        SectionTitle("GPU")
        InfoRow("Renderer", d.gpu.renderer ?: "Unknown (not exposed)")
        InfoRow("Vendor", d.gpu.vendor ?: "Unknown (not exposed)")
        val gles = d.gpu.openGlEsMajorVersion?.let { major ->
            val minor = d.gpu.openGlEsMinorVersion ?: 0
            "OpenGL ES $major.$minor"
        } ?: "Unknown"
        InfoRow("OpenGL ES", gles)
        InfoRow(
            "Vulkan",
            when {
                d.gpu.supportsVulkan == true -> {
                    val v = d.gpu.vulkanVersion
                    if (v != null && v > 0) "Yes (API ${v shr 22}.${(v shr 12) and 0x3FF})" else "Yes"
                }
                d.gpu.supportsVulkan == false -> "No"
                else -> "Unknown"
            },
        )
    }
}

@Composable
private fun MemoryCard(d: DeviceCapabilities) {
    KubeCard(Modifier.fillMaxWidth()) {
        SectionTitle("Memory & storage")
        val totalGb = (d.memory.totalRamBytes / RamTier.GB.toDouble() * 10).roundToInt() / 10.0
        val availGb = (d.memory.availableRamBytes / RamTier.GB.toDouble() * 10).roundToInt() / 10.0
        InfoRow("Total RAM", if (d.memory.totalRamBytes > 0) String.format(Locale.US, "%.1f GB", totalGb) else "Unknown")
        InfoRow("Available RAM", String.format(Locale.US, "%.1f GB", availGb))
        InfoRow("Memory pressure", if (d.memory.isLowMemory) "Low memory warning active" else "Normal")
        InfoRow("RAM tier", d.memory.ramTier.name.lowercase().replaceFirstChar { it.uppercase() })
    }
}

@Composable
private fun DisplayCard(d: DeviceCapabilities) {
    KubeCard(Modifier.fillMaxWidth()) {
        SectionTitle("Display")
        InfoRow("Resolution", "${d.display.widthPixels} × ${d.display.heightPixels}")
        InfoRow("Density", "${d.display.densityDpi} dpi")
        InfoRow(
            "Current refresh rate",
            d.display.currentRefreshRateHz.takeIf { it > 0 }?.let { "${it.roundToInt()} Hz" } ?: "Unknown",
        )
        val rates = d.display.supportedRefreshRatesHz
        InfoRow(
            "Supported modes",
            if (rates.isEmpty()) "Not advertised" else rates.joinToString { "${it.roundToInt()} Hz" },
        )
        InfoRow("Variable refresh", if (d.display.supportsVariableRefreshRate) "Yes" else "No")
    }
}

@Composable
private fun BatteryThermalCard(d: DeviceCapabilities) {
    KubeCard(Modifier.fillMaxWidth()) {
        SectionTitle("Battery & thermal")
        InfoRow("Battery", d.battery.percentage?.let { "$it%" } ?: "Unknown")
        InfoRow(
            "Charging",
            when (d.battery.chargingState) {
                BatteryChargingState.CHARGING -> "Charging"
                BatteryChargingState.FULL -> "Full"
                BatteryChargingState.DISCHARGING -> "Discharging"
                BatteryChargingState.UNKNOWN -> "Unknown"
            },
        )
        InfoRow("Battery saver", if (d.battery.isBatterySaverActive) "Active" else "Off")
        InfoRow(
            "Battery health",
            d.battery.healthPercent?.let { "$it%" } ?: "Not exposed by this device",
        )
        InfoRow(
            "Thermal status",
            when (d.thermal.status) {
                ThermalStatus.NONE -> "None (cool)"
                ThermalStatus.LIGHT -> "Light"
                ThermalStatus.MODERATE -> "Moderate"
                ThermalStatus.SEVERE -> "Severe"
                ThermalStatus.CRITICAL -> "Critical"
                ThermalStatus.EMERGENCY -> "Emergency"
                ThermalStatus.SHUTDOWN -> "Shutdown imminent"
                ThermalStatus.UNAVAILABLE -> "Not exposed"
            },
        )
        d.thermal.deviceTemperatureCelsius?.let { temp ->
            InfoRow("Device temperature", String.format(Locale.US, "%.1f °C", temp))
        }
    }
}
