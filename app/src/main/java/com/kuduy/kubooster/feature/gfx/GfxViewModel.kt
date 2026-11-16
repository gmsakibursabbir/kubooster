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
    /** File found and decoded; map holds the managed FPS keys' current values. */
    data class Loaded(
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
)

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
                        _state.update {
                            it.copy(
                                status = GfxFileStatus.Loaded(
                                    fpsValues = GfxConfigEditor.readFpsValues(content),
                                    configPath = engine.configPath(pkg),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    /** [fps] comes from the fixed set offered in the UI (30/60/90/120). */
    fun applyFps(fps: Int) {
        val pkg = _state.value.selectedPackage ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            val running = engine.isGameRunning(pkg)
            _state.update { it.copy(gameRunning = running) }
            if (running == true) {
                _state.update {
                    it.copy(
                        busy = false,
                        message = "PUBG is running. Close it completely (also from Recent apps) — the game rewrites this file on exit — then apply again.",
                    )
                }
                return@launch
            }
            val original = engine.readConfig(pkg)
            if (original == null) {
                _state.update { it.copy(busy = false, message = "Could not read UserCustom.ini. Nothing was changed.") }
                return@launch
            }
            // Automatic, byte-exact backup before the first modification.
            if (!engine.backupExists(pkg)) engine.storeBackup(pkg, original)
            val content = String(original, Charsets.ISO_8859_1)
            val edited = GfxConfigEditor.applyFps(content, fps)
            val ok = engine.writeConfigVerified(pkg, edited.toByteArray(Charsets.ISO_8859_1))
            if (ok) {
                _state.update {
                    it.copy(
                        busy = false,
                        hasBackup = true,
                        message = "Applied & verified: frame-rate keys set to $fps. Start the game and pick the new frame rate in its own graphics settings.",
                    )
                }
                loadFile()
            } else {
                _state.update {
                    it.copy(
                        busy = false,
                        message = "The write failed verification — the file was left as it was. Check that Shizuku is running and retry.",
                    )
                }
            }
        }
    }

    fun restoreOriginal() {
        val pkg = _state.value.selectedPackage ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            val running = engine.isGameRunning(pkg)
            _state.update { it.copy(gameRunning = running) }
            if (running == true) {
                _state.update {
                    it.copy(busy = false, message = "Close PUBG completely first, then restore.")
                }
                return@launch
            }
            val backup = engine.backupBytes(pkg)
            if (backup == null) {
                _state.update { it.copy(busy = false, message = "No backup stored yet.") }
                return@launch
            }
            val ok = engine.writeConfigVerified(pkg, backup)
            _state.update {
                it.copy(
                    busy = false,
                    message = if (ok) "Original UserCustom.ini restored byte-exact."
                    else "Restore failed — the file was left unchanged. Try again with Shizuku running.",
                )
            }
            if (ok) loadFile()
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GfxViewModel(container) as T
        }
    }
}
