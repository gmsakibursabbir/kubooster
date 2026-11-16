package com.kuduy.kubooster.feature.optimization

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuduy.kubooster.core.designsystem.InfoRow
import com.kuduy.kubooster.core.designsystem.KubeCard
import com.kuduy.kubooster.core.designsystem.SectionTitle
import com.kuduy.kubooster.core.designsystem.StatusChip
import com.kuduy.kubooster.domain.model.OptimizationCategory
import com.kuduy.kubooster.domain.model.OptimizationProposal
import com.kuduy.kubooster.domain.model.OptimizationResult

/**
 * Optimization flow: review proposals → confirm → apply with progress →
 * verified results → restore. Every step shows what is real.
 */
@Composable
fun OptimizationScreen(
    state: OptimizationUiState,
    onConfirm: () -> Unit,
    onRestore: () -> Unit,
    onToggle: (com.kuduy.kubooster.domain.model.OptimizationOperationId) -> Unit,
    onDone: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            "Boost performance",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
        )

        when (val step = state.step) {
            OptimizationStep.Loading -> {
                KubeCard(Modifier.fillMaxWidth()) {
                    Text("Evaluating recommendations…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            is OptimizationStep.Error -> {
                KubeCard(Modifier.fillMaxWidth()) {
                    Text(step.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onDone) { Text("Back") }
                }
            }
            is OptimizationStep.Review -> {
                Text(
                    "These are the changes KuBooster can make right now. Unchecked items are skipped. Nothing runs until you confirm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                // unslopovich-ignore unvirtualized-list: at most five proposal cards, fixed by design.
                step.proposals.forEach { proposal ->
                    ProposalRow(
                        proposal = proposal,
                        checked = proposal.operationId in state.selected,
                        onToggle = {
                            if (proposal.supported) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggle(proposal.operationId)
                            }
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onConfirm,
                    enabled = state.selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Text("Apply ${state.selected.size} change${if (state.selected.size == 1) "" else "s"}")
                }
                Spacer(Modifier.height(24.dp))
            }
            OptimizationStep.Applying -> {
                KubeCard(Modifier.fillMaxWidth(), ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(12.dp))
                        Text("Applying changes and verifying each one…")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            is OptimizationStep.Result -> {
                LaunchedEffect(Unit) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                SectionTitle("Results")
                step.report.results.forEach { result ->
                    ResultRow(result)
                    Spacer(Modifier.height(10.dp))
                }
                if (step.report.skippedCount > 0) {
                    Text(
                        "${step.report.skippedCount} recommended change(s) were not supported on this device and were skipped.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                    Text("Restore all KuBooster changes")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Done")
                }
                Spacer(Modifier.height(24.dp))
            }
            is OptimizationStep.Restored -> {
                SectionTitle("Restoration")
                step.results.forEach { r ->
                    KubeCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        InfoRow(r.operationId.name, if (r.verified) "Restored & verified" else r.message)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProposalRow(
    proposal: OptimizationProposal,
    checked: Boolean,
    onToggle: () -> Unit,
    /** Checked state is controlled by the parent through [checked]. */
) {
    KubeCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(proposal.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    proposal.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (proposal.supported) {
                Checkbox(checked = checked, onCheckedChange = { onToggle() })
            } else {
                StatusChip("Unavailable", MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        proposal.unsupportedReason?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Text(
            "Reversible" ,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ResultRow(result: OptimizationResult) {
    KubeCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                titleOf(result),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            StatusChip(
                when {
                    result.applied && result.verified -> "Applied & verified"
                    result.applied -> "Applied, unverified"
                    else -> "Failed"
                },
                when {
                    result.applied && result.verified -> MaterialTheme.colorScheme.primary
                    result.applied -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                },
            )
        }
        Text(
            result.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun titleOf(result: OptimizationResult): String = when (result.operationId) {
    com.kuduy.kubooster.domain.model.OptimizationOperationId.SET_PEAK_REFRESH_RATE -> "Peak refresh rate"
    com.kuduy.kubooster.domain.model.OptimizationOperationId.SET_MIN_REFRESH_RATE -> "Minimum refresh rate"
    com.kuduy.kubooster.domain.model.OptimizationOperationId.SET_REDUCED_BRIGHTNESS -> "Screen brightness"
    com.kuduy.kubooster.domain.model.OptimizationOperationId.DISABLE_BATTERY_SAVER -> "Battery saver"
    com.kuduy.kubooster.domain.model.OptimizationOperationId.SET_ANIMATION_SCALES -> "Animation scale"
}
