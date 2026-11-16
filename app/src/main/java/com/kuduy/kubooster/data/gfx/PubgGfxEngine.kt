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

    fun configPath(packageName: String): String =
        "/storage/emulated/0/Android/data/$packageName/files/UE4Game/ShadowTrackerExtra/" +
            "ShadowTrackerExtra/Saved/Config/Android/UserCustom.ini"

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

    /** True when the game process is alive — the file must not be edited then. */
    suspend fun isGameRunning(packageName: String): Boolean? = withContext(dispatchers.io) {
        val result = shizuku.runShell("pidof '$packageName'") ?: return@withContext null
        result.exitCode == 0 && String(result.stdout, Charsets.ISO_8859_1).trim().isNotEmpty()
    }

    suspend fun readConfig(packageName: String): ByteArray? = withContext(dispatchers.io) {
        val result = shizuku.runShell("cat '${configPath(packageName)}'")
            ?: return@withContext null
        if (result.exitCode != 0) null else result.stdout
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
        withContext(dispatchers.io) {
            val feed = shizuku.runShell("cat > $tempPath", stdin = bytes)
                ?: return@withContext false
            if (feed.exitCode != 0) return@withContext false
            val copy = shizuku.runShell(
                "cp $tempPath '${configPath(packageName)}' && rm -f $tempPath && echo KUB_OK",
            ) ?: return@withContext false
            val copied = copy.exitCode == 0 &&
                String(copy.stdout, Charsets.ISO_8859_1).contains("KUB_OK")
            if (!copied) return@withContext false
            readConfig(packageName)?.contentEquals(bytes) == true
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
    }
}
