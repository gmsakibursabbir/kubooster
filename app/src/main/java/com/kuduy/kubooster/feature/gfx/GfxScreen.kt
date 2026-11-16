package com.kuduy.kubooster.feature.gfx

import android.content.Intent
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kuduy.kubooster.core.designsystem.InfoRow
import com.kuduy.kubooster.core.designsystem.KubeCard
import com.kuduy.kubooster.data.gfx.ActiveSavEditor
import com.kuduy.kubooster.data.gfx.GfxConfigEditor
import com.kuduy.kubooster.data.gfx.GfxLog
import com.kuduy.kubooster.data.gfx.RefreshRatePicker
import com.kuduy.kubooster.domain.model.GameVariant
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * GFX mode — the app's only screen. Layout follows the product mockup:
 * header, Device Ready status, one card per setting (game version,
 * resolution, graphics, frame rate, anti-aliasing), what the game uses now,
 * and a sticky "Swipe to Apply & Boost" control. Requires Shizuku.
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
    onSetAntiAliasing: (Boolean) -> Unit,
    onRequestShizuku: () -> Unit,
    onApply: () -> Unit,
    onRemoveGfx: () -> Unit,
    onDiscardBackup: () -> Unit,
    onDismissOverlay: () -> Unit,
    /** Opens the selected game after a successful apply (activity-context launch). */
    onLaunchGame: () -> Unit,
    /** Shown under the header, e.g. the in-app update banner. */
    topContent: @Composable () -> Unit = {},
) {
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.applyPhase) {
        if (state.applyPhase == GfxApplyPhase.Success) {
            // Let the Success overlay register, then open the game from the
            // Activity context (app-context starts are blocked on some skins).
            delay(600)
            onLaunchGame()
            delay(1900)
            onDismissOverlay()
        }
    }

    val loaded = state.status as? GfxFileStatus.Loaded

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Header(isDark, onToggleTheme)
            topContent()
            HeroCard(state, loaded)

            when {
                !state.shizukuInstalled -> ShizukuMissingCard()
                !state.shizukuReady -> ShizukuPermissionCard(onRequestShizuku, onRefresh)
                state.targets.isEmpty() -> KubeCard(Modifier.fillMaxWidth()) {
                    Text(
                        "No PUBG Mobile version is installed. Install the game and reopen KuBooster.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> {
                    if (state.gameRunning == true) {
                        NoticeCard("PUBG is running — KuBooster closes it automatically when you apply, then reopens it.")
                    }

                    SettingCard(Icons.Filled.SportsEsports, "Game Version") {
                        Segmented(
                            options = state.targets.map { it.variant.packageName to variantShortLabel(it.variant) },
                            selected = state.selectedPackage,
                            onSelect = onSelectTarget,
                        )
                        // Proper game name + installed version for the pick.
                        state.targets
                            .firstOrNull { it.variant.packageName == state.selectedPackage }
                            ?.let { target ->
                                Text(
                                    buildString {
                                        append(target.variant.displayName)
                                        target.versionName?.let { append("  ·  v$it") }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                    }

                    when (val status = state.status) {
                        is GfxFileStatus.Loading, GfxFileStatus.Idle -> KubeCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("Reading game settings…", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        is GfxFileStatus.Unavailable -> ProblemCard("Can't access the game files", status.reason, onRefresh)
                        is GfxFileStatus.Missing -> ProblemCard(
                            "Game settings not found",
                            "Open the game once, go to its graphics settings, close it fully, then tap Retry.",
                            onRefresh,
                        )
                        is GfxFileStatus.Loaded -> {
                            ResolutionCard(state.resolutionKey, onSelectResolution)

                            SettingCard(Icons.Filled.HighQuality, "Graphics Quality") {
                                TileGrid(
                                    options = PRESETS.map { it to presetLabelOf(it) },
                                    selected = state.presetKey,
                                    onSelect = onSelectPreset,
                                )
                            }

                            SettingCard(Icons.Filled.Timer, "FPS Limit") {
                                Segmented(
                                    options = listOf(30, 60, 90, 120).map { it to "$it" },
                                    selected = state.fpsTarget,
                                    onSelect = onSelectFps,
                                )
                                val fps = state.fpsTarget
                                val isFlash = state.selectedPackage == GameVariant.FLASH.packageName
                                when {
                                    fps != null && fps > 60 && isFlash -> Warning(
                                        "PUBG Flash limits the frame rate itself on many phones. If the in-game FPS meter stays at 60, use 60 — forcing more only adds lag.",
                                    )
                                    fps != null && fps > 60 && state.panelHz.isNotEmpty() &&
                                        !RefreshRatePicker.isSmooth(fps, state.panelHz) -> Warning(
                                        "Your screen runs at ${state.panelHz.joinToString("/")} Hz, so $fps FPS can't be shown evenly and may stutter. ${state.panelHz.max()} or 60 is smoother.",
                                    )
                                    else -> Hint("For 90 or 120, pick Super Smooth or Smooth — higher graphics make the game lower the frame rate.")
                                }
                            }

                            // Restore first: it must never be buried below the fold.
                            BackupCard(state, onRemoveGfx, onDiscardBackup)

                            KubeCard(Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CardIcon(Icons.Filled.BlurOn)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Anti-Aliasing", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Smoothens jagged edges · costs FPS",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Switch(
                                        checked = state.antiAliasing,
                                        onCheckedChange = onSetAntiAliasing,
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            state.message?.let { NoticeCard(it) }
            GfxLogCard()

            // Room for the sticky swipe bar (bar + its padding + nav inset).
            Spacer(Modifier.height(150.dp))
        }

        // ---- Sticky swipe-to-apply ------------------------------------
        if (loaded != null && state.shizukuReady) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                SwipeToApply(
                    enabled = state.fpsTarget != null && !state.busy,
                    onSwiped = { showConfirm = true },
                )
            }
        }

        // ---- Apply overlays --------------------------------------------
        when (state.applyPhase) {
            GfxApplyPhase.Optimizing -> ApplyOverlay {
                CircularProgressIndicator(
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(18.dp))
                Text("Boosting…", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            }
            GfxApplyPhase.Success -> ApplyOverlay {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.height(18.dp))
                Text("Boosted!", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Text(
                    "Launching game…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            onConfirm = {
                showConfirm = false
                onApply()
            },
            onDismiss = { showConfirm = false },
        )
    }
}

private val PRESETS = listOf("supersmooth", "smooth", "balanced", "hd", "hdr", "ultrahdr")

// ---- Header & status ---------------------------------------------------------

@Composable
private fun Header(isDark: Boolean, onToggleTheme: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("kuBooster", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleTheme),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Hero: device + Shizuku status, and — once the game's save is read — what
 * the game will actually run, as two large stats.
 */
@Composable
private fun HeroCard(state: GfxUiState, loaded: GfxFileStatus.Loaded?) {
    val ready = state.shizukuReady
    val accent = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val device = remember {
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else Build.HARDWARE
        "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} · $soc"
    }
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.04f)),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.35f), shape)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Memory, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        ready -> "Device Ready"
                        state.shizukuInstalled -> "Shizuku permission needed"
                        else -> "Shizuku not installed"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(device, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        if (loaded != null) {
            val fpsLevel = loaded.saveFpsLevels?.get("BattleFPS") ?: loaded.saveFpsLevels?.values?.firstOrNull()
            Spacer(Modifier.height(18.dp))
            Text(
                "IN GAME NOW",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat(
                    value = fpsLevel?.let { levelFps(it) } ?: "—",
                    unit = if (fpsLevel != null && levelFps(fpsLevel) != "—") "FPS" else "",
                    caption = fpsLevel?.let(ActiveSavEditor::levelLabel)?.substringBefore(" (") ?: "Open the game once",
                    modifier = Modifier.weight(1f),
                )
                Stat(
                    value = loaded.saveQuality?.let(ActiveSavEditor::qualityLabel) ?: "—",
                    unit = "",
                    caption = "Graphics",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Stat(value: String, unit: String, caption: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
        }
        Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Game frame-rate level -> the number it stands for. */
private fun levelFps(level: Int): String = when (level) {
    2 -> "20"
    3 -> "25"
    4 -> "30"
    5 -> "40"
    6 -> "60"
    7 -> "90"
    8 -> "120"
    else -> "—"
}

// ---- Setting cards -----------------------------------------------------------

@Composable
private fun SettingCard(
    icon: ImageVector,
    title: String,
    trailing: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    KubeCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardIcon(icon)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            trailing()
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun CardIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
}

@Composable
private fun ResolutionCard(selectedKey: String, onSelect: (String) -> Unit) {
    val keys = GfxConfigEditor.RESOLUTION_KEYS
    SettingCard(
        icon = Icons.Filled.AspectRatio,
        title = "Resolution",
        trailing = {
            Text(
                resolutionLabel(selectedKey),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        },
    ) {
        Segmented(
            keys.map { it to resolutionLabel(it) },
            selected = selectedKey,
            onSelect = onSelect,
        )
    }
}

/**
 * Equal-width segmented control on a recessed track; the selected segment
 * is a solid accent pill (game version, frame rate).
 */
@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            SelectableTile(label, value == selected, Modifier.weight(1f), RoundedCornerShape(12.dp)) { onSelect(value) }
        }
    }
}

/** Even 3-column grid of tiles (graphics quality). */
@Composable
private fun <T> TileGrid(options: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    SelectableTile(
                        label,
                        value == selected,
                        Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (value == selected) androidx.compose.ui.graphics.Color.Transparent
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(14.dp),
                            ),
                        RoundedCornerShape(14.dp),
                    ) { onSelect(value) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SelectableTile(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    val bg by androidx.compose.animation.animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
        label = "tile",
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun Warning(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun NoticeCard(text: String) {
    KubeCard(Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ProblemCard(title: String, body: String, onRetry: () -> Unit) {
    KubeCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 10.dp)) { Text("Retry") }
    }
}

// ---- Swipe to apply ----------------------------------------------------------

/**
 * Drag the round handle to the end to apply — a deliberate gesture so a
 * stray tap never edits game files. Springs back if released early.
 */
@Composable
private fun SwipeToApply(enabled: Boolean, onSwiped: () -> Unit) {
    val handle = 56.dp
    val shape = RoundedCornerShape(50)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        val maxPx = with(LocalDensity.current) { (maxWidth - handle - 8.dp).toPx() }
        var drag by remember { mutableFloatStateOf(0f) }
        var dragging by remember { mutableStateOf(false) }
        val shown by animateFloatAsState(if (dragging) drag else 0f, label = "swipe")
        Row(
            Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (enabled) "Swipe to Apply & Boost" else "Pick a frame rate first",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f),
            )
            if (enabled) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Box(
            Modifier
                .padding(4.dp)
                .offset { IntOffset((if (dragging) drag else shown).roundToInt(), 0) }
                .size(handle)
                .clip(CircleShape)
                .background(
                    if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                .draggable(
                    enabled = enabled,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta -> drag = (drag + delta).coerceIn(0f, maxPx) },
                    onDragStarted = { dragging = true },
                    onDragStopped = {
                        if (drag >= maxPx * 0.85f) onSwiped()
                        dragging = false
                        drag = 0f
                    },
                )
                // Accessibility / tap fallback: a tap also opens the confirm step.
                .clickable(enabled = enabled, onClickLabel = "Apply and boost", onClick = onSwiped),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.KeyboardDoubleArrowRight,
                contentDescription = "Swipe to apply",
                tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- What the game uses now + restore ------------------------------------------

/** Restore + Discard, shown only while a backup exists. */
@Composable
private fun BackupCard(state: GfxUiState, onRemoveGfx: () -> Unit, onDiscardBackup: () -> Unit) {
    // Always visible: Restore from the backup, or — with no backup (e.g. after
    // a reinstall) — Reset to default, which removes KuBooster's settings.
    KubeCard(Modifier.fillMaxWidth()) {
        Text(
            if (state.hasBackup) "Restore default settings" else "Reset to default",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            if (state.hasBackup) "Puts back: ${state.restoreSummary ?: "original graphics config"}"
            else "Removes KuBooster's settings from the game. For frame rate and graphics, also use the game's Reset Graphics.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(Modifier.padding(top = 4.dp)) {
            TextButton(onClick = onRemoveGfx, enabled = !state.busy) {
                Text(if (state.hasBackup) "Restore" else "Reset", color = MaterialTheme.colorScheme.error)
            }
            if (state.restoreSummary != null) {
                TextButton(onClick = onDiscardBackup, enabled = !state.busy) { Text("Discard backup") }
            }
        }
    }
}

// ---- Diagnostic log --------------------------------------------------------

@Composable
private fun GfxLogCard() {
    val lines by GfxLog.lines.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    KubeCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Diagnostic log", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Share this if something doesn't work.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide" else "Show") }
        }
        if (expanded) {
            Text(
                text = lines.takeLast(150).joinToString("\n").ifEmpty { "(empty)" },
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
            )
            Row {
                TextButton(onClick = { clipboard.setText(AnnotatedString(GfxLog.text())) }) { Text("Copy") }
                TextButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, GfxLog.text())
                    context.startActivity(Intent.createChooser(send, "Share GFX log"))
                }) { Text("Share") }
                TextButton(onClick = { GfxLog.clear() }) { Text("Clear") }
            }
        }
    }
}

// ---- Overlays & dialogs ------------------------------------------------------

@Composable
private fun ApplyOverlay(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

@Composable
private fun ApplyConfirmDialog(
    fps: Int?,
    presetLabel: String,
    resolutionLabel: String,
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
            Text(
                "This edits PUBG's settings files. Edited game files can be detected and accounts can be banned for it — the same risk as every GFX tool. Your originals are backed up, but no outcome is guaranteed. If PUBG is open, it will be closed and reopened.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
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
            "KuBooster needs Shizuku (free, no root) to edit the game's settings files. Install Shizuku, start it with wireless debugging, then reopen KuBooster.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ShizukuPermissionCard(onRequest: () -> Unit, onRefresh: () -> Unit) {
    KubeCard(Modifier.fillMaxWidth()) {
        Text("Grant KuBooster access", style = MaterialTheme.typography.titleMedium)
        Text(
            "Shizuku is running. Allow KuBooster to use it — one tap, no root.",
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

// ---- Labels ------------------------------------------------------------------

private fun resolutionLabel(key: String): String = when (key) {
    "default" -> "Default"
    else -> key
}

private fun presetLabelOf(key: String?): String = when (key) {
    null -> "Game's own"
    "supersmooth" -> "Super Smooth"
    "smooth" -> "Smooth"
    "balanced" -> "Balanced"
    "hd" -> "HD"
    "hdr" -> "HDR"
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
