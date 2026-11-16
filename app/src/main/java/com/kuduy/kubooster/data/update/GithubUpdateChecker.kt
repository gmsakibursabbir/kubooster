package com.kuduy.kubooster.data.update

import com.kuduy.kubooster.core.common.AppDispatchers
import com.kuduy.kubooster.data.compatibility.VersionComparison
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * In-app update checker against the official GitHub repository.
 *
 * Network honesty: the ONLY hosts contacted are `api.github.com` (latest
 * release metadata) and `github.com` (the APK asset the user chooses to
 * download). No analytics, no telemetry, no other endpoints.
 */
class GithubUpdateChecker(private val dispatchers: AppDispatchers) {

    companion object {
        const val REPO = "gmsakibursabbir/kubooster"
        const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"
    }

    data class LatestRelease(
        val tagName: String,
        val title: String,
        val htmlUrl: String,
        val apkUrl: String?,
        val apkName: String?,
        val apkSize: Long,
    )

    @Serializable
    private data class AssetDto(
        val name: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String,
        val size: Long = 0,
    )

    @Serializable
    private data class ReleaseDto(
        @SerialName("tag_name") val tagName: String,
        val name: String? = null,
        @SerialName("html_url") val htmlUrl: String,
        val assets: List<AssetDto> = emptyList(),
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** Latest release metadata, or null when offline / rate-limited / unreachable. */
    suspend fun checkLatest(): LatestRelease? = withContext(dispatchers.io) {
        try {
            val conn = URL(LATEST_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "KuBooster-update-check")
            try {
                if (conn.responseCode != 200) return@withContext null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val dto = json.decodeFromString<ReleaseDto>(body)
                val apk = dto.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                LatestRelease(
                    tagName = dto.tagName,
                    title = dto.name ?: dto.tagName,
                    htmlUrl = dto.htmlUrl,
                    apkUrl = apk?.browserDownloadUrl,
                    apkName = apk?.name,
                    apkSize = apk?.size ?: 0,
                )
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    /** True when the release tag (e.g. `v1.0.3`) is newer than the installed version. */
    fun isNewer(tagName: String, currentVersionName: String): Boolean {
        val remote = tagName.removePrefix("v").removePrefix("V").trim()
        return try {
            VersionComparison.compareVersionNames(remote, currentVersionName) > 0
        } catch (_: Exception) {
            false
        }
    }
}
