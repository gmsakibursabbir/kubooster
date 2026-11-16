package com.kuduy.kubooster.feature.onboarding

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuduy.kubooster.core.designsystem.KubeCard
import com.kuduy.kubooster.core.designsystem.StatusChip
import com.kuduy.kubooster.ui.Routes

/**
 * Four onboarding pages + a detection step. No permissions are requested here;
 * the only optional permission (system settings write) is requested later,
 * contextually, with an explanation.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    val detectionRunning by viewModel.detectionRunning.collectAsState()
    val detectionDone by viewModel.detectionDone.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        StatusChip(
            text = "Step ${page + 1} of 5",
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))

        when (page) {
            0 -> OnboardingPage(
                title = "KuBooster",
                body = "A performance companion for gaming on Android. It reads your device's real capabilities, checks PUBG Mobile compatibility, and applies a small set of reversible system settings.",
            )
            1 -> OnboardingPage(
                title = "What it actually does",
                body = "KuBooster detects your hardware, display refresh-rate modes, memory, thermal and battery state, and recommends graphics presets and FPS targets that fit your device. It never modifies PUBG files, game settings, or anti-cheat systems, and it never reports a change it did not verify.",
            )
            2 -> OnboardingPage(
                title = "Shizuku is optional",
                body = "A few useful changes (animation speed, battery saver) need a system-level settings permission. You can grant that through Shizuku, or skip it — every core feature works without it. KuBooster lists exactly which operation it performs before asking.",
            )
            3 -> OnboardingPage(
                title = "Honest limits",
                body = "A 120 Hz screen does not mean 120 FPS gameplay — the game renders FPS on its own. FPS options marked Unverified are plausible for your display but not yet documented for your game version. KuBooster shows Unknown rather than guessing.",
            )
            4 -> {
                OnboardingPage(
                    title = "Detecting your device",
                    body = "Reading hardware, display modes, memory, thermal and battery state, and checking installed PUBG variants. Everything stays on this device.",
                )
                Spacer(Modifier.height(16.dp))
                KubeCard(Modifier.fillMaxWidth()) {
                    Text(
                        when {
                            detectionRunning -> "Running detection…"
                            detectionDone -> "Done. Your device report is ready."
                            else -> "Ready to run."
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                // Kick off the real detection engines when this page shows.
                LaunchedEffect(Unit) { viewModel.runDeviceDetection() }
            }
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                if (page < 4) page += 1
                else viewModel.completeOnboarding(onFinished)
            },
            enabled = page != 4 || detectionDone || !detectionRunning,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text(
                when {
                    page < 4 -> "Continue"
                    detectionDone -> "Start using KuBooster"
                    else -> "Detecting…"
                },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun OnboardingPage(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.displayMedium)
    Spacer(Modifier.height(12.dp))
    Text(
        body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
