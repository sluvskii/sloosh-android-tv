package com.sloosh.tv.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.sloosh.tv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

private const val TAG = "UpdateManager"
private const val GITHUB_REPO_OWNER = "sluvskii"
private const val GITHUB_REPO_NAME = "sloosh-android-tv"

data class GitHubReleaseDto(
    @SerializedName("tag_name") val tagName: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("assets") val assets: List<GitHubAssetDto>?
)

data class GitHubAssetDto(
    @SerializedName("name") val name: String?,
    @SerializedName("browser_download_url") val downloadUrl: String?,
    @SerializedName("size") val size: Long?
)

data class AppUpdateInfo(
    val newVersion: String,
    val currentVersion: String,
    val releaseTitle: String,
    val changelog: String,
    val downloadUrl: String,
    val fileSize: Long
)

class UpdateManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun checkForUpdates(
        owner: String = GITHUB_REPO_OWNER,
        repo: String = GITHUB_REPO_NAME
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Sloosh-Android-TV/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Check update returned HTTP ${response.code}")
                    return@withContext null
                }
                val bodyString = response.body?.string() ?: return@withContext null
                val release = gson.fromJson(bodyString, GitHubReleaseDto::class.java) ?: return@withContext null

                val remoteTag = release.tagName ?: return@withContext null
                val remoteVersionClean = remoteTag.trim().removePrefix("v").removePrefix("V")
                val currentVersionClean = BuildConfig.VERSION_NAME.trim().removePrefix("v").removePrefix("V")

                if (isNewerVersion(remoteVersionClean, currentVersionClean)) {
                    val apkAsset = release.assets?.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
                        ?: release.assets?.firstOrNull()

                    val downloadUrl = apkAsset?.downloadUrl
                    if (!downloadUrl.isNullOrBlank()) {
                        return@withContext AppUpdateInfo(
                            newVersion = remoteTag,
                            currentVersion = "v${BuildConfig.VERSION_NAME}",
                            releaseTitle = release.name ?: "Обновление $remoteTag",
                            changelog = release.body ?: "Что нового:\n• Исправления ошибок и повышение стабильности",
                            downloadUrl = downloadUrl,
                            fileSize = apkAsset.size ?: 0L
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "checkForUpdates error: ${e.message}")
        }
        return@withContext null
    }

    suspend fun downloadAndInstall(
        downloadUrl: String,
        onProgress: (progressFraction: Float, downloadedMb: Float, totalMb: Float) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Sloosh-Android-TV/${BuildConfig.VERSION_NAME}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                onError("Ошибка скачивания: HTTP ${response.code}")
                return@withContext
            }

            val body = response.body ?: run {
                onError("Пустой ответ от сервера")
                return@withContext
            }

            val contentLength = body.contentLength()
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "sloosh-update.apk")
            if (apkFile.exists()) apkFile.delete()

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val progress = if (contentLength > 0) totalRead.toFloat() / contentLength.toFloat() else 0f
                        val downloadedMb = totalRead / (1024f * 1024f)
                        val totalMb = if (contentLength > 0) contentLength / (1024f * 1024f) else downloadedMb

                        withContext(Dispatchers.Main) {
                            onProgress(progress, downloadedMb, totalMb)
                        }
                    }
                    output.flush()
                }
            }

            withContext(Dispatchers.Main) {
                installApk(apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "downloadAndInstall error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "Ошибка скачивания обновления")
            }
        }
    }

    fun installApk(apkFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "installApk error: ${e.message}", e)
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        return try {
            val remoteParts = remote.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
            val localParts = local.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            false
        } catch (e: Exception) {
            remote != local
        }
    }
}
