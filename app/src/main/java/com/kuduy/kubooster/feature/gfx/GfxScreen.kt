package com.kuduy.kubooster.feature.gfx

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kuduy.kubooster.core.designsystem.InfoRow
import com.kuduy.kubooster.core.designsystem.KubeCard
import com.kuduy.kubooster.data.gfx.ActiveSavEditor
import com.kuduy.kubooster.data.gfx.GfxConfigEditor
import com.kuduy.kubooster.domain.model.GameVariant
import kotlinx.coroutines.delay

/**
 * GFX mode — the real editor for PUBG's UserCustom.ini, styled after the
 * product design mockups: two-tone title, theme toggle, hero card, plain
 * section rows, and a full-width Apply & Run Game action. Requires Shizuku
 * (ADB-level file access, no root). Every apply is preceded by an explicit
 * backup of the default config; restoring/removing is one tap.
 */
@Composable
fun GfxScreen(
    state: GfxUiState,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onSelectTarget: (String) -> Unit,
    onSelectFps: (Int) -> Unit,
    onSelectPreset: (String?) -> Unit,
    onSelectResolution: (String) -> Unit,
    onRequestShizuku: () -> Unit,
    onApply: () -> Unit,
    onRemoveGfx: () -> Unit,
    onDismissOverlay: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    // Auto-dismiss the success overlay; "Launching game…" is informational.
    LaunchedEffect(state.applyPhase) {
        if (state.applyPhase == GfxApplyPhase.Success) {
            delay(2500)
            onDismissOverlay()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // ---- Header -------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "kuBooster",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = " GFX",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.weight(1f))
                ShizukuPill(state)
                Spacer(Modifier.width(8.dp))
                ThemeToggleButton(isDark, onToggleTheme)
            }

            // ---- Hero ---------------------------------------------------
            KubeCard(Modifier.fillMaxWidth()) {
                Text(
                    text = "PRO OPTIMIZER",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Unlock Max FPS &\nUltra Graphics",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = "Configured for ultimate PUBG Mobile performance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Spacer(Modifier.height(18.dp))

            when {
                !state.shizukuInstalled -> ShizukuMissingCard()
                !state.shizukuReady -> ShizukuPermissionCard(onRequestShizuku, onRefresh)
                state.targets.isEmpty() -> KubeCard(Modifier.fillMaxWidth()) {
                    Text(
                        "No PUBG Mobile variant is installed. Install the game and open this screen again.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> {
                    state.gameRunning?.let { running ->
                        if (running) {
                            KubeCard(Modifier.fillMaxWidth()) {
                                Text(
                                    "PUBG is running. Close it completely (also from Recent apps) before applying — the game rewrites its config on exit.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    when (val status = state.status) {
                        is GfxFileStatus.Loading, GfxFileStatus.Idle -> {
                            KubeCard(Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.height(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Reading UserCustom.ini…", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        is GfxFileStatus.Unavailable -> {
                            KubeCard(Modifier.fillMaxWidth()) {
                                Text("Can't access the config", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    status.reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                OutlinedButton(onClick = onRefresh, modifier = Modifier.padding(top = 10.dp)) {
                                    Text("Retry")
                                }
                            }
                        }
                        is GfxFileStatus.Missing -> {
                            KubeCard(Modifier.fillMaxWidth()) {
                                Text("Config file not found", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    status.reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                Text(
                                    status.configPath,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                                status.dirListing?.let {
                                    Text(
                                        "Folder contains: ${if (it.isBlank()) "(empty)" else it}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                                OutlinedButton(onClick = onRefresh, modifier = Modifier.padding(top = 10.dp)) {
                                    Text("Retry")
                                }
                            }
                        }
                        is GfxFileStatus.Loaded -> {
                            val selectedVariant = state.targets
                                .firstOrNull { it.variant.packageName == state.selectedPackage }?.variant

                            // ---- Option sections (plain rows, per mockup) --
                            SectionLabel("Game Version")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                state.targets.forEach { target ->
                                    ChoiceChip(
                                        label = variantShortLabel(target.variant),
                                        selected = target.variant.packageName == state.selectedPackage,
                                        onClick = { onSelectTarget(target.variant.packageName) },
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            SectionLabel("Resolution")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                GfxConfigEditor.RESOLUTION_KEYS.forEach { key ->
                                    ChoiceChip(
                                        label = resolutionLabel(key),
                                        selected = state.resolutionKey == key,
                                        onClick = { onSelectResolution(key) },
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            SectionLabel("Graphics")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                ChoiceChip("Default", state.presetKey == null, { onSelectPreset(null) })
                                if (selectedVariant == GameVariant.FLASH) {
                                    ChoiceChip("Super Smooth", state.presetKey == "supersmooth", { onSelectPreset("supersmooth") })
                                }
                                ChoiceChip("Smooth", state.presetKey == "smooth", { onSelectPreset("smooth") })
                                ChoiceChip("Balanced", state.presetKey == "balanced", { onSelectPreset("balanced") })
                                ChoiceChip("HD", state.presetKey == "hd", { onSelectPreset("hd") })
                                ChoiceChip("Ultra HD", state.presetKey == "ultrahdr", { onSelectPreset("ultrahdr") })
                            }
                            Text(
                                text = presetDescription(state.presetKey),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp),
                            )

                            Spacer(Modifier.height(16.dp))
                            SectionLabel("FPS Limit")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                listOf(30, 60, 90, 120).forEach { fps ->
                                    ChoiceChip(
                                        label = fpsGameLabel(fps),
                                        selected = state.fpsTarget == fps,
                                        onClick = { onSelectFps(fps) },
                                    )
                                }
                            }
                            Text(
                                "PUBG names: 60 = Extreme · 90 = Extreme+ · 120 = Ultra Extreme. KuBooster sets it directly in the game's save (Active.sav). Don't change Frame Rate in the game afterwards — that overwrites it. Real FPS can't exceed your screen's refresh rate.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp),
                            )

                            Spacer(Modifier.height(22.dp))

                            // ---- Apply & Run Game ----------------------
                            Button(
                                onClick = { showConfirm = true },
                                enabled = state.fpsTarget != null && !state.busy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Icon(Icons.Filled.Bolt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Apply & Run Game",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                            Text(
                                "Your default config is backed up automatically before the first write — remove GFX any time to go back.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            )

                            // ---- Current values -------------------------
                            Spacer(Modifier.height(18.dp))
                            CurrentConfigCard(status)

                            // ---- Remove / restore -----------------------
                            Spacer(Modifier.height(14.dp))
                            RemoveGfxCard(state, onRemoveGfx)
                        }
                    }
                }
            }

            state.message?.let {
                Spacer(Modifier.height(14.dp))
                KubeCard(Modifier.fillMaxWidth()) {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(28.dp))
        }

        // ---- Full-screen apply overlays (per mockup) -------------------
        when (state.applyPhase) {
            GfxApplyPhase.Optimizing -> ApplyOverlay {
                CircularProgressIndicator(
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "Optimizing…",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Injecting kuBooster configuration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GfxApplyPhase.Success -> ApplyOverlay {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "Success!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Launching game…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Default config backed up — remove GFX any time to restore it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
            else -> Unit
        }
    }

    if (showConfirm) {
        ApplyConfirmDialog(
            fps = state.fpsTarget,
            presetLabel = presetLabelOf(state.presetKey),
            resolutionLabel = resolutionLabel(state.resolutionKey),
            hasBackup = state.hasBackup,
            onConfirm = {
                showConfirm = false
                onApply()
            },
            onDismiss = { showConfirm = false },
        )
    }
}

// ---- Overlays ------------------------------------------------------------

@Composable
private fun ApplyOverlay(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            content()
        }
    }
}

// ---- Header helpers --------------------------------------------------------

@Composable
private fun ThemeToggleButton(isDark: Boolean, onToggleTheme: () -> Unit) {
    val icon: ImageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
            .clickable(onClick = onToggleTheme),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ShizukuPill(state: GfxUiState) {
    val (label, color) = when {
        state.shizukuReady -> "Connected" to MaterialTheme.colorScheme.primary
        state.shizukuInstalled -> "Needs grant" to MaterialTheme.colorScheme.secondary
        else -> "No Shizuku" to MaterialTheme.colorScheme.error
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

// ---- Current values ---------------------------------------------------------

@Composable
private fun CurrentConfigCard(status: GfxFileStatus.Loaded) {
    // Active.sav holds the frame rate the game actually uses; prefer it.
    val fpsSummary = run {
        val levels = status.saveFpsLevels?.values?.distinct().orEmpty()
        val values = GfxConfigEditor.MANAGED_FPS_KEYS.mapNotNull { status.fpsValues[it] }
        when {
            levels.size == 1 -> ActiveSavEditor.levelLabel(levels.first())
            levels.size > 1 -> "Mixed"
            values.isEmpty() -> "Game default"
            values.distinct().size == 1 -> "${values.first()} FPS"
            else -> "Mixed"
        }
    }
    KubeCard(Modifier.fillMaxWidth()) {
        Text("Current config", style = MaterialTheme.typography.titleMedium)
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        ) {
            Text(
                text = fpsSummary,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = if (fpsSummary == "Game default")
                    MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            status.configPath,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )
        InfoRow(
            label = "Active.sav frame rate",
            value = when {
                status.saveFpsLevels == null -> "file not found"
                status.saveFpsLevels.isEmpty() -> "not set yet"
                else -> status.saveFpsLevels.entries.joinToString { "${it.key}=${it.value}" }
            },
            valueColor = if (!status.saveFpsLevels.isNullOrEmpty())
                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GfxConfigEditor.MANAGED_FPS_KEYS.forEach { key ->
            InfoRow(
                label = key.removePrefix("r.PUBGDeviceFPS"),
                value = status.fpsValues[key] ?: "not set",
                valueColor = if (status.fpsValues[key] != null)
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        InfoRow(
            label = "Graphics quality",
            value = graphicsSummary(status.cvars),
            valueColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Human summary of the graphics keys actually present in the file. */
private fun graphicsSummary(cvars: Map<String, String>): String {
    val quality = cvars["r.UserQualitySetting"]
    val hdr = cvars["r.UserHDRSetting"]
    val parts = buildList {
        add(
            when (quality) {
                "0" -> "Smooth"
                "1" -> "Balanced"
                "2" -> "HD"
                "3" -> "Ultra"
                null -> "not set"
                else -> "Level $quality"
            },
        )
        when (hdr) {
            "1" -> add("HDR 1")
            "2" -> add("HDR 2")
            "3" -> add("HDR 3")
        }
    }
    return if (parts.all { it == "not set" }) "Game default" else parts.joinToString(" · ")
}

// ---- Remove / restore -------------------------------------------------------

@Composable
private fun RemoveGfxCard(state: GfxUiState, onRemoveGfx: () -> Unit) {
    if (!state.hasBackup) return
    KubeCard(Modifier.fillMaxWidth()) {
        Text("Reset to default", style = MaterialTheme.typography.titleMedium)
        Text(
            "Removes every GFX change: the backed-up default UserCustom.ini is written back byte-exact and the game returns to its original settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
        Button(
            onClick = onRemoveGfx,
            enabled = !state.busy,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text("Remove GFX settings")
        }
    }
}

// ---- Confirm dialog ----------------------------------------------------------

@Composable
private fun ApplyConfirmDialog(
    fps: Int?,
    presetLabel: String,
    resolutionLabel: String,
    hasBackup: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var understood by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        KubeCard(Modifier.fillMaxWidth()) {
            Text("Apply to PUBG?", style = MaterialTheme.typography.titleMedium)
            InfoRow("Frame rate", fps?.let { "$it FPS" } ?: "—", modifier = Modifier.padding(top = 8.dp))
            InfoRow("Graphics", presetLabel)
            InfoRow("Resolution", resolutionLabel)
            InfoRow(
                "Backup",
                if (hasBackup)
                    "Default config already backed up — restorable any time"
                else
                    "Default config will be backed up before writing",
                valueColor = MaterialTheme.colorScheme.primary,
            )
            Text(
                "This edits PUBG's UserCustom.ini config file. Modified configs are detectable by the game's security systems and accounts can be banned for it — this is the same category of change every GFX tool makes. KuBooster stores your original file and cannot guarantee any outcome; you use this at your own risk.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { understood = !understood },
            ) {
                Checkbox(
                    checked = understood,
                    onCheckedChange = { understood = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                )
                Text("I understand the risk", style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                Button(onClick = onConfirm, enabled = understood) { Text("Apply") }
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    }
}

// ---- Setup cards -------------------------------------------------------------

@Composable
private fun ShizukuMissingCard() {
    KubeCard(Modifier.fillMaxWidth()) {
        Text("Shizuku is required", style = MaterialTheme.typography.titleMedium)
        Text(
            "GFX mode needs ADB-level file access to edit UserCustom.ini. The free Shizuku app provides it without root: install Shizuku, start it (wireless debugging works on Android 11+, no PC needed), and grant KuBooster access.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ShizukuPermissionCard(
    onRequest: () -> Unit,
    onRefresh: () -> Unit,
) {
    KubeCard(Modifier.fillMaxWidth()) {
        Text("Grant KuBooster access", style = MaterialTheme.typography.titleMedium)
        Text(
            "Shizuku is running. Grant KuBooster permission to use it — one tap, no root.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onRequest) { Text("Grant") }
            OutlinedButton(onClick = onRefresh) { Text("Retry") }
        }
    }
}

// ---- Choice chip (per mockup: tinted selected, flat unselected) --------------

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---- Small pieces ------------------------------------------------------------

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

private fun resolutionLabel(key: String): String = when (key) {
    "default" -> "Default"
    "720p" -> "720p"
    "1080p" -> "1080p"
    else -> key
}

/** Chip label: the number plus the game's own frame-rate option name. */
private fun fpsGameLabel(fps: Int): String = when (fps) {
    60 -> "60 Extreme"
    90 -> "90 Extreme+"
    120 -> "120 Ultra Extreme"
    else -> "$fps"
}

private fun presetDescription(key: String?): String = when (key) {
    null -> "Leave graphics exactly as the game set them — only the frame rate changes."
    "supersmooth" -> "Flash exclusive — lightest tuning, everything optional off. For weak devices and highest FPS."
    "smooth" -> "Fewest shadows and effects — least lag, best for weak devices and high FPS."
    "balanced" -> "Middle ground between visuals and performance."
    "hd" -> "Sharper visuals, more device load."
    "ultrahdr" -> "Maximum quality with HDR lighting — needs a strong device."
    else -> ""
}

private fun presetLabelOf(key: String?): String = when (key) {
    null -> "Game's own"
    "supersmooth" -> "Super Smooth"
    "smooth" -> "Smooth"
    "balanced" -> "Balanced"
    "hd" -> "HD"
    "ultrahdr" -> "Ultra HD"
    else -> key
}

private fun variantShortLabel(variant: GameVariant): String = when (variant) {
    GameVariant.GLOBAL -> "Global"
    GameVariant.FLASH -> "Flash"
    GameVariant.KR -> "KR"
    GameVariant.VN -> "VN"
    GameVariant.BGMI -> "BGMI"
}
