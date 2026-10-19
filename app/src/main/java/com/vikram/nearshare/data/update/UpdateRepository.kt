package com.vikram.nearshare.data.update

import android.content.Context
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(val versionCode: Long, val versionName: String, val downloadUrl: String, val releaseUrl: String)

class UpdateRepository(private val context: Context) {
    private val directory = File(context.filesDir, "updates")
    fun installedVersionCode(): Long = if (android.os.Build.VERSION.SDK_INT >= 28) context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode else {
        @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
    }
    fun installedVersionName(): String = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    fun downloadedApk() = File(directory, APK_NAME)
    suspend fun checkForUpdate(): AppUpdate = withContext(Dispatchers.IO) {
        val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000; readTimeout = 15000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "NearShare-Android")
        }
        try {
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IOException("GitHub update check failed (HTTP $status).")
            val release = JSONObject(text)
            val body = release.optString("body")
            val code = Regex("""(?im)^\s*versionCode\s*:\s*(\d+)\s*$""").find(body)?.groupValues?.get(1)?.toLongOrNull()
                ?: throw IOException("The latest debug release has no version information yet. Check again after the next successful build.")
            val name = Regex("""(?im)^\s*versionName\s*:\s*(.+?)\s*$""").find(body)?.groupValues?.get(1) ?: "Debug $code"
            val assets = release.optJSONArray("assets") ?: throw IOException("No APK is attached to the latest debug release.")
            var download: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                if (asset.optString("name") == APK_NAME) download = asset.optString("browser_download_url").takeIf { it.startsWith("https://") }
            }
            AppUpdate(code, name, download ?: throw IOException("Latest debug APK is not available."), release.optString("html_url", RELEASE_PAGE))
        } finally { connection.disconnect() }
    }
    suspend fun download(update: AppUpdate, progress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val temp = File(directory, "$APK_NAME.part")
        val target = downloadedApk()
        val connection = (URL(update.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000; readTimeout = 30000; instanceFollowRedirects = true
            setRequestProperty("User-Agent", "NearShare-Android")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("APK download failed (HTTP $status).")
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(8192); var received = 0L
                    while (true) {
                        val count = input.read(buffer); if (count < 0) break
                        output.write(buffer, 0, count); received += count
                        if (total > 0) progress(((received * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            if (!temp.exists() || temp.length() == 0L) throw IOException("Downloaded APK is empty.")
            if (!temp.renameTo(target)) { temp.copyTo(target, overwrite = true); temp.delete() }
            target
        } catch (e: Exception) { temp.delete(); throw e } finally { connection.disconnect() }
    }
    fun uriFor(file: File) = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    companion object {
        const val APK_NAME = "nearshare-debug.apk"
        private const val RELEASE_API = "https://api.github.com/repos/hustlewithvikram/nearshare/releases/tags/latest-debug"
        private const val RELEASE_PAGE = "https://github.com/hustlewithvikram/nearshare/releases/tag/latest-debug"
    }
}
