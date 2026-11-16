package com.kuduy.kubooster.feature.gfx

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kuduy.kubooster.data.gfx.GfxConfigEditor
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
    /** Frame rate chosen in the UI (null = nothing chosen yet). */
    val fpsTarget: Int? = null,
    /** Graphics preset key from [GfxConfigEditor.GRAPHICS_PRESETS]; null = keep the game's own graphics. */
    val presetKey: String? = null,
    /** Resolution tier: "default" | "720p" | "1080p". */
    val resolutionKey: String = "default",
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
                    hasBackup = preferred?.let { t -> engine.backupExists(t.variant.packageName) } ?: false,
                )
            }
            if (preferred != null) loadFile()
        }
    }

    fun selectTarget(packageName: String) {
        _state.update { it.copy(selectedPackage = packageName, hasBackup = engine.backupExists(packageName)) }
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
            val gameRunning = engine.isGameRunning(pkg)
            _state.update { it.copy(gameRunning = gameRunning) }
            val present = engine.isConfigPresent(pkg)
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
                        _state.update {
                            it.copy(
                                status = GfxFileStatus.Loaded(
                                    cvars = cvars,
                                    fpsValues = cvars.filterKeys { key ->
                                        key in GfxConfigEditor.MANAGED_FPS_KEYS
                                    },
                                    configPath = engine.configPath(pkg),
                                ),
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
        val presetLabel = presetLabel(presetKey)
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null, applyPhase = GfxApplyPhase.Optimizing) }
            val running = engine.isGameRunning(pkg)
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
            val content = String(original, Charsets.ISO_8859_1)
            val edited = GfxConfigEditor.applyCvars(content, values)
            val ok = engine.writeConfigVerified(pkg, edited.toByteArray(Charsets.ISO_8859_1))
            if (ok) {
                _state.update {
                    it.copy(
                        busy = false,
                        hasBackup = true,
                        applyPhase = GfxApplyPhase.Success,
                        message = buildString {
                            append("Applied & verified: frame rate $fps")
                            if (presetKey != null) append(", graphics $presetLabel")
                            if (resolutionKey != "default") append(", resolution $resolutionKey")
                            append(". Default config is backed up and restorable any time.")
                        },
                    )
                }
                launchGame(pkg)
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

    /** Opens the selected game after a successful apply ("Apply & Run Game"). */
    private fun launchGame(packageName: String) {
        try {
            container.appContext.packageManager
                .getLaunchIntentForPackage(packageName)
                ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                ?.let { container.appContext.startActivity(it) }
        } catch (_: Exception) {
            // Launching is a convenience; the config change already succeeded.
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
            if (backup == null) {
                _state.update { it.copy(busy = false, message = "No backup stored yet — the config was never modified.") }
                return@launch
            }
            val ok = engine.writeConfigVerified(pkg, backup)
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
                        "GFX settings removed — UserCustom.ini is back to its original, default state."
                    } else {
                        "Remove failed — the file was left unchanged. Try again with Shizuku running."
                    },
                )
            }
            if (ok) loadFile()
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun presetLabel(key: String?): String = when (key) {
        null -> "game's own"
        "supersmooth" -> "Super Smooth"
        "smooth" -> "Smooth"
        "balanced" -> "Balanced"
        "hd" -> "HD"
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
