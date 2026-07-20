package com.dousports.app.utils

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String
)

@Singleton
class UpdateChecker @Inject constructor() {

    private val gson = Gson()

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://api.github.com/repos/K-Bastin/DouSports/releases/latest")
                .openConnection()
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            val json = conn.getInputStream().bufferedReader().readText()

            val release = gson.fromJson(json, GitHubRelease::class.java)
            val tagVersion = release.tag_name.trimStart('v')

            if (isNewer(tagVersion, currentVersion)) {
                val downloadUrl = release.assets
                    .firstOrNull { it.name.endsWith(".apk") }
                    ?.browser_download_url
                    ?: release.html_url
                UpdateInfo(tagVersion, downloadUrl)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val l = local.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0..2) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }
}

internal data class GitHubRelease(
    val tag_name: String,
    val html_url: String,
    val assets: List<GitHubAsset>
)

internal data class GitHubAsset(
    val name: String,
    val browser_download_url: String
)
