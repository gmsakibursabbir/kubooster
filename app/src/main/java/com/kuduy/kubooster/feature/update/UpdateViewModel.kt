package com.kuduy.kubooster.feature.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kuduy.kubooster.data.update.GithubUpdateChecker
import com.kuduy.kubooster.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

enum class UpdateStatus { IDLE, CHECKING, AVAILABLE, UP_TO_DATE, DOWNLOADING, DOWNLOADED, ERROR }

data class UpdateUiState(
    val status: UpdateStatus = UpdateStatus.IDLE,
    val latest: GithubUpdateChecker.LatestRelease? = null,
    val currentVersion: String = "",
    val message: String? = null,
) {
    /** Set internally once a download starts; used to resolve the APK for install. */
    internal var downloadedFileName: String? = null
}

/**
 * In-app update flow against GitHub Releases:
 * check (GitHub API) → download (system DownloadManager, from github.com only)
 * → install (system package installer with user confirmation).
 */
class UpdateViewModel(
    private val container: AppContainer,
    private val autoCheckOnLaunch: Boolean,
) : ViewModel() {

    private val checker = GithubUpdateChecker(container.dispatchers)
    private val context: Context = container.appContext

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state

    init {
        _state.value = _state.value.copy(currentVersion = currentVersionName())
        viewModelScope.launch {
            val enabled = container.userPreferences.updateCheckOnLaunch.first()
            if (autoCheckOnLaunch && enabled) checkNow(silent = true)
        }
    }

    /** Silent = no UI noise on failure or when already up to date. */
    fun checkNow(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _state.value = _state.value.copy(status = UpdateStatus.CHECKING, message = null)
            val latest = checker.checkLatest()
            val current = currentVersionName()
            if (latest == null) {
                _state.value = if (silent) {
                    _state.value.copy(status = UpdateStatus.IDLE, currentVersion = current)
                } else {
                    _state.value.copy(
                        status = UpdateStatus.ERROR,
                        message = "Could not reach GitHub. Check your internet connection and try again.",
                        currentVersion = current,
                    )
                }
                return@launch
            }
            if (checker.isNewer(latest.tagName, current)) {
                _state.value = _state.value.copy(status = UpdateStatus.AVAILABLE, latest = latest, currentVersion = current)
            } else {
                _state.value = _state.value.copy(
                    status = if (silent) UpdateStatus.IDLE else UpdateStatus.UP_TO_DATE,
                    latest = latest,
                    currentVersion = current,
                    message = if (silent) null else "You are on the latest version (v$current).",
                )
            }
        }
    }

    /** Enqueues the signed release APK download through the system DownloadManager. */
    fun download() {
        val latest = _state.value.latest ?: return
        val url = latest.apkUrl ?: return
        val fileName = latest.apkName ?: "kubooster-${latest.tagName}-release.apk"
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("KuBooster update — official GitHub release")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setMimeType("application/vnd.android.package-archive")
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            val id = dm.enqueue(request)
            _state.value.downloadedFileName = fileName
            _state.value = _state.value.copy(status = UpdateStatus.DOWNLOADING, message = null)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                status = UpdateStatus.ERROR,
                message = "Download could not start (${e.javaClass.simpleName}).",
            )
        }
    }

    /** Called by the DOWNLOAD_COMPLETE receiver; verifies the finished download. */
    fun onDownloadComplete(downloadId: Long) {
        val fileName = _state.value.downloadedFileName ?: return
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (!file.exists()) return
        // Only react to our own download finishing successfully.
        if (_state.value.status != UpdateStatus.DOWNLOADING) return
        _state.value = _state.value.copy(status = UpdateStatus.DOWNLOADED, message = null)
    }

    /** Opens the downloaded APK with the system installer (user confirms install). */
    fun install() {
        val fileName = _state.value.downloadedFileName ?: return
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (!file.exists()) {
            _state.value = _state.value.copy(
                status = UpdateStatus.ERROR,
                message = "The downloaded file is missing. Download it again.",
            )
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                status = UpdateStatus.ERROR,
                message = "Installer could not open (${e.javaClass.simpleName}). Allow \"install unknown apps\" for KuBooster in system settings.",
            )
        }
    }

    /** Opens the GitHub release page in the browser. */
    fun openReleasePage() {
        val url = _state.value.latest?.htmlUrl
            ?: "https://github.com/${GithubUpdateChecker.REPO}/releases"
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: Exception) {
        }
    }

    fun clearTransient() {
        if (_state.value.status == UpdateStatus.UP_TO_DATE || _state.value.status == UpdateStatus.ERROR) {
            _state.value = _state.value.copy(status = UpdateStatus.IDLE, message = null)
        }
    }

    private fun currentVersionName(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
    } catch (_: Exception) {
        "0"
    }

    companion object {
        fun factory(container: AppContainer, autoCheckOnLaunch: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    UpdateViewModel(container, autoCheckOnLaunch) as T
            }
    }
}
