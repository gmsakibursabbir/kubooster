package com.kuduy.kubooster.data.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider
import com.kuduy.kubooster.domain.repository.ShizukuRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Shizuku integration. Strictly optional: every core feature works without it.
 *
 * Privileged operations use the documented Shizuku binder API (IShizukuService
 * AIDL, from the official Shizuku-API sample).
 *
 * Two privilege scopes exist:
 *  1. The original fixed allowlist: `pm grant` / `pm revoke` of
 *     WRITE_SECURE_SETTINGS for this own package ([grantSelfWriteSecureSettings]).
 *  2. [runShell] — generic `sh -c` used only by the GFX engine to edit PUBG's
 *     UserCustom.ini. Scripts are assembled exclusively from compile-time
 *     constants, the fixed GameVariant enum and numeric FPS values; no
 *     user-entered text is ever interpolated.
 */
class ShizukuRepositoryImpl(
    private val context: Context,
) : ShizukuRepository {

    private val _permissionEvents = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    override val permissionEvents: SharedFlow<Int> = _permissionEvents

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            _permissionEvents.tryEmit(grantResult)
        }

    init {
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    override fun isInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(ShizukuProvider.MANAGER_APPLICATION_ID, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    override fun isServiceAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Exception) {
        false
    }

    override fun hasPermission(): Boolean = try {
        isServiceAvailable() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) {
        false
    }

    override fun requestPermission() {
        if (!isServiceAvailable()) return
        Shizuku.requestPermission(REQUEST_CODE)
    }

    override fun dispose() {
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    private fun service(): IShizukuService {
        val binder = Shizuku.getBinder() ?: error("Shizuku binder unavailable")
        return IShizukuService.Stub.asInterface(ShizukuBinderWrapper(binder))
    }

    /** Exit code + captured stdout of a Shizuku shell run. */
    data class ShellResult(val exitCode: Int, val stdout: ByteArray)

    /**
     * Runs `sh -c <script>` under Shizuku (ADB-level privileges, no root),
     * optionally feeding [stdin] to the process and capturing stdout.
     *
     * Scope: used by the GFX engine only. Scripts are assembled exclusively
     * from compile-time constants plus values taken from the fixed
     * `GameVariant` enum and the numeric FPS set — no user-entered text is
     * ever interpolated, so command injection is not possible.
     */
    fun runShell(script: String, stdin: ByteArray? = null): ShellResult? = try {
        require(hasPermission()) { "Shizuku permission not granted" }
        val process = service().newProcess(arrayOf("sh", "-c", script), null, null)
        if (stdin != null) {
            ParcelFileDescriptor.AutoCloseOutputStream(process.outputStream).use { it.write(stdin) }
        }
        val stdout = ParcelFileDescriptor.AutoCloseInputStream(process.inputStream).use { it.readBytes() }
        val exit = process.waitFor()
        process.destroy()
        ShellResult(exit, stdout)
    } catch (_: Exception) {
        null
    }

    /**
     * The single allowlisted privileged operation: `pm grant <self>
     * WRITE_SECURE_SETTINGS`. Returns the process exit code, or null when the
     * binder call failed. Caller must verify the grant afterwards.
     */
    fun grantSelfWriteSecureSettings(): Int? =
        runPrivileged(arrayOf("pm", "grant", context.packageName, android.Manifest.permission.WRITE_SECURE_SETTINGS))

    /** Reverses the grant — `pm revoke` on our own package, equally allowlisted. */
    fun revokeSelfWriteSecureSettings(): Int? =
        runPrivileged(arrayOf("pm", "revoke", context.packageName, android.Manifest.permission.WRITE_SECURE_SETTINGS))

    private fun runPrivileged(cmd: Array<String>): Int? = try {
        require(hasPermission()) { "Shizuku permission not granted" }
        val process = service().newProcess(cmd, null, null)
        val exit = process.waitFor()
        process.destroy()
        exit
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val REQUEST_CODE = 4097
    }
}
