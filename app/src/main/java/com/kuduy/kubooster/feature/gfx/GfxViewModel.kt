package com.kuduy.kubooster.feature.gfx

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kuduy.kubooster.data.gfx.ActiveSavEditor
import com.kuduy.kubooster.data.gfx.GfxConfigEditor
import com.kuduy.kubooster.data.gfx.GfxLog
import com.kuduy.kubooster.data.gfx.PubgGfxEngine
import com.kuduy.kubooster.di.AppContainer
import com.kuduy.kubooster.domain.model.DetectionState
import com.kuduy.kubooster.domain.model.GameMetadata
import com.kuduy.kubooster.domain.model.GameVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface GfxFileStatus {
    data object Idle : GfxFileStatus
    data object Loading : GfxFileStatus
    /** File found and decoded; all CVar keys' current values are available. */
    data class Loaded(
        val cvars: Map<String, String>,
        val fpsValues: Map<String, String>,
        val configPath: String,
        /** Frame-rate properties from Active.sav (name -> level); null = file not found. */
        val saveFpsLevels: Map<String, Int>? = null,
        /** BattleRenderQuality from Active.sav (game's graphics level); null = unknown. */
        val saveQuality: Int? = null,
    ) : GfxFileStatus
    /** File not present at the documented path; listing shows what is there. */
    data class Missing(
        val reason: String,
        val dirListing: String?,
        val configPath: String,
    ) : GfxFileStatus
    /** Shizuku unavailable/permission missing, or the shell call failed. */
    data class Unavailable(val reason: String) : GfxFileStatus
}

data class InstalledTarget(
    val variant: GameVariant,
    val versionName: String?,
)

data class GfxUiState(
    val shizukuInstalled: Boolean = true,
    val shizukuReady: Boolean = false,
    val targets: List<InstalledTarget> = emptyList(),
    val selectedPackage: String? = null,
    val status: GfxFileStatus = GfxFileStatus.Idle,
    val busy: Boolean = false,
    val hasBackup: Boolean = false,
    val gameRunning: Boolean? = null,
    val message: String? = null,
    /** What Restore will put back, in the game's names (null = no game-settings backup). */
    val restoreSummary: String? = null,
    /** Frame rate chosen in the UI (null = nothing chosen yet). */
    val fpsTarget: Int? = 90,
    /**
     * Graphics preset key from [GfxConfigEditor.GRAPHICS_PRESETS]. Defaults to
     * Super Smooth: higher graphics make the game lower the frame rate.
     */
    val presetKey: String? = "supersmooth",
    /** Resolution tier: "default" | "720p" | "1080p". */
    val resolutionKey: String = "default",
    /** MSAA on/off; off by default because it costs frame rate. */
    val antiAliasing: Boolean = false,
    /** Drives the full-screen apply overlays. */
    val applyPhase: GfxApplyPhase = GfxApplyPhase.Idle,
)

enum class GfxApplyPhase { Idle, Optimizing, Success, Failed }

/**
 * GFX mode: the real UserCustom.ini editor.
 *
 * Read → show decoded FPS keys → user picks a target → write with byte-exact
 * verification → one-tap restore from the automatic backup. The game must be
 * fully closed while editing (it rewrites the file on exit).
 */
class GfxViewModel(private val container: AppContainer) : ViewModel() {

    private val engine = PubgGfxEngine(
        container.appContext, container.shizukuRepository, container.dispatchers,
    )
    private val shizuku = container.shizukuRepository

    private val _state = MutableStateFlow(GfxUiState())
    val state: StateFlow<GfxUiState> = _state

