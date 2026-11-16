package com.kuduy.kubooster.feature.graphics

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
import androidx.compose.material3.FilterChip
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
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.GraphicsPreset
import com.kuduy.kubooster.domain.model.GraphicsRecommendation
import com.kuduy.kubooster.domain.model.RamTier

/**
 * Graphics screen. Presents the three engine presets with concrete in-game
 * settings the user applies inside PUBG themselves, plus the stored custom
 * profile selection. KuBooster writes nothing into the game.
 */
@Composable
fun GraphicsScreen(
    state: GraphicsScreenState,
    onSelectPreset: (GraphicsPreset) -> Unit,
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
                text = "Graphics optimizer",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            if (state.loading) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
        }

        Text(
            text = "These are recommendations for the settings inside PUBG Mobile. KuBooster never changes game files or game settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )

        when {
            state.error != null -> {
                KubeCard(Modifier.fillMaxWidth()) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { onSelectPreset(state.selectedPreset) }) { Text("Retry") }
                }
            }
            else -> {
                // Preset selector chips — the stored preference.
                SectionTitle("Your preset")
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    listOf(
                        GraphicsPreset.SMOOTH_PERFORMANCE to "Smooth",
                        GraphicsPreset.BALANCED to "Balanced",
                        GraphicsPreset.HIGH_QUALITY to "Quality",
                    ).forEach { (preset, label) ->
                        FilterChip(
                            selected = state.selectedPreset == preset,
                            onClick = { onSelectPreset(preset) },
                            label = { Text(label) },
                        )
                    }
                }
                if (state.saved) {
                    Text(
                        "Saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))
                // unslopovich-ignore unvirtualized-list: three preset cards, fixed count.
                state.recommendations.forEach { rec ->
                    RecommendationCard(rec, state.device)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RecommendationCard(
    rec: GraphicsRecommendation,
    device: DeviceCapabilities?,
) {
    val highlighted = device != null && rec.preset == when (device.memory.ramTier) {
        RamTier.ENTRY, RamTier.MID -> GraphicsPreset.SMOOTH_PERFORMANCE
        else -> GraphicsPreset.BALANCED
    }
    KubeCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(rec.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (highlighted) {
                Text(
                    "suggested for this device",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            rec.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(10.dp))
        rec.settings.forEach { advice ->
            InfoRow(advice.setting, advice.value)
        }
        if (rec.rationale.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Why",
                style = MaterialTheme.typography.titleSmall,
            )
            rec.rationale.forEach { reason ->
                Text(
                    "• $reason",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
