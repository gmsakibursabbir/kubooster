package com.kuduy.kubooster.data.gfx

import android.content.Context
import com.kuduy.kubooster.core.common.AppDispatchers
import com.kuduy.kubooster.data.shizuku.ShizukuRepositoryImpl
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads and edits PUBG Mobile's `UserCustom.ini` through Shizuku (ADB-level
 * access — no root needed).
 *
 * Config path (verified against published GFX-tool documentation):
 *   /storage/emulated/0/Android/data/<pkg>/files/UE4Game/ShadowTrackerExtra/
 *     ShadowTrackerExtra/Saved/Config/Android/UserCustom.ini
 *
 * Backup policy: the pristine file is copied into the app's private storage
 * before the first modification and can always be restored byte-exact.
 * Writes are verified by reading the file back after the copy.
 */
class PubgGfxEngine(
    private val context: Context,
    private val shizuku: ShizukuRepositoryImpl,
    private val dispatchers: AppDispatchers,
) {
    init {
        GfxLog.init(context.filesDir)
    }

    fun configPath(packageName: String): String =
        "/storage/emulated/0/Android/data/$packageName/files/UE4Game/ShadowTrackerExtra/" +
            "ShadowTrackerExtra/Saved/Config/Android/UserCustom.ini"

    /** The UE4 save holding the selected frame rate (what the game actually uses). */
    fun savePath(packageName: String): String =
        "/storage/emulated/0/Android/data/$packageName/files/UE4Game/ShadowTrackerExtra/" +
            "ShadowTrackerExtra/Saved/SaveGames/Active.sav"

    private fun configDir(packageName: String): String =
        configPath(packageName).substringBeforeLast('/')

    private val tempPath = "/data/local/tmp/kubooster_usercustom.ini"

    /** null = Shizuku command failed; true/false = the real answer. */
    suspend fun isConfigPresent(packageName: String): Boolean? = withContext(dispatchers.io) {
        val result = shizuku.runShell(
            "test -f '${configPath(packageName)}' && echo KUB_YES || echo KUB_NO",
        ) ?: return@withContext null
        String(result.stdout, Charsets.ISO_8859_1).contains("KUB_YES")
    }

    /**
     * Closes the game (`am force-stop`) so its files can be edited safely,
     * then reports whether it is really gone. Only ever called right after
     * the user confirmed an apply/restore.
     */
    suspend fun closeGame(packageName: String): Boolean {
        withContext(dispatchers.io) { shizuku.runShell("am force-stop '$packageName'") }
        repeat(5) {
            if (isGameRunning(packageName) == false) return true
            kotlinx.coroutines.delay(300)
        }
        return isGameRunning(packageName) == false
    }

    /** True when the game process is alive — the file must not be edited then. */
    suspend fun isGameRunning(packageName: String): Boolean? = withContext(dispatchers.io) {
        val result = shizuku.runShell("pidof '$packageName'") ?: return@withContext null
        result.exitCode == 0 && String(result.stdout, Charsets.ISO_8859_1).trim().isNotEmpty()
    }

    suspend fun readConfig(packageName: String): ByteArray? = readFile(configPath(packageName))

    suspend fun readSave(packageName: String): ByteArray? = readFile(savePath(packageName))

    private suspend fun readFile(path: String): ByteArray? = withContext(dispatchers.io) {
        val result = shizuku.runShell("cat '$path'")
        when {
            result == null -> { GfxLog.log("read ${path.substringAfterLast('/')}: Shizuku call failed"); null }
            result.exitCode != 0 -> { GfxLog.log("read ${path.substringAfterLast('/')}: exit ${result.exitCode} (missing or no access)"); null }
            else -> result.stdout
        }
    }

    /** Listing of the SaveGames folder, for the diagnostic log. */
    suspend fun listSaveDir(packageName: String): String? = withContext(dispatchers.io) {
        val result = shizuku.runShell("ls -l '${savePath(packageName).substringBeforeLast('/')}' 2>&1")
            ?: return@withContext null
        String(result.stdout, Charsets.ISO_8859_1).trim()
    }

    /** Directory listing for honest diagnostics when the file is not found. */
    suspend fun listConfigDir(packageName: String): String? = withContext(dispatchers.io) {
        val result = shizuku.runShell("ls '${configDir(packageName)}' 2>&1")
            ?: return@withContext null
        String(result.stdout, Charsets.ISO_8859_1).trim()
    }

    /**
     * Writes [bytes] to the config via a shell-owned temp file in
     * /data/local/tmp, then verifies by reading the destination back and
     * comparing byte-for-byte.
     */
    suspend fun writeConfigVerified(packageName: String, bytes: ByteArray): Boolean =
        writeFileVerified(configPath(packageName), bytes)

    suspend fun writeSaveVerified(packageName: String, bytes: ByteArray): Boolean =
        writeFileVerified(savePath(packageName), bytes)

    private suspend fun writeFileVerified(path: String, bytes: ByteArray): Boolean =
        withContext(dispatchers.io) {
            val name = path.substringAfterLast('/')
            val feed = shizuku.runShell("cat > $tempPath", stdin = bytes)
            if (feed == null || feed.exitCode != 0) {
                GfxLog.log("write $name: temp file failed (${feed?.exitCode ?: "Shizuku call failed"})")
                return@withContext false
            }
            val copy = shizuku.runShell(
                "cp $tempPath '$path' 2>&1 && rm -f $tempPath && echo KUB_OK",
            )
            val copyOut = copy?.let { String(it.stdout, Charsets.ISO_8859_1).trim() }
            val copied = copy != null && copy.exitCode == 0 && copyOut!!.contains("KUB_OK")
            if (!copied) {
                GfxLog.log("write $name: copy failed (exit ${copy?.exitCode ?: "Shizuku call failed"}) $copyOut")
                return@withContext false
            }
            val verified = readFile(path)?.contentEquals(bytes) == true
            GfxLog.log("write $name: ${bytes.size} bytes, verify ${if (verified) "OK" else "FAILED"}")
            verified
        }

    // ---- Backup (app-private storage) ----------------------------------

    private fun backupDir(): File = File(context.filesDir, "gfx_backup").apply { mkdirs() }
    private fun backupFile(packageName: String) = File(backupDir(), "$packageName.ini")
    private fun backupMetaFile(packageName: String) = File(backupDir(), "$packageName.meta")

    fun backupExists(packageName: String): Boolean = backupFile(packageName).exists()

    fun storeBackup(packageName: String, bytes: ByteArray) {
        backupFile(packageName).writeBytes(bytes)
        backupMetaFile(packageName).writeText("saved=${System.currentTimeMillis()}")
    }

    fun backupBytes(packageName: String): ByteArray? = backupFile(packageName).takeIf {
        it.exists()
    }?.readBytes()

    /**
     * Deletes the backup after a successful restore: the config is back to
     * its default state, so keeping a stale backup would only make the UI
     * claim a restore is still pending. The next apply creates a fresh one.
     */
    fun deleteBackup(packageName: String) {
        backupFile(packageName).delete()
        backupMetaFile(packageName).delete()
        saveBackupFile(packageName).delete()
        clearApplied(packageName)
    }

    /**
     * Diagnostic: every .ini the game keeps under Saved/, with any line that
     * mentions a frame-rate limit (encoded +CVars are decoded first). Used to
     * find where a variant (e.g. PUBG Flash) caps FPS below what Active.sav
     * asks for. Read-only.
     */
    suspend fun scanFpsSettings(packageName: String): List<String> = withContext(dispatchers.io) {
        val saved = configPath(packageName).substringBefore("/Config/")
        val found = shizuku.runShell("find '$saved' -maxdepth 4 -name '*.ini' 2>/dev/null")
            ?: return@withContext listOf("scan: Shizuku call failed")
        val files = String(found.stdout, Charsets.ISO_8859_1).lines()
            .map { it.trim() }
            .filter { it.endsWith(".ini") && '\'' !in it }
            .take(25)
        val fpsRe = Regex("fps|framerate|frame_rate|maxfps|vsync", RegexOption.IGNORE_CASE)
        val out = mutableListOf("scan: ${files.size} .ini files under Saved/")
        for (path in files) {
            val bytes = readFile(path) ?: continue
            val name = path.removePrefix("$saved/")
            if (bytes.size > 300_000) { out += "  $name (${bytes.size} B, skipped)"; continue }
            val text = String(bytes, Charsets.ISO_8859_1)
            val hits = text.lineSequence().map { line ->
                val v = line.substringAfter("+CVars=", "").trim()
                if (v.isNotEmpty() && UserCustomCodec.isEncoded(v)) UserCustomCodec.decode(v) ?: line else line.trim()
            }.filter { fpsRe.containsMatchIn(it) }.distinct().take(20).toList()
            out += "  $name (${bytes.size} B): " + hits.joinToString(" | ").ifEmpty { "no FPS lines" }
        }
        out
    }

    /**
     * Opens the game through Shizuku (`am start` as the shell user, exactly
     * what `adb shell am start` does). OEM skins such as ColorOS/OxygenOS and
     * MIUI silently block one app from starting another; the shell user is
     * not subject to that. Returns true when the activity manager accepted it.
     */
    suspend fun launchGame(packageName: String): Boolean = withContext(dispatchers.io) {
        val script = "c=\$(cmd package resolve-activity --brief -c android.intent.category.LAUNCHER '$packageName' | tail -n 1); " +
            "case \"\$c\" in */*) am start -n \"\$c\" >/dev/null 2>&1 && echo KUB_OK && exit 0;; esac; " +
            "monkey -p '$packageName' -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 && echo KUB_OK"
        val r = shizuku.runShell(script) ?: return@withContext false
        String(r.stdout, Charsets.ISO_8859_1).contains("KUB_OK")
    }

    // ---- Backup mirror beside the game's files ----------------------------
    //
    // App-private backups vanish when KuBooster is uninstalled (e.g. when a
    // release build replaces a differently-signed test build). A copy is
    // kept next to the game's own files too, so Restore survives reinstalls.

    private fun iniMirror(pkg: String) = configPath(pkg) + ".kubooster-original"
    private fun savMirror(pkg: String) = savePath(pkg) + ".kubooster-original"

    /** Copies the app-private backups next to the game files (best effort). */
    suspend fun mirrorBackups(pkg: String) {
        backupBytes(pkg)?.let { if (readFile(iniMirror(pkg)) == null) writeFileVerified(iniMirror(pkg), it) }
        saveBackupBytes(pkg)?.let { if (readFile(savMirror(pkg)) == null) writeFileVerified(savMirror(pkg), it) }
    }

    /** Re-imports mirrored backups into app storage when they are missing there. */
    suspend fun importMirroredBackups(pkg: String) {
        if (!backupFile(pkg).exists()) readFile(iniMirror(pkg))?.let { storeBackup(pkg, it) }
        if (!saveBackupFile(pkg).exists()) readFile(savMirror(pkg))?.let { storeSaveBackup(pkg, it) }
    }

    suspend fun deleteMirrors(pkg: String, ini: Boolean, sav: Boolean) = withContext(dispatchers.io) {
        if (ini) shizuku.runShell("rm -f '${iniMirror(pkg)}'")
        if (sav) shizuku.runShell("rm -f '${savMirror(pkg)}'")
    }

    /**
     * No-backup fallback for "Reset to default": unlocks the refresh rate
     * (the original values are unknown, so the lock is simply removed).
     */
    suspend fun clearRefreshLock() = withContext(dispatchers.io) {
        shizuku.runShell("settings delete system min_refresh_rate")
        refreshBackupFile().delete()
    }

    // ---- Display refresh-rate lock -------------------------------------
    //
    // A game asks Android for a refresh rate based on the highest frame rate
    // it thinks the device allows. When GFX unlocks a higher level than the
    // game offers (e.g. 90 on PUBG Flash), the game may keep the screen at
    // 60 Hz while rendering 90 FPS — uneven frame pacing that looks like heavy
    // lag. Locking peak/min refresh rate to the panel's maximum avoids that.
    // Originals are saved once (device-wide) and restored on Remove.

    private val refreshKeys = listOf("peak_refresh_rate", "min_refresh_rate")
    private fun refreshBackupFile() = File(backupDir(), "refresh_rate.txt")

    fun supportedRefreshRates(): List<Float> = com.kuduy.kubooster.system.DisplayCapabilitiesProvider(context)
        .read().supportedRefreshRatesHz

    fun maxRefreshRate(): Float = com.kuduy.kubooster.system.DisplayCapabilitiesProvider(context)
        .read().supportedRefreshRatesHz.maxOrNull() ?: 60f

    private suspend fun getSystemSetting(key: String): String? = withContext(dispatchers.io) {
        val r = shizuku.runShell("settings get system $key") ?: return@withContext null
        String(r.stdout, Charsets.ISO_8859_1).trim().takeIf { it.isNotEmpty() }
    }

    /** Locks the display at [hz]. Returns the values read back, for the log. */
    suspend fun lockRefreshRate(hz: Float): Map<String, String?> = withContext(dispatchers.io) {
        if (!refreshBackupFile().exists()) {
            // "null" = the key was unset; restore deletes it again.
            val lines = refreshKeys.map { "$it=${getSystemSetting(it) ?: "null"}" }
            refreshBackupFile().writeText(lines.joinToString("\n"))
        }
        val value = hz.toString()
        refreshKeys.forEach { shizuku.runShell("settings put system $it $value") }
        refreshKeys.associateWith { getSystemSetting(it) }
    }

    /** Puts the original refresh-rate settings back. Null when nothing was locked. */
    suspend fun restoreRefreshRate(): Map<String, String?>? = withContext(dispatchers.io) {
        val file = refreshBackupFile().takeIf { it.exists() } ?: return@withContext null
        file.readLines().mapNotNull { line ->
            line.split('=', limit = 2).takeIf { it.size == 2 && it[0] in refreshKeys }
        }.forEach { (key, original) ->
            // Only values read from Settings earlier are ever written back.
            if (original == "null") shizuku.runShell("settings delete system $key")
            else if (original.toFloatOrNull() != null) shizuku.runShell("settings put system $key $original")
        }
        file.delete()
        refreshKeys.associateWith { getSystemSetting(it) }
    }

    // ---- What was applied (to detect a game resetting it) -----------------

    private fun appliedFile(pkg: String) = File(backupDir(), "$pkg.applied")

    /** Remembers the frame-rate level written and a fingerprint of the save as written. */
    fun recordApplied(pkg: String, fpsLevel: Int, written: ByteArray) {
        appliedFile(pkg).writeText("$fpsLevel ${written.contentHashCode()}")
    }

    /** (frame-rate level applied, fingerprint of the save as written), or null. */
    fun appliedRecord(pkg: String): Pair<Int, Int>? = try {
        appliedFile(pkg).takeIf { it.exists() }?.readText()?.trim()?.split(' ')
            ?.let { (level, hash) -> level.toInt() to hash.toInt() }
    } catch (_: Exception) {
        null
    }

    fun clearApplied(pkg: String) {
        appliedFile(pkg).delete()
    }

    // Active.sav backup, kept beside the .ini backup.
    private fun saveBackupFile(packageName: String) = File(backupDir(), "$packageName.sav")

    fun saveBackupExists(packageName: String): Boolean = saveBackupFile(packageName).exists()

    /** Drops a bad game-settings backup; the next apply saves the current settings instead. */
    fun deleteSaveBackup(packageName: String) {
        saveBackupFile(packageName).delete()
    }

    fun storeSaveBackup(packageName: String, bytes: ByteArray) =
        saveBackupFile(packageName).writeBytes(bytes)

    fun saveBackupBytes(packageName: String): ByteArray? =
        saveBackupFile(packageName).takeIf { it.exists() }?.readBytes()
}