    init {
        viewModelScope.launch {
            shizuku.permissionEvents.collect {
                refresh() // permission was granted (or revoked) — re-evaluate
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    busy = true,
                    message = null,
                    shizukuInstalled = shizuku.isInstalled(),
                    shizukuReady = shizuku.hasPermission(),
                )
            }
            val installed = mutableListOf<InstalledTarget>()
            GameVariant.entries.forEach { variant ->
                val meta = container.gameDetectionRepository.detectVariant(variant)
                if (meta.state == DetectionState.INSTALLED ||
                    meta.state == DetectionState.VERSION_UNREADABLE
                ) {
                    installed.add(InstalledTarget(variant, meta.versionName))
                }
            }
            val pinnedName = container.userPreferences.selectedGameVariant.firstOrNull()
            val preferred = installed.firstOrNull { it.variant.name == pinnedName }
                ?: installed.firstOrNull()
            _state.update {
                it.copy(
                    targets = installed,
                    selectedPackage = preferred?.variant?.packageName,
                    busy = false,
                    hasBackup = preferred?.let { t -> hasAnyBackup(t.variant.packageName) } ?: false,
                )
            }
            if (preferred != null) loadFile()
        }
    }

    fun selectTarget(packageName: String) {
        _state.update { it.copy(selectedPackage = packageName, hasBackup = hasAnyBackup(packageName)) }
        loadFile()
    }

    fun selectFps(fps: Int) = _state.update { it.copy(fpsTarget = fps) }

    fun selectPreset(key: String?) {
        require(key == null || key in GfxConfigEditor.GRAPHICS_PRESETS) { "unknown preset: $key" }
        _state.update { it.copy(presetKey = key) }
    }

    fun selectResolution(key: String) {
        require(key in GfxConfigEditor.RESOLUTION_KEYS) { "unknown resolution: $key" }
        _state.update { it.copy(resolutionKey = key) }
    }

    fun setAntiAliasing(on: Boolean) = _state.update { it.copy(antiAliasing = on) }

    fun dismissApplyPhase() = _state.update { it.copy(applyPhase = GfxApplyPhase.Idle) }

    fun loadFile() {
        val pkg = _state.value.selectedPackage ?: return
        viewModelScope.launch {
            if (!shizuku.hasPermission()) {
                _state.update {
                    it.copy(
                        status = GfxFileStatus.Unavailable(
                            if (!shizuku.isInstalled())
                                "Shizuku is not installed. Install it and start it, then come back — no root needed."
                            else "Shizuku permission not granted yet.",
                        ),
                        gameRunning = null,
                    )
                }
                return@launch
            }
            _state.update { it.copy(status = GfxFileStatus.Loading, message = null) }
            val version = _state.value.targets.firstOrNull { it.variant.packageName == pkg }?.versionName
            GfxLog.log("---- load $pkg v${version ?: "?"} · Android ${android.os.Build.VERSION.SDK_INT} · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            val gameRunning = engine.isGameRunning(pkg)
            GfxLog.log("game running: ${gameRunning ?: "unknown (Shizuku call failed)"}")
            _state.update { it.copy(gameRunning = gameRunning) }
            val present = engine.isConfigPresent(pkg)
            GfxLog.log("UserCustom.ini present: ${present ?: "unknown (Shizuku call failed)"}")
            when {
                present == null -> _state.update {
                    it.copy(status = GfxFileStatus.Unavailable("Could not talk to Shizuku. Restart Shizuku and retry."))
                }
                !present -> {
                    val listing = engine.listConfigDir(pkg)
                    _state.update {
                        it.copy(
                            status = GfxFileStatus.Missing(
                                reason = "UserCustom.ini was not found. Start the game once and open its graphics settings, fully close it, then retry. The listing below shows what actually exists at the config folder.",
                                dirListing = listing,
                                configPath = engine.configPath(pkg),
                            ),
                        )
                    }
                }
                else -> {
                    val bytes = engine.readConfig(pkg)
                    if (bytes == null) {
                        _state.update { it.copy(status = GfxFileStatus.Unavailable("Could not read UserCustom.ini through Shizuku.")) }
                    } else {
                        val content = String(bytes, Charsets.ISO_8859_1)
                        val cvars = GfxConfigEditor.readCvars(content)
                        GfxLog.log("UserCustom.ini: ${bytes.size} bytes, ${cvars.size} CVars, FPS keys ${cvars.filterKeys { it in GfxConfigEditor.MANAGED_FPS_KEYS }}")
                        val sav = engine.readSave(pkg)
                        logSave(pkg, sav)
                        engine.scanFpsSettings(pkg).forEach(GfxLog::log)
                        val saveLevels = sav?.let { s ->
                            ActiveSavEditor.readFpsProperties(s).associate { it.name to it.value }
                        }
                        _state.update {
                            it.copy(
                                status = GfxFileStatus.Loaded(
                                    cvars = cvars,
                                    fpsValues = cvars.filterKeys { key ->
                                        key in GfxConfigEditor.MANAGED_FPS_KEYS
                                    },
                                    configPath = engine.configPath(pkg),
                                    saveFpsLevels = saveLevels,
                                    saveQuality = sav?.let { s ->
                                        ActiveSavEditor.readIntProperties(s).firstOrNull { it.name == "BattleRenderQuality" }?.value
                                    },
                                ),
                                hasBackup = hasAnyBackup(pkg),
                                restoreSummary = restoreSummaryFor(pkg),
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Applies the chosen frame rate, graphics preset and resolution in one
     * verified write. The default config is always backed up BEFORE the
     * first modification; the write only happens after the backup exists.
     */
    fun apply() {
        val pkg = _state.value.selectedPackage ?: return
        val fps = _state.value.fpsTarget ?: return
        val presetKey = _state.value.presetKey
        val resolutionKey = _state.value.resolutionKey
        val values = mutableMapOf<String, String>()
        values += GfxConfigEditor.fpsEntries(fps)
        presetKey?.let { values += GfxConfigEditor.GRAPHICS_PRESETS.getValue(it) }
        resolutionKey.takeIf { it != "default" }?.let { values += GfxConfigEditor.resolutionEntries(it) }
        values += GfxConfigEditor.antiAliasingEntries(_state.value.antiAliasing)
        val presetLabel = presetLabel(presetKey)
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null, applyPhase = GfxApplyPhase.Optimizing) }
            GfxLog.log("---- apply $pkg: fps $fps, preset ${presetKey ?: "none"}, resolution $resolutionKey")
            val running = engine.isGameRunning(pkg)
            GfxLog.log("game running: ${running ?: "unknown"}")
            _state.update { it.copy(gameRunning = running) }
            if (running == true) {
                _state.update {
                    it.copy(
                        busy = false,
                        applyPhase = GfxApplyPhase.Failed,
                        message = "PUBG is running. Close it completely (also from Recent apps) — the game rewrites this file on exit — then apply again.",
                    )
                }
                return@launch
            }
            val original = engine.readConfig(pkg)
            if (original == null) {
                _state.update { it.copy(busy = false, applyPhase = GfxApplyPhase.Failed, message = "Could not read UserCustom.ini. Nothing was changed.") }
                return@launch
            }
            // The default config is backed up BEFORE anything is written —
            // guaranteed order, so a restore is always possible afterwards.
            if (!engine.backupExists(pkg)) engine.storeBackup(pkg, original)
            if (!engine.backupExists(pkg)) {
                _state.update {
                    it.copy(
                        busy = false,
                        applyPhase = GfxApplyPhase.Failed,
                        message = "Could not create the backup — nothing was changed.",
                    )
                }
                return@launch
            }
            // Build from the pristine backup, so keys written by an earlier
            // apply (or older KuBooster versions) never linger in the file.
            val base = engine.backupBytes(pkg) ?: original
            val content = String(base, Charsets.ISO_8859_1)
            val edited = GfxConfigEditor.applyCvars(content, values)
            val ok = engine.writeConfigVerified(pkg, edited.toByteArray(Charsets.ISO_8859_1))
            GfxLog.log("UserCustom.ini write: ${if (ok) "OK" else "FAILED"} (${values.size} keys)")
            if (ok) {
                // The frame rate the game actually runs at lives in Active.sav,
                // not in UserCustom.ini — without this step the unlock does nothing.
                val saveResult = applySaveFps(pkg, fps, presetKey)
                GfxLog.log("apply result: $saveResult")
                val saveOk = saveResult == SaveFpsResult.Written
                if (saveOk) syncRefreshRate(fps)
                _state.update {
                    it.copy(
                        busy = false,
                        hasBackup = true,
                        restoreSummary = restoreSummaryFor(pkg),
                        applyPhase = if (saveOk) GfxApplyPhase.Success else GfxApplyPhase.Failed,
                        message = buildString {
                            if (saveOk) {
                                append("Applied & verified: frame rate ${ActiveSavEditor.levelLabel(ActiveSavEditor.fpsLevel(fps))}")
                            } else {
                                append("Graphics config written, but the frame rate was NOT unlocked: ")
                                append(saveResult.reason)
                            }
                            if (presetKey != null) append(", graphics $presetLabel")
                            if (resolutionKey != "default") append(", resolution $resolutionKey")
                            append(". Originals are backed up and restorable any time.")
                            if (saveOk) append(" Do not change Frame Rate in the game's settings afterwards — that overwrites the unlock.")
                        },
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        busy = false,
                        applyPhase = GfxApplyPhase.Failed,
                        message = "The write failed verification — the file was left as it was. Check that Shizuku is running and retry.",
                    )
                }
            }
        }
    }

    enum class SaveFpsResult(val reason: String) {
        Written(""),
        Missing("Active.sav not found. Open the game once, change Frame Rate in its settings, fully close it, then apply again"),
        NoFpsField("Active.sav has no frame-rate setting yet. Open the game, pick any Frame Rate option once, fully close it, then apply again"),
        WriteFailed("writing Active.sav failed verification (file left unchanged). Restart Shizuku and retry"),
    }

    /**
     * Backs up Active.sav once, then sets every frame-rate property to the
     * level for [fps] and, when a preset is chosen, every graphics-quality
     * property to that preset's game level (e.g. Super Smooth = 6).
     */
    private suspend fun applySaveFps(pkg: String, fps: Int, presetKey: String?): SaveFpsResult {
        val sav = engine.readSave(pkg)
        if (sav == null) {
            GfxLog.log("Active.sav missing — SaveGames: ${engine.listSaveDir(pkg)}")
            return SaveFpsResult.Missing
        }
        val fpsLevel = ActiveSavEditor.fpsLevel(fps)
        var edited = ActiveSavEditor.applyFpsLevel(sav, fpsLevel)
        if (edited == null) {
            GfxLog.log("Active.sav: no int frame-rate property found — nothing written")
            logSave(pkg, sav)
            return SaveFpsResult.NoFpsField
        }
        GfxLog.log("Active.sav: frame-rate level -> $fpsLevel (${ActiveSavEditor.levelLabel(fpsLevel)})")
        if (presetKey == null) {
            val quality = ActiveSavEditor.readIntProperties(sav).firstOrNull { it.name == "BattleRenderQuality" }?.value
            GfxLog.log("no graphics preset: keeping game graphics level ${quality ?: "?"} — levels above Balanced (3) can make the game lower the frame rate")
        }
        presetKey?.let(ActiveSavEditor::qualityLevel)?.let { q ->
            val withQuality = ActiveSavEditor.applyQualityLevel(edited!!, q)
            if (withQuality != null) {
                edited = withQuality
                GfxLog.log("Active.sav: graphics level -> $q ($presetKey)")
            } else {
                GfxLog.log("Active.sav: no graphics-quality property found, graphics level not written")
            }
        }
        if (!engine.saveBackupExists(pkg)) engine.storeSaveBackup(pkg, sav)
        if (!engine.saveBackupExists(pkg)) {
            GfxLog.log("Active.sav backup could not be stored — not writing")
            return SaveFpsResult.WriteFailed
        }
        if (!engine.writeSaveVerified(pkg, edited!!)) return SaveFpsResult.WriteFailed
        // Read back what the game will see.
        engine.readSave(pkg)?.let { after ->
            GfxLog.log("Active.sav after write: " + ActiveSavEditor.readIntProperties(after)
                .filter { ActiveSavEditor.isFpsProperty(it.name) || ActiveSavEditor.isQualityProperty(it.name) }
                .joinToString { "${it.name}=${it.value}" })
        }
        return SaveFpsResult.Written
    }

    /** Logs what Active.sav contains: size, int settings and FPS/quality-like names of any type. */
    private suspend fun logSave(pkg: String, sav: ByteArray?) {
        if (sav == null) {
            GfxLog.log("Active.sav: not found — SaveGames: ${engine.listSaveDir(pkg)}")
            return
        }
        val ints = ActiveSavEditor.readIntProperties(sav)
        GfxLog.log("Active.sav: ${sav.size} bytes, header ${String(sav, 0, minOf(4, sav.size), Charsets.ISO_8859_1)}, ${ints.size} int settings")
        GfxLog.log("Active.sav FPS/quality ints: " + ints
            .filter { ActiveSavEditor.isFpsProperty(it.name) || ActiveSavEditor.isQualityProperty(it.name) }
            .joinToString { "${it.name}=${it.value}" }.ifEmpty { "none" })
        GfxLog.log("Active.sav related names: " + ActiveSavEditor.describeRelevantStrings(sav).joinToString(" | ").ifEmpty { "none" })
    }

    /**
     * Above 60 FPS, keeps the screen at its highest refresh rate so the game
     * can't hold it at 60 Hz while rendering faster (judder that looks like
     * lag — seen on PUBG Flash, which doesn't offer 90 itself). At 60 or
     * below, any earlier lock is undone.
     */
    private suspend fun syncRefreshRate(fps: Int) {
        val max = engine.maxRefreshRate()
        if (fps > 60 && max > 60f) {
            val readBack = engine.lockRefreshRate(max)
            GfxLog.log("refresh rate locked to ${max}Hz (panel max): $readBack")
        } else {
            engine.restoreRefreshRate()?.let { GfxLog.log("refresh rate lock removed: $it") }
            if (fps > 60) GfxLog.log("panel max is ${max}Hz — screen can't show more than that")
        }
    }

    /**
     * Opens the selected game after a successful apply. Called from the UI
     * with the Activity context: starting activities from an app-context
     * background launch is blocked on some OEM skins (MIUI/HyperOS, ColorOS),
     * which is why "game didn't open" used to happen silently.
     */
    fun launchGameFrom(context: Context) {
        val pkg = _state.value.selectedPackage ?: return
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(pkg)
            ?: pm.getLeanbackLaunchIntentForPackage(pkg)
            ?: Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(pkg)
            }
        if (intent == null) {
            GfxLog.log("game launch: no launcher intent for $pkg")
            _state.update { it.copy(message = "Config applied — but PUBG could not be opened. Start it manually.") }
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            GfxLog.log("game launch: starting $pkg")
            context.startActivity(intent)
        } catch (e: Exception) {
            GfxLog.log("game launch failed: ${e.javaClass.simpleName}: ${e.message}")
            _state.update {
                it.copy(message = "Config applied — but PUBG could not be opened (${e.javaClass.simpleName}). Start it manually.")
            }
        }
    }

    /** Removes GFX settings: writes the pristine original file back (default state). */
    fun removeGfx() {
        val pkg = _state.value.selectedPackage ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            val running = engine.isGameRunning(pkg)
            _state.update { it.copy(gameRunning = running) }
            if (running == true) {
                _state.update {
                    it.copy(busy = false, message = "Close PUBG completely first, then remove.")
                }
                return@launch
            }
            val backup = engine.backupBytes(pkg)
            val saveBackup = engine.saveBackupBytes(pkg)
            if (backup == null && saveBackup == null) {
                _state.update { it.copy(busy = false, message = "No backup stored yet — the config was never modified.") }
                return@launch
            }
            GfxLog.log("---- remove $pkg: ini backup ${backup != null}, sav backup ${saveBackup != null}")
            val iniOk = backup == null || engine.writeConfigVerified(pkg, backup)
            val ok = iniOk && (saveBackup == null || restoreSaveValues(pkg, saveBackup))
            GfxLog.log("remove result: ${if (ok) "OK" else "FAILED"}")
            if (ok) engine.restoreRefreshRate()?.let { GfxLog.log("refresh rate restored: $it") }
            if (ok) {
                // The config is back to its default state: drop the backup so
                // the "Remove GFX settings" card disappears (a stale backup
                // would only suggest a restore is still pending). The next
                // apply creates a fresh one automatically.
                engine.deleteBackup(pkg)
            }
            _state.update {
                it.copy(
                    busy = false,
                    hasBackup = if (ok) false else _state.value.hasBackup,
                    message = if (ok) {
                        "GFX settings removed — UserCustom.ini and Active.sav are back to their original state."
                    } else {
                        "Remove failed — the file was left unchanged. Try again with Shizuku running."
                    },
                )
            }
            if (ok) loadFile()
        }
    }

    /**
     * Puts back only the frame-rate and graphics values from the backup, into
     * the current Active.sav. That file also holds controls, sensitivity and
     * other settings the player may have changed since; overwriting it with
     * the old copy would undo them.
     */
    private suspend fun restoreSaveValues(pkg: String, backup: ByteArray): Boolean {
        val original = ActiveSavEditor.managedValues(backup)
        val current = engine.readSave(pkg) ?: return engine.writeSaveVerified(pkg, backup)
        // What the game left after playing: shows whether it kept or reset the unlock.
        GfxLog.log("Active.sav before restore (as the game left it): ${ActiveSavEditor.managedValues(current)}")
        GfxLog.log("restoring Active.sav values: $original")
        return engine.writeSaveVerified(pkg, ActiveSavEditor.setValues(current, original))
    }

    /** "Super Smooth · Extreme+ (90)" for the game-settings backup, or null. */
    private fun restoreSummaryFor(pkg: String): String? {
        val values = engine.saveBackupBytes(pkg)?.let(ActiveSavEditor::managedValues) ?: return null
        val quality = values["BattleRenderQuality"]?.let(ActiveSavEditor::qualityLabel)
        val fps = values["BattleFPS"]?.let(ActiveSavEditor::levelLabel)
        return listOfNotNull(quality, fps).joinToString(" · ").ifEmpty { "original settings" }
    }

    /**
     * Discards a bad game-settings backup (e.g. one saved after an earlier
     * faulty apply). The next apply saves the then-current settings instead.
     */
    fun discardSaveBackup() {
        val pkg = _state.value.selectedPackage ?: return
        engine.deleteSaveBackup(pkg)
        GfxLog.log("game-settings backup discarded for $pkg")
        _state.update {
            it.copy(
                hasBackup = hasAnyBackup(pkg),
                restoreSummary = null,
                message = "Backup discarded. Set graphics and frame rate in the game (or use its Reset Graphics), then apply again.",
            )
        }
    }

    private fun hasAnyBackup(pkg: String) = engine.backupExists(pkg) || engine.saveBackupExists(pkg)

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun presetLabel(key: String?): String = when (key) {
        null -> "game's own"
        "supersmooth" -> "Super Smooth"
        "smooth" -> "Smooth"
        "balanced" -> "Balanced"
        "hd" -> "HD"
        "hdr" -> "HDR"
        "ultrahdr" -> "Ultra HDR"
        else -> key
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GfxViewModel(container) as T
        }
    }
}
