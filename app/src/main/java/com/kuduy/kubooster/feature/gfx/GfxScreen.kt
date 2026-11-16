package com.kuduy.kubooster.feature.gfx

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kuduy.kubooster.core.designsystem.KubeCard
import com.kuduy.kubooster.core.designsystem.SectionTitle
import com.kuduy.kubooster.data.gfx.GfxConfigEditor
import com.kuduy.kubooster.domain.model.GameVariant

/**
 * GFX mode — the real editor for PUBG's UserCustom.ini. Requires Shizuku
 * (ADB-level file access, no root). Every action is user-confirmed and the
 * original file can always be restored byte-exact.
 */
@Composable
fun GfxScreen(
    state: GfxUiState,
    onRefresh: () -> Unit,
    onSelectTarget: (String) -> Unit,
    onRequestShizuku: () -> Unit,
    onApplyFps: (Int) -> Unit,
    onRestore: () -> Unit,
) {
    var confirmTarget by remember { mutableStateOf<Int?>(null) }

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
            Text(
                text = "GFX mode",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            if (state.busy) {
                CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
            }
        }
        Text(
            text = "Edits PUBG's own config file (UserCustom.ini) to set its frame-rate options. Original file is backed up automatically and restorable any time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

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
                if (state.targets.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        state.targets.forEach { target ->
                            FilterChip(
                                selected = target.variant.packageName == state.selectedPackage,
                                onClick = { onSelectTarget(target.variant.packageName) },
                                label = { Text(variantShortLabel(target.variant)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                state.gameRunning?.let { running ->
                    if (running) {
                        KubeCard(Modifier.fillMaxWidth()) {
                            Text(
                                "PUBG is running. Close it completely (also from Recent apps) before applying — the game rewrites its config on exit.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
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
                    is GfxFileStatus.Loaded -> FpsEditorCard(
                        status,
                        state,
                        onApply = { fps -> confirmTarget = fps },
                        onRestore = onRestore,
                        onRefresh = onRefresh,
                    )
                }
            }
        }

        state.message?.let {
            Spacer(Modifier.height(12.dp))
            KubeCard(Modifier.fillMaxWidth()) {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(28.dp))
    }

    confirmTarget?.let { fps ->
        ApplyConfirmDialog(
            fps = fps,
            onConfirm = {
                confirmTarget = null
                onApplyFps(fps)
            },
            onDismiss = { confirmTarget = null },
        )
    }
}

@Composable
private fun FpsEditorCard(
    status: GfxFileStatus.Loaded,
    state: GfxUiState,
    onApply: (Int) -> Unit,
    onRestore: () -> Unit,
    onRefresh: () -> Unit,
) {
    var selectedFps by remember { mutableStateOf<Int?>(null) }

    KubeCard(Modifier.fillMaxWidth()) {
        SectionTitle("Current values in UserCustom.ini", Modifier)
        Text(
            status.configPath,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(10.dp))
        GfxConfigEditor.MANAGED_FPS_KEYS.forEach { key ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(
                    key.removePrefix("r.PUBGDeviceFPS"),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.4f),
                )
                Text(
                    status.fpsValues[key] ?: "not set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status.fpsValues[key] != null)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionTitle("Set frame rate to", Modifier)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 6.dp),
        ) {
            listOf(30, 60, 90, 120).forEach { fps ->
                FilterChip(
                    selected = selectedFps == fps,
                    onClick = { selectedFps = fps },
                    label = { Text("$fps FPS") },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { selectedFps?.let(onApply) },
            enabled = selectedFps != null && !state.busy,
        ) {
            Text("Apply to PUBG")
        }
        Text(
            "Takes effect after the game is fully closed and reopened. Game updates may regenerate the file — apply again if needed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    if (state.hasBackup) {
        Spacer(Modifier.height(12.dp))
        KubeCard(Modifier.fillMaxWidth()) {
            Text("Original config backed up", style = MaterialTheme.typography.titleMedium)
            Text(
                "The untouched UserCustom.ini is stored in KuBooster's private storage. Restoring writes it back byte-exact.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )
            OutlinedButton(onClick = onRestore, enabled = !state.busy) {
                Text("Restore original config")
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    TextButton(onClick = onRefresh) { Text("Re-read file") }
}

@Composable
private fun ApplyConfirmDialog(
    fps: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var understood by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        KubeCard(Modifier.fillMaxWidth()) {
            Text("Set PUBG frame rate to $fps?", style = MaterialTheme.typography.titleMedium)
            Text(
                "This edits PUBG's UserCustom.ini config file. Modified configs are detectable by the game's security systems and accounts can be banned for it — this is the same category of change every GFX tool makes. KuBooster stores your original file and cannot guarantee any outcome; you use this at your own risk.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = understood, onCheckedChange = { understood = it })
                Text("I understand the risk", style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Button(
                    onClick = onConfirm,
                    enabled = understood,
                    colors = ButtonDefaults.buttonColors(),
                ) { Text("Apply") }
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    }
}

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

private fun variantShortLabel(variant: GameVariant): String = when (variant) {
    GameVariant.GLOBAL -> "Global"
    GameVariant.FLASH -> "Flash"
    GameVariant.KR -> "KR"
    GameVariant.VN -> "VN"
    GameVariant.BGMI -> "BGMI"
}
