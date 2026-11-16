package com.kuduy.kubooster.feature.shizuku

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuduy.kubooster.core.designsystem.InfoRow
import com.kuduy.kubooster.core.designsystem.KubeCard
import com.kuduy.kubooster.core.designsystem.SectionTitle
import com.kuduy.kubooster.core.designsystem.StatusChip

/**
 * Shizuku screen: real status, the exact operations it enables, and
 * troubleshooting. Every operation listed is one that exists in code.
 */
@Composable
fun ShizukuScreen(
    state: ShizukuState,
    onRequestPermission: () -> Unit,
    onGrantWriteSecure: () -> Unit,
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
            Text("Shizuku", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            StatusChip(
                when {
                    state.running && state.authorized -> "Active"
                    state.running -> "Needs permission"
                    state.installed -> "Service stopped"
                    else -> "Not installed"
                },
                if (state.running && state.authorized) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            "Shizuku lets apps use system-level settings permissions without root. It is optional — KuBooster's core features work without it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )

        KubeCard(Modifier.fillMaxWidth()) {
            SectionTitle("Status")
            InfoRow("App installed", if (state.installed) "Yes" else "No")
            InfoRow("Service running", if (state.running) "Yes" else "No")
            InfoRow("Permission granted", if (state.authorized) "Yes" else "No")
            InfoRow(
                "WRITE_SECURE_SETTINGS",
                if (state.writeSecureGranted) "Granted" else "Not granted",
            )
        }

        Spacer(Modifier.height(16.dp))
        KubeCard(Modifier.fillMaxWidth()) {
            SectionTitle("What Shizuku enables here")
            Text(
                "One operation: granting KuBooster the WRITE_SECURE_SETTINGS permission so it can manage these Android settings:",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text("• Animation scale (window / transition / animator)", style = MaterialTheme.typography.bodySmall)
            Text("• Battery saver on/off", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Nothing else. No shell commands, no game files, no other apps' data. The command is fixed in the app and shown here before anything runs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))
        if (state.running && !state.authorized) {
            OutlinedButton(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                Text("Request Shizuku permission")
            }
            Spacer(Modifier.height(10.dp))
        }
        if (state.running && state.authorized && !state.writeSecureGranted) {
            OutlinedButton(
                onClick = onGrantWriteSecure,
                enabled = !state.working,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.working) "Running operation…"
                    else "Grant WRITE_SECURE_SETTINGS (pm grant)",
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        state.lastOperationMessage?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        KubeCard(Modifier.fillMaxWidth()) {
            SectionTitle("Troubleshooting")
            Text(
                "• Install Shizuku from its official GitHub releases, then start it via wireless debugging or ADB.\n" +
                    "• If the service shows stopped, open Shizuku and start it again.\n" +
                    "• If permission was denied earlier, use Request Shizuku permission again.\n" +
                    "• KuBooster works fully without Shizuku; these settings are optional extras.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
