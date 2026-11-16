package com.kuduy.kubooster.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kuduy.kubooster.core.designsystem.InfoRow
import com.kuduy.kubooster.core.designsystem.KubeCard
import com.kuduy.kubooster.core.designsystem.SectionTitle

@Composable
fun SettingsScreen(
    state: SettingsState,
    onSetThemeFollowSystem: (Boolean) -> Unit,
    onClearPreferences: () -> Unit,
    pendingCount: Int,
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )

        SectionTitle("Appearance")
        KubeCard(Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Follow system dark mode", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "KuBooster is designed dark. Turn off to force the light scheme.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.themeFollowSystem,
                    onCheckedChange = onSetThemeFollowSystem,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Data")
        KubeCard(Modifier.fillMaxWidth()) {
            InfoRow(
                "Pending restorable changes",
                if (pendingCount == 0) "None" else "$pendingCount",
            )
            Text(
                "KuBooster keeps a record of the system settings it changed so it can restore them. Clearing preferences does not touch that record.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { showClearDialog = true }) { Text("Clear local preferences") }
            if (state.cleared) {
                Text(
                    "Cleared. Custom profile and flags were reset.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("About")
        KubeCard(Modifier.fillMaxWidth(), onClick = { showPrivacy = true }) {
            Text("Privacy", style = MaterialTheme.typography.titleMedium)
            Text(
                "What leaves your device: nothing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        KubeCard(Modifier.fillMaxWidth(), onClick = { showLicenses = true }) {
            Text("Open-source licenses", style = MaterialTheme.typography.titleMedium)
            Text(
                "Kotlin, Jetpack Compose, AndroidX, DataStore, kotlinx-serialization, Shizuku.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        KubeCard(Modifier.fillMaxWidth()) {
            Text("KuBooster", style = MaterialTheme.typography.titleMedium)
            Text(
                "Version 1.0.0 — KUDUY. Play Smooth. Stay Cool.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear preferences?") },
            text = { Text("Your custom graphics profile and flags are deleted. System settings KuBooster changed stay restorable from the Boost screen.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearPreferences()
                    showClearDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Privacy") },
            text = {
                Text(
                    "KuBooster has no network permissions, no analytics, and no ads. It reads public device state (hardware, display, battery, thermal), detects installed PUBG packages to report version compatibility, and stores your preferences locally on this device. It does not collect PUBG account details or player IDs.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) { Text("Close") }
            },
        )
    }
    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text("Open-source licenses") },
            text = {
                Text(
                    "Kotlin (Apache 2.0)\nJetpack Compose & AndroidX (Apache 2.0)\nkotlinx-serialization (Apache 2.0)\nShizuku API (Apache 2.0)\nInter typeface (OFL)\n\nFull license texts are included in the source repository.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) { Text("Close") }
            },
        )
    }
}
