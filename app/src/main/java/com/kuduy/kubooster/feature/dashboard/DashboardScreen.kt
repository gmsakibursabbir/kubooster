package com.kuduy.kubooster.feature.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.kuduy.kubooster.core.designsystem.InfoRow
import com.kuduy.kubooster.core.designsystem.KubeCard
import com.kuduy.kubooster.core.designsystem.SectionTitle
import com.kuduy.kubooster.core.designsystem.StatusChip
import com.kuduy.kubooster.domain.model.CompatibilityConfidence
import com.kuduy.kubooster.domain.model.DetectionState
import com.kuduy.kubooster.domain.model.GameVariant
import com.kuduy.kubooster.domain.model.ThermalStatus
import com.kuduy.kubooster.feature.update.UpdateStatus
import com.kuduy.kubooster.feature.update.UpdateUiState

/**
 * Home dashboard. Layout notes:
 *  - One hero card (device + compatibility), one action, then a supporting
 *    list — deliberately not a grid of identical stat tiles.
 *  - Every value is real; unknowns say so with a reason, not a dash.
 */
@Composable
fun DashboardScreen(
    state: DashboardState,
    update: UpdateUiState,
    onBoost: () -> Unit,
    onOpenFps: () -> Unit,
    onOpenGraphics: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenShizuku: () -> Unit,
    onSelectVariant: (String?) -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenRelease: () -> Unit,
    onRefresh: () -> Unit,
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
            Column(Modifier.weight(1f)) {
                Text(
                    text = "KuBooster",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = state.deviceLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.width(20.dp).height(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                TextButton(onClick = onRefresh) { Text("Refresh") }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Game variant selector: user can pin PUBG Mobile Global, Flash, etc.
        val selectable = state.variants.filter {
            it.state == DetectionState.INSTALLED || it.state == DetectionState.VERSION_UNREADABLE
        }
        if (selectable.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = state.selectedVariantName == null,
                    onClick = { onSelectVariant(null) },
                    label = { Text("Auto") },
                )
                selectable.forEach { meta ->
                    FilterChip(
                        selected = state.selectedVariantName == meta.variant.name,
                        onClick = { onSelectVariant(meta.variant.name) },
                        label = {
                            Text(
                                variantShortLabel(meta.variant) + when (meta.state) {
                                    DetectionState.VERSION_UNREADABLE -> " ?"
                                    else -> ""
                                }
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        when {
            state.errorMessage != null -> {
                KubeCard {
                    Text(
                        state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onRefresh) { Text("Try again") }
                }
            }
            state.device == null -> {
                KubeCard {
                    Text(
                        "Reading device information…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> HeroDeviceCard(state)
        }

        UpdateBanner(
            update = update,
            onDownload = onDownloadUpdate,
            onInstall = onInstallUpdate,
            onOpenRelease = onOpenRelease,
        )

        Spacer(Modifier.height(16.dp))

        val canBoost = state.device != null && state.compatibility != null && !state.loading
        Button(
            onClick = onBoost,
            enabled = canBoost,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(
                if (canBoost) "Boost performance" else "Reading device…",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        if (canBoost) {
            Text(
                text = "Applies the settings changes listed on the next screen. Nothing is modified without your confirmation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Status")
        KubeCard(Modifier.fillMaxWidth()) {
            val compat = state.compatibility
            InfoRow("Game", gameLine(state))
            InfoRow("Display", state.refreshRateLabel)
            InfoRow("Recommended FPS", state.recommendedFps?.let {
                "${it.option.label}" + if (it.state != com.kuduy.kubooster.domain.model.FpsProfileState.VERIFIED_AVAILABLE) " (unverified)" else ""
            } ?: "Unknown")
            InfoRow("Compatibility", compat?.compatibilityConfidence?.label() ?: "Checking…")
            InfoRow("Thermal", state.thermalLabel)
            InfoRow("Shizuku", shizukuLine(state))
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("More")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickLink("FPS profiles", "What each FPS option means on this device", onOpenFps)
            QuickLink("Graphics optimizer", "Preset recommendations for your hardware", onOpenGraphics)
            QuickLink("Device diagnostics", "Full hardware and system report", onOpenDiagnostics)
            QuickLink("Shizuku integration", "Optional privileged settings management", onOpenShizuku)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HeroDeviceCard(state: DashboardState) {
    val device = state.device ?: return
    val compat = state.compatibility
    KubeCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${device.display.currentRefreshRateHz.takeIf { it > 0 }?.roundToInt() ?: "–"} Hz",
                    style = MaterialTheme.typography.displayMedium,
                )
                Text(
                    text = "current display refresh rate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (state.gameStatus) {
                GameStatusTile.INSTALLED -> StatusChip("PUBG installed", MaterialTheme.colorScheme.primary)
                GameStatusTile.NOT_INSTALLED -> StatusChip("PUBG not installed", MaterialTheme.colorScheme.onSurfaceVariant)
                GameStatusTile.CHECKING -> StatusChip("Checking…", MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        InfoRow("RAM", state.ramLabel)
        InfoRow("Battery", state.batteryLabel)
        InfoRow("Thermal", state.thermalLabel)
        compat?.summary?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (device.thermal.status in setOf(ThermalStatus.SEVERE, ThermalStatus.CRITICAL, ThermalStatus.EMERGENCY)) {
            Spacer(Modifier.height(10.dp))
            Text(
                "The device is running hot. Let it cool before long gaming sessions and avoid charging while playing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun QuickLink(title: String, subtitle: String, onClick: () -> Unit) {
    KubeCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun gameLine(state: DashboardState): String {
    val meta = state.game ?: return "Checking…"
    return when (meta.state) {
        DetectionState.NOT_INSTALLED -> "Not installed"
        DetectionState.VERSION_UNREADABLE -> "Installed, version unreadable"
        DetectionState.UNABLE_TO_DETECT -> "Unable to detect"
        DetectionState.INSTALLED ->
            "${meta.variant.displayName} ${meta.versionName ?: "unknown version"}"
    }
}

private fun shizukuLine(state: DashboardState): String = when {
    !state.shizukuInstalled -> "Not installed (optional)"
    !state.shizukuRunning -> "Installed, service not running"
    !state.shizukuAuthorized -> "Running, permission not granted"
    else -> "Active"
}

private fun CompatibilityConfidence.label(): String = when (this) {
    CompatibilityConfidence.HIGH -> "High"
    CompatibilityConfidence.MEDIUM -> "Medium"
    CompatibilityConfidence.LOW -> "Low"
    CompatibilityConfidence.NONE -> "None yet"
}

private fun variantShortLabel(variant: GameVariant): String = when (variant) {
    GameVariant.GLOBAL -> "Global"
    GameVariant.FLASH -> "Flash"
    GameVariant.KR -> "KR"
    GameVariant.VN -> "VN"
    GameVariant.BGMI -> "BGMI"
}

/**
 * In-app update banner. Shown only when a newer signed release exists on the
 * official GitHub repository. Downloads come from github.com only.
 */
@Composable
private fun UpdateBanner(
    update: UpdateUiState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenRelease: () -> Unit,
) {
    var dismissed by remember { mutableStateOf(false) }
    if (dismissed) return
    when (update.status) {
        UpdateStatus.AVAILABLE -> {
            val latest = update.latest
            KubeCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Update available${latest?.tagName?.let { ": $it" } ?: ""}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Installed: v${update.currentVersion}" +
                                (latest?.apkSize?.takeIf { it > 0 }
                                    ?.let { " · ${"%.1f".format(it / (1024.0 * 1024.0))} MB" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    TextButton(onClick = { dismissed = true }) { Text("Hide") }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (latest?.apkUrl != null) {
                        Button(onClick = onDownload) { Text("Download update") }
                    }
                    OutlinedButton(onClick = onOpenRelease) { Text("View release") }
                }
                Text(
                    "Checked against the official GitHub repository. Nothing else is contacted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        UpdateStatus.DOWNLOADING -> {
            KubeCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Downloading update from GitHub…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        UpdateStatus.DOWNLOADED -> {
            KubeCard(Modifier.fillMaxWidth()) {
                Text("Update downloaded", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Downloaded from official GitHub. Android will ask you to confirm the installation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                )
                Button(onClick = onInstall) { Text("Install update") }
            }
            Spacer(Modifier.height(16.dp))
        }
        else -> Unit
    }
}
