package com.spendshot.android.utils

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Checks for app updates via the GitHub Releases API.
 *
 * Compares the current app version against the latest GitHub release tag
 * and provides download information if an update is available.
 */
object GitHubUpdateChecker {

    private const val GITHUB_OWNER = "mrT4ntr4"
    private const val GITHUB_REPO = "SpendShot"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    data class UpdateInfo(
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val htmlUrl: String
    )

    /**
     * Checks GitHub Releases for a newer version.
     *
     * @param context Application context (used to get current version)
     * @return [UpdateInfo] if a newer version is available, null if up-to-date
     * @throws Exception on network or parsing errors
     */
    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        val currentVersion = getCurrentVersion(context)

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(API_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2026-03-10")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return@withContext null

        val body = response.body?.string() ?: return@withContext null
        val json = JSONObject(body)

        val tagName = json.optString("tag_name", "").removePrefix("v")
        val releaseNotes = json.optString("body", "No release notes available.")
        val htmlUrl = json.optString("html_url", "")

        // Try to find an APK asset in the release
        var downloadUrl = htmlUrl
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk")) {
                    downloadUrl = asset.optString("browser_download_url", htmlUrl)
                    break
                }
            }
        }

        if (tagName.isNotEmpty() && isNewerVersion(tagName, currentVersion)) {
            UpdateInfo(
                latestVersion = tagName,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                htmlUrl = htmlUrl
            )
        } else {
            null
        }
    }

    private fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0.0"
        }
    }

    /**
     * Compares two semantic version strings (e.g., "0.0.22" vs "0.0.21").
     * Returns true if [remote] is newer than [local].
     */
    internal fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
