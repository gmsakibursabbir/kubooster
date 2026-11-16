package com.kuduy.kubooster.feature.fps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.kuduy.kubooster.core.designsystem.StatusChip
import com.kuduy.kubooster.domain.model.FpsProfileState
import com.kuduy.kubooster.domain.model.VerificationStatus
import kotlin.math.roundToInt

/**
 * FPS screen. The distinction this screen exists to make:
 * display Hz, display capability, verified game option, measured FPS — four
 * different facts. The UI names each one and never blurs them.
 */
@Composable
fun FpsScreen(
    state: FpsScreenState,
    onRetry: () -> Unit,
    onOpenGfx: () -> Unit = {},
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
            Text(
                text = "FPS profiles",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            if (state.loading) {
                CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // unslopovich-ignore unvirtualized-list: five fixed cards, not a data list.
        // Display facts card — read-only, from DisplayManager.
        KubeCard(Modifier.fillMaxWidth()) {
            SectionTitle("Your display", Modifier)
            Text(
                text = if (state.currentRefreshRateHz != null)
                    "Running at ${state.currentRefreshRateHz!!.roundToInt()} Hz right now"
                else "Refresh rate unknown",
                style = MaterialTheme.typography.titleMedium,
            )
            val rates = state.supportedRefreshRatesHz
            Text(
                text = if (rates.isEmpty())
                    "This display does not advertise its supported refresh rates."
                else "Supported modes: ${rates.joinToString { "${it.roundToInt()} Hz" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "A 120 Hz display does not mean the game renders at 120 FPS. KuBooster never reports the display rate as game FPS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Options")

        when {
            state.error != null -> {
                KubeCard(Modifier.fillMaxWidth()) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onRetry) { Text("Try again") }
                }
            }
            else -> {
                state.profiles.forEach { profile ->
                    val (chipText, chipColor) = when (profile.state) {
                        FpsProfileState.VERIFIED_AVAILABLE -> "Verified" to MaterialTheme.colorScheme.primary
                        FpsProfileState.DISPLAY_COMPATIBLE_GAME_UNVERIFIED -> "Unverified" to MaterialTheme.colorScheme.secondary
                        FpsProfileState.NOT_SUPPORTED -> "Not supported" to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    KubeCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                profile.option.label,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f),
                            )
                            StatusChip(chipText, chipColor)
                        }
                        Text(
                            profile.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        // Manifest status — the honest source of truth for game-side claims.
        Spacer(Modifier.height(12.dp))
        KubeCard(Modifier.fillMaxWidth()) {
            SectionTitle("Compatibility manifest", Modifier)
            InfoRow(
                "Game-side verification",
                when (state.manifestStatus) {
                    VerificationStatus.VERIFIED -> "Verified"
                    VerificationStatus.UNVERIFIED -> "Not verified for this game version"
                    VerificationStatus.UNKNOWN -> "No manifest entry"
                },
            )
            state.manifestTargetVersion?.let {
                InfoRow("Manifest target", "PUBG Mobile v$it")
            }
            state.verifiedAt?.let {
                InfoRow("Verified at", it)
            }
            if (!state.gameInstalled) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "PUBG Mobile is not installed, so game-side options cannot be evaluated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Action path: the FPS screen reports facts; GFX mode changes them.
        Spacer(Modifier.height(12.dp))
        KubeCard(Modifier.fillMaxWidth()) {
            SectionTitle("Want to force these options?", Modifier)
            Text(
                "This screen only reports what your display and the game officially support. GFX mode can set the game's frame-rate options directly — the same edit every GFX tool makes. Requires the free Shizuku app (no root). Modified configs are detectable and accounts can be banned; the original file is backed up and restorable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            Button(onClick = onOpenGfx) { Text("Open GFX mode") }
        }

        Spacer(Modifier.height(24.dp))
    }
}
